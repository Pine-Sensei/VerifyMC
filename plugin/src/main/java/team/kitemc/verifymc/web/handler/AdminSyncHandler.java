package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

public class AdminSyncHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminSyncHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        // Require admin privileges
        if (AdminAuthUtil.requireAdmin(exchange, ctx) == null) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String language = req.optString("language", "en");

        if (ctx.getAuthmeService() == null) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("authme.service_unavailable", language)), 500);
            return;
        }

        if (!ctx.getAuthmeService().isAuthmeEnabled()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("authme.not_enabled", language)), 400);
            return;
        }

        try {
            ctx.getAuthmeService().syncApprovedUsers();
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success(
                    ctx.getMessage("authme.sync_success", language)));
        } catch (Exception e) {
            ctx.debugLog("AuthMe sync failed: " + e.getMessage());
            String errorMsg = ctx.getMessage("authme.sync_failed", language).replace("{error}", e.getMessage());
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(errorMsg), 500);
        }
    }
}
