package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.security.AdminAction;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Unlinks a Discord account from a user.
 * Requires authentication: only the user themselves or an admin can unlink.
 */
public class DiscordUnlinkHandler implements HttpHandler {
    private final PluginContext ctx;

    public DiscordUnlinkHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        // Step 1: Authenticate the request
        String authenticatedUser = AdminAuthUtil.getAuthenticatedUser(exchange, ctx);
        if (authenticatedUser == null) {
            return; // Response already sent by getAuthenticatedUser
        }

        JSONObject req;
        try {
            req = WebResponseHelper.readJson(exchange);
        } catch (JSONException e) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("error.invalid_json", "en")), 400);
            return;
        }
        String targetUsername = req.optString("username", "");
        String language = req.optString("language", "en");

        if (targetUsername.isBlank()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.missing_user_identifier", language)));
            return;
        }

        // Step 2: Check authorization - user must be the target user or an admin
        boolean isSelf = authenticatedUser.equalsIgnoreCase(targetUsername);

        if (!isSelf && !ctx.getAdminAccessManager().canAccess(authenticatedUser, AdminAction.UNLINK)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.forbidden", language)), 403);
            return;
        }

        boolean ok = ctx.getDiscordService().unlinkUser(targetUsername);
        if (ok) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success(
                    ctx.getMessage("discord.link_success", language)));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("discord.link_failed", language)));
        }
    }
}
