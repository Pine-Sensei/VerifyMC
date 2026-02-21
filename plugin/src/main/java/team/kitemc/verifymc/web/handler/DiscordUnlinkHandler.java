package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Unlinks a Discord account from a user.
 */
public class DiscordUnlinkHandler implements HttpHandler {
    private final PluginContext ctx;

    public DiscordUnlinkHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        JSONObject req = WebResponseHelper.readJson(exchange);
        String username = req.optString("username", "");
        String language = req.optString("language", "en");

        if (username.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.missing_user_identifier", language)));
            return;
        }

        boolean ok = ctx.getDiscordService().unlinkUser(username);
        if (ok) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success(
                    ctx.getMessage("discord.link_success", language)));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("discord.link_failed", language)));
        }
    }
}
