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
 * Deletes a user from the system and removes them from the whitelist.
 */
public class AdminUserDeleteHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminUserDeleteHandler(PluginContext ctx) {
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
        String language = req.optString("language", "en");

        if (target.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.missing_user_identifier", language)));
            return;
        }

        boolean ok = ctx.getUserDao().deleteUser(target);
        if (ok) {
            org.bukkit.Bukkit.getScheduler().runTask(ctx.getPlugin(), () ->
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "whitelist remove " + target));

            if (ctx.getAuthmeService() != null && ctx.getAuthmeService().isAuthmeEnabled()) {
                ctx.getAuthmeService().unregisterFromAuthme(target);
            }

            ctx.getAuditDao().addAudit(new AuditRecord("delete", operator, target, "", System.currentTimeMillis()));
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success(
                    ctx.getMessage("admin.delete_success", language)));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.delete_failed", language)));
        }
    }
}
