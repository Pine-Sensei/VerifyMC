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
 * Bans a user — updates status and removes from whitelist.
 */
public class AdminUserBanHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminUserBanHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;
        if (!AdminAuthUtil.requireAuth(exchange, ctx)) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String target = req.optString("username", req.optString("uuid", ""));
        String operator = req.optString("operator", "admin");
        String reason = req.optString("reason", "");

        if (target.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Missing username or uuid"));
            return;
        }

        boolean ok = ctx.getUserDao().banUser(target);
        if (ok) {
            org.bukkit.Bukkit.getScheduler().runTask(ctx.getPlugin(), () ->
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "whitelist remove " + target));
            ctx.getAuditDao().addAudit(new AuditRecord("ban", operator, target, reason, System.currentTimeMillis()));
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success("User banned"));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Failed to ban user"));
        }
    }
}
