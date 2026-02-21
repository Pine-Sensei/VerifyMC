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
 * Rejects a pending user, optionally sending an email with reject reason.
 * Extracted from WebServer.start() — the "/api/admin/user/reject" context.
 */
public class AdminUserRejectHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminUserRejectHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        // Require admin privileges and get operator username
        String operator = AdminAuthUtil.requireAdmin(exchange, ctx);
        if (operator == null) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String target = req.optString("username", req.optString("uuid", ""));
        String reason = req.optString("reason", "");
        String language = req.optString("language", "en");

        if (target.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.missing_user_identifier", language)));
            return;
        }

        boolean ok = ctx.getUserDao().updateUserStatus(target, "rejected", operator);
        if (ok) {
            var user = ctx.getUserDao().getUserByUsername(target);
            if (user != null) {
                String email = (String) user.get("email");
                if (email != null && !email.isEmpty()) {
                    ctx.getMailService().sendReviewResult(email, target, false,
                            ctx.getConfigManager().getLanguage());
                }
            }

            ctx.getAuditDao().addAudit(new AuditRecord("reject", operator, target, reason, System.currentTimeMillis()));

            if (ctx.getWsServer() != null) {
                ctx.getWsServer().broadcast(new JSONObject()
                        .put("type", "user_rejected")
                        .put("username", target).toString());
            }

            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success(
                    ctx.getMessage("review.reject_success", language)));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("review.failed", language)));
        }
    }
}
