package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;
import java.util.Map;

public class UserStatusHandler implements HttpHandler {
    private final PluginContext ctx;

    public UserStatusHandler(PluginContext ctx) {
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
                        username = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    } else if ("language".equals(kv[0])) {
                        language = kv[1];
                    }
                }
            }
        }

        JSONObject resp = new JSONObject();
        if (username != null && !username.isBlank()) {
            Map<String, Object> user = ctx.getUserDao().getUserByUsername(username);
            if (user != null) {
                resp.put("success", true);
                resp.put("registered", true);
                resp.put("status", user.getOrDefault("status", "unknown"));
                resp.put("username", user.getOrDefault("username", ""));
            } else {
                resp.put("success", true);
                resp.put("registered", false);
            }
        } else {
            resp.put("success", false);
            resp.put("msg", ctx.getMessage("error.missing_username", language));
        }
        WebResponseHelper.sendJson(exchange, resp);
    }
}
