package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Initiates Discord OAuth2 authorization — returns the auth URL.
 */
public class DiscordAuthHandler implements HttpHandler {
    private final PluginContext ctx;

    public DiscordAuthHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        String query = exchange.getRequestURI().getQuery();
        String username = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "username".equals(kv[0])) {
                    username = kv[1];
                }
            }
        }

        if (username == null || username.isBlank()) {
            JSONObject resp = new JSONObject().put("success", false).put("msg", "Missing username");
            WebResponseHelper.sendJson(exchange, resp);
            return;
        }

        String authUrl = ctx.getDiscordService().getAuthorizationUrl(username);
        JSONObject resp = new JSONObject();
        resp.put("success", true);
        resp.put("url", authUrl);
        WebResponseHelper.sendJson(exchange, resp);
    }
}
