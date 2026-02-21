package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.OpsManager;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.util.PasswordUtil;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebAuthHelper;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;
import java.util.Map;

public class LoginHandler implements HttpHandler {
    private final PluginContext ctx;
    private final boolean isAdminLogin;

    public LoginHandler(PluginContext ctx) {
        this(ctx, false);
    }

    public LoginHandler(PluginContext ctx, boolean isAdminLogin) {
        this.ctx = ctx;
        this.isAdminLogin = isAdminLogin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String username = req.optString("username", "");
        String password = req.optString("password", "");
        String language = req.optString("language", "en");

        OpsManager opsManager = ctx.getOpsManager();
        if (isAdminLogin && (opsManager == null || opsManager.getOps().isEmpty())) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.system_error", language)));
            return;
        }

        UserDao userDao = ctx.getUserDao();
        Map<String, Object> user = userDao.getUserByUsername(username);
        if (user == null) {
            user = userDao.getUserByEmail(username);
        }

        if (user == null) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.user_not_found", language)));
            return;
        }

        String actualUsername = (String) user.get("username");
        if (actualUsername == null || actualUsername.isEmpty()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.user_not_found", language)));
            return;
        }

        if (isAdminLogin) {
            if (!opsManager.isOp(actualUsername)) {
                ctx.getPlugin().getLogger().warning("[Security] Non-op login attempt for user: " + actualUsername);
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                        ctx.getMessage("login.not_authorized", language)));
                return;
            }
        }

        String storedPassword = (String) user.get("password");
        AuthmeService authmeService = ctx.getAuthmeService();
        boolean passwordValid = false;

        if (authmeService != null && authmeService.isAuthmeEnabled() && authmeService.hasAuthmeUser(actualUsername)) {
            String authmePassword = authmeService.getAuthmePassword(actualUsername);
            if (authmePassword != null && !authmePassword.isEmpty()) {
                passwordValid = PasswordUtil.verify(password, authmePassword);
            }
        }

        if (!passwordValid && storedPassword != null && !storedPassword.isEmpty()) {
            passwordValid = PasswordUtil.verify(password, storedPassword);
        }

        if (!passwordValid) {
            String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            ctx.getPlugin().getLogger().warning("[Security] Failed login attempt for user: " + actualUsername + " from IP: " + clientIp);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.failed", language)));
            return;
        }

        WebAuthHelper webAuthHelper = ctx.getWebAuthHelper();
        String token = webAuthHelper.generateToken(actualUsername);
        JSONObject resp = ApiResponseFactory.success(ctx.getMessage("login.success", language));
        resp.put("token", token);
        resp.put("username", actualUsername);
        WebResponseHelper.sendJson(exchange, resp);
    }
}
