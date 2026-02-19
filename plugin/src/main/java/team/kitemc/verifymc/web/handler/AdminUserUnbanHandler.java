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
 * Unbans a user — restores status and re-adds to whitelist.
 */
public class AdminUserUnbanHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminUserUnbanHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;
        if (!AdminAuthUtil.requireAuth(exchange, ctx)) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String target = req.optString("username", req.optString("uuid", ""));
        String operator = req.optString("operator", "admin");

        if (target.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Missing username or uuid"));
            return;
        }

        boolean ok = ctx.getUserDao().unbanUser(target);
        if (ok) {
            org.bukkit.Bukkit.getScheduler().runTask(ctx.getPlugin(), () ->
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "whitelist add " + target));
            ctx.getAuditDao().addAudit(new AuditRecord("unban", operator, target, "", System.currentTimeMillis()));
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success("User unbanned"));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Failed to unban user"));
        }
    }
}
