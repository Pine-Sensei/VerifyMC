package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.web.ApiResponse;
import team.kitemc.verifymc.db.UserDao;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PublicApiHandler implements HttpHandler {
    private final UserDao userDao;
    
    public PublicApiHandler(UserDao userDao) {
        this.userDao = userDao;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (path.equals("/api/ping") && method.equals("GET")) {
                handlePing(exchange);
            } else if (path.equals("/api/config") && method.equals("GET")) {
                handleConfig(exchange);
            } else if (path.equals("/api/check-whitelist") && method.equals("GET")) {
                handleCheckWhitelist(exchange);
            } else {
                sendResponse(exchange, 404, ApiResponse.failure("Not found"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Internal server error"));
        }
    }
    
    private void handlePing(HttpExchange exchange) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        data.put("timestamp", System.currentTimeMillis());
        sendResponse(exchange, 200, ApiResponse.success("pong", data));
    }
    
    private void handleConfig(HttpExchange exchange) throws IOException {
        Map<String, Object> config = new HashMap<>();
        config.put("theme", "glassx");
        sendResponse(exchange, 200, ApiResponse.success("ok", config));
    }
    
    private void handleCheckWhitelist(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String username = getQueryParam(query, "username");
        
        if (username == null || username.isEmpty()) {
            sendResponse(exchange, 400, ApiResponse.failure("Username required"));
            return;
        }
        
        Map<String, Object> user = userDao.getUserByUsername(username);
        Map<String, Object> result = new HashMap<>();
        
        if (user != null) {
            result.put("found", true);
            result.put("status", user.get("status"));
            result.put("username", user.get("username"));
        } else {
            result.put("found", false);
            result.put("status", "not_registered");
        }
        
        sendResponse(exchange, 200, ApiResponse.success("ok", result));
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, ApiResponse response) throws IOException {
        String json = response.toJson();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(param)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
