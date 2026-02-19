package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Handles admin user listing with pagination and search.
 * Extracted from WebServer.start() — the "/api/admin/users" context.
 */
public class AdminUserListHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminUserListHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        String token = exchange.getRequestHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!ctx.getWebAuthHelper().isValidToken(token)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Unauthorized"), 401);
            return;
        }

        // Parse query params
        String query = exchange.getRequestURI().getQuery();
        int page = 1, size = 20;
        String search = null, status = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length != 2) continue;
                switch (kv[0]) {
                    case "page" -> { try { page = Integer.parseInt(kv[1]); } catch (NumberFormatException ignored) {} }
                    case "size" -> { try { size = Integer.parseInt(kv[1]); } catch (NumberFormatException ignored) {} }
                    case "search" -> search = kv[1];
                    case "status" -> status = kv[1];
                }
            }
        }

        List<Map<String, Object>> users = ctx.getUserDao().getUsers(page, size, search, status);
        int total = ctx.getUserDao().getTotalUsers(search, status);
        int totalPages = (int) Math.ceil((double) total / size);

        JSONArray usersArray = new JSONArray();
        for (Map<String, Object> user : users) {
            usersArray.put(new JSONObject(user));
        }

        JSONObject pagination = new JSONObject();
        pagination.put("currentPage", page);
        pagination.put("pageSize", size);
        pagination.put("totalCount", total);
        pagination.put("totalPages", totalPages);
        pagination.put("hasNext", page < totalPages);
        pagination.put("hasPrev", page > 1);

        JSONObject resp = new JSONObject();
        resp.put("success", true);
        resp.put("users", usersArray);
        resp.put("pagination", pagination);
        WebResponseHelper.sendJson(exchange, resp);
    }
}
