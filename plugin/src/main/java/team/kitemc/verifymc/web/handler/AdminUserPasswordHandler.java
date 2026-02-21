package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.db.AuditRecord;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Changes a user's password (stored and/or AuthMe).
 */
public class AdminUserPasswordHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminUserPasswordHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;
        if (!AdminAuthUtil.requireAuth(exchange, ctx)) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String target = req.optString("username", req.optString("uuid", ""));
        String password = req.optString("password", "");
        String operator = req.optString("operator", "admin");
        String language = req.optString("language", "en");

        if (target.isBlank() || password.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.missing_user_identifier", language)));
            return;
        }

        boolean ok = ctx.getUserDao().updatePassword(target, password);

        if (ok && ctx.getAuthmeService().isAuthmeEnabled()) {
            ctx.getAuthmeService().changePassword(target, password);
        }

        if (ok) {
            ctx.getAuditDao().addAudit(new AuditRecord("password_change", operator, target, "", System.currentTimeMillis()));
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success(
                    ctx.getMessage("admin.password_change_success", language)));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.password_change_failed", language)));
        }
    }
}
