package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Verifies an admin token is still valid.
 * Extracted from WebServer.start() — the "/api/admin/verify" context.
 */
public class AdminVerifyHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminVerifyHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String token = req.optString("token", "");
        String language = req.optString("language", "en");

        if (ctx.getWebAuthHelper().isValidToken(token)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success(
                    ctx.getMessage("admin.login_success", language)));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.login_failed", language)));
        }
    }
}
