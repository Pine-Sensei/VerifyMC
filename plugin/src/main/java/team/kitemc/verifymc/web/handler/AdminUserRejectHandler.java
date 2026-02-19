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
        if (!requireAuth(exchange)) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String target = req.optString("username", req.optString("uuid", ""));
        String operator = req.optString("operator", "admin");
        String reason = req.optString("reason", "");

        if (target.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Missing username or uuid"));
            return;
        }

        boolean ok = ctx.getUserDao().updateUserStatus(target, "rejected", operator);
        if (ok) {
            // Send rejection email
            var user = ctx.getUserDao().getUserByUsername(target);
            if (user != null) {
                String email = (String) user.get("email");
                if (email != null && !email.isEmpty()) {
                    ctx.getMailService().sendReviewResult(email, target, false,
                            ctx.getConfigManager().getLanguage());
                }
            }

            // Audit
            ctx.getAuditDao().addAudit(new AuditRecord("reject", operator, target, reason, System.currentTimeMillis()));

            // Broadcast via WebSocket
            if (ctx.getWsServer() != null) {
                ctx.getWsServer().broadcast(new JSONObject()
                        .put("type", "user_rejected")
                        .put("username", target).toString());
            }

            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success("User rejected"));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Failed to reject user"));
        }
    }

    private boolean requireAuth(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!ctx.getWebAuthHelper().isValidToken(token)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Unauthorized"), 401);
            return false;
        }
        return true;
    }
}
