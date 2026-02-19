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
 * Approves a pending user — updates status to "approved" and whitelists in-game.
 * Extracted from WebServer.start() — the "/api/admin/user/approve" context.
 */
public class AdminUserApproveHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminUserApproveHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;
        if (!requireAuth(exchange)) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String target = req.optString("username", req.optString("uuid", ""));
        String operator = req.optString("operator", "admin");

        if (target.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Missing username or uuid"));
            return;
        }

        boolean ok = ctx.getUserDao().updateUserStatus(target, "approved", operator);
        if (ok) {
            // Whitelist the user in-game
            org.bukkit.Bukkit.getScheduler().runTask(ctx.getPlugin(), () ->
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "whitelist add " + target));

            // AuthMe registration if applicable
            if (ctx.getAuthmeService().isAuthmeEnabled()) {
                var user = ctx.getUserDao().getUserByUsername(target);
                if (user != null) {
                    String storedPassword = (String) user.get("password");
                    if (storedPassword != null && !storedPassword.isEmpty()) {
                        ctx.getAuthmeService().registerToAuthme(target, storedPassword);
                    }
                }
            }

            // Send approval email
            var user = ctx.getUserDao().getUserByUsername(target);
            if (user != null) {
                String email = (String) user.get("email");
                if (email != null && !email.isEmpty()) {
                    ctx.getMailService().sendReviewResult(email, target, true,
                            ctx.getConfigManager().getLanguage());
                }
            }

            // Audit
            ctx.getAuditDao().addAudit(new AuditRecord("approve", operator, target, "", System.currentTimeMillis()));

            // Broadcast via WebSocket
            if (ctx.getWsServer() != null) {
                ctx.getWsServer().broadcast(new JSONObject()
                        .put("type", "user_approved")
                        .put("username", target).toString());
            }

            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success("User approved"));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Failed to approve user"));
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
