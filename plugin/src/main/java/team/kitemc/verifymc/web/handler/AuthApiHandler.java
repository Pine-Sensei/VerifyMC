package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.web.ApiResponse;
import team.kitemc.verifymc.web.WebAuthHelper;
import team.kitemc.verifymc.web.WebResponseHelper;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class AuthApiHandler implements HttpHandler {
    private final Plugin plugin;
    private final WebAuthHelper authHelper;
    private final BiFunction<String, String, String> messageResolver;
    
    public AuthApiHandler(Plugin plugin, WebAuthHelper authHelper, BiFunction<String, String, String> messageResolver) {
        this.plugin = plugin;
        this.authHelper = authHelper;
        this.messageResolver = messageResolver;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (path.equals("/api/admin-login") && method.equals("POST")) {
                handleAdminLogin(exchange);
            } else if (path.equals("/api/admin-verify") && method.equals("POST")) {
                handleAdminVerify(exchange);
            } else {
                sendResponse(exchange, 404, ApiResponse.failure("Not found"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Internal server error"));
        }
    }
    
    private void handleAdminLogin(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String password = req.optString("password");
        String language = req.optString("language", "en");
        
        String adminPassword = plugin.getConfig().getString("admin.password", "");
        
        if (password.equals(adminPassword)) {
            Map<String, Object> data = new HashMap<>();
            data.put("token", authHelper.generateSecureToken());
            sendResponse(exchange, 200, ApiResponse.success(messageResolver.apply("admin.login_success", language), data));
        } else {
            sendResponse(exchange, 401, ApiResponse.failure(messageResolver.apply("admin.login_failed", language)));
        }
    }
    
    private void handleAdminVerify(HttpExchange exchange) throws IOException {
        if (authHelper.isAuthenticated(exchange)) {
            sendResponse(exchange, 200, ApiResponse.success("Token is valid"));
        } else {
            sendResponse(exchange, 401, ApiResponse.failure("Invalid or expired token"));
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, ApiResponse response) throws IOException {
        String json = response.toJson();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }
}
