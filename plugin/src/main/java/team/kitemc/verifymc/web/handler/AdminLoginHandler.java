package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Admin login handler — validates admin password and returns a session token.
 * Extracted from WebServer.start() — the "/api/admin/login" context.
 */
public class AdminLoginHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminLoginHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String password = req.optString("password", "");
        String language = req.optString("language", "en");

        String adminPassword = ctx.getConfigManager().getAdminPassword();
        if (adminPassword.isEmpty()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("error.resource_init_failed", language)));
            return;
        }

        if (!adminPassword.equals(password)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.login_failed", language)));
            return;
        }

        String token = ctx.getWebAuthHelper().generateToken();
        JSONObject resp = ApiResponseFactory.success(ctx.getMessage("admin.login_success", language));
        resp.put("token", token);
        WebResponseHelper.sendJson(exchange, resp);
    }
}
