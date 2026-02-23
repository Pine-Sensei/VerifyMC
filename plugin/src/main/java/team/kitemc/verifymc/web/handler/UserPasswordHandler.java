package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.db.AuditRecord;
import team.kitemc.verifymc.util.PasswordUtil;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;
import java.util.Map;

public class UserPasswordHandler implements HttpHandler {
    private final PluginContext ctx;

    public UserPasswordHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        String username = AdminAuthUtil.getAuthenticatedUser(exchange, ctx);
        if (username == null) return;

        JSONObject req;
        try {
            req = WebResponseHelper.readJson(exchange);
        } catch (JSONException e) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("error.invalid_json", "en")), 400);
            return;
        }

        String language = req.optString("language", "en");
        String currentPassword = req.optString("currentPassword", "");
        String newPassword = req.optString("newPassword", "");

        if (currentPassword.isBlank() || newPassword.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.password_required", language)));
            return;
        }

        String passwordRegex = ctx.getConfigManager().getAuthmePasswordRegex();
        if (!newPassword.matches(passwordRegex)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.invalid_password", language).replace("{regex}", passwordRegex)));
            return;
        }

        Map<String, Object> user = ctx.getUserDao().getUserByUsername(username);
        if (user == null) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("error.user_not_found", language)));
            return;
        }

        String storedPassword = (String) user.get("password");
        if (storedPassword == null || storedPassword.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("user.password_not_set", language)));
            return;
        }

        if (!PasswordUtil.verify(currentPassword, storedPassword)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("user.current_password_incorrect", language)));
            return;
        }

        boolean updated = ctx.getUserDao().updatePassword(username, newPassword);

        if (updated) {
            if (ctx.getAuthmeService() != null && ctx.getAuthmeService().isAuthmeEnabled()) {
                ctx.getAuthmeService().changePassword(username, newPassword);
            }

            ctx.getAuditDao().addAudit(new AuditRecord(
                "password_change", username, username, 
                "User changed own password", System.currentTimeMillis()
            ));
            
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success(
                    ctx.getMessage("admin.password_change_success", language)));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.password_change_failed", language)));
        }
    }
}
