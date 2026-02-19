package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;
import java.util.Map;

/**
 * Returns user whitelist/registration status by UUID.
 */
public class UserStatusHandler implements HttpHandler {
    private final PluginContext ctx;

    public UserStatusHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        String query = exchange.getRequestURI().getQuery();
        String uuid = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "uuid".equals(kv[0])) {
                    uuid = kv[1];
                }
            }
        }

        JSONObject resp = new JSONObject();
        if (uuid != null && !uuid.isBlank()) {
            Map<String, Object> user = ctx.getUserDao().getUserByUuid(uuid);
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
            resp.put("msg", "Missing uuid");
        }
        WebResponseHelper.sendJson(exchange, resp);
    }
}
