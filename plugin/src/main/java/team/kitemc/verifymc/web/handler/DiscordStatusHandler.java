package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

public class DiscordStatusHandler implements HttpHandler {
    private final PluginContext ctx;

    public DiscordStatusHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        String query = exchange.getRequestURI().getQuery();
        String username = null;
        String language = "en";
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2) {
                    if ("username".equals(kv[0])) {
                        username = kv[1];
                    } else if ("language".equals(kv[0])) {
                        language = kv[1];
                    }
                }
            }
        }

        JSONObject resp = new JSONObject();
        if (username != null && !username.isBlank()) {
            boolean linked = ctx.getDiscordService().isLinked(username);
            resp.put("success", true);
            resp.put("linked", linked);
            if (linked) {
                var discordUser = ctx.getDiscordService().getLinkedUser(username);
                if (discordUser != null) {
                    resp.put("user", discordUser.toJson());
                }
            }
        } else {
            resp.put("success", false);
            resp.put("msg", ctx.getMessage("error.missing_username", language));
        }
        WebResponseHelper.sendJson(exchange, resp);
    }
}
