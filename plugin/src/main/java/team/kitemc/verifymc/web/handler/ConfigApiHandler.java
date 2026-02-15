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

public class ConfigApiHandler implements HttpHandler {
    private final Plugin plugin;
    private final WebAuthHelper authHelper;
    private final BiFunction<String, String, String> messageResolver;
    private String staticDir;
    
    public ConfigApiHandler(Plugin plugin, WebAuthHelper authHelper, 
                            BiFunction<String, String, String> messageResolver, String staticDir) {
        this.plugin = plugin;
        this.authHelper = authHelper;
        this.messageResolver = messageResolver;
        this.staticDir = staticDir;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (path.equals("/api/reload-config") && method.equals("POST")) {
                handleReloadConfig(exchange);
            } else if (path.equals("/api/version-check") && method.equals("GET")) {
                handleVersionCheck(exchange);
            } else {
                sendResponse(exchange, 404, ApiResponse.failure("Not found"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Internal server error: " + e.getMessage()));
        }
    }
    
    private void handleReloadConfig(HttpExchange exchange) throws IOException {
        if (!authHelper.isAuthenticated(exchange)) {
            sendResponse(exchange, 401, ApiResponse.failure("Authentication required"));
            return;
        }
        
        try {
            plugin.reloadConfig();
            String theme = plugin.getConfig().getString("frontend.theme", "default");
            
            team.kitemc.verifymc.ResourceManager resourceManager = 
                new team.kitemc.verifymc.ResourceManager((org.bukkit.plugin.java.JavaPlugin) plugin);
            
            if (!resourceManager.themeExists(theme)) {
                sendResponse(exchange, 400, ApiResponse.failure(
                    "Theme not found: " + theme + ". Available themes: default, glassx"));
                return;
            }
            
            String newStaticDir = resourceManager.getThemeStaticDir(theme);
            this.staticDir = newStaticDir;
            
            Map<String, Object> data = new HashMap<>();
            data.put("theme", theme);
            data.put("static_dir", newStaticDir);
            
            sendResponse(exchange, 200, ApiResponse.success(
                "Configuration reloaded successfully. Theme switched to: " + theme, data));
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Failed to reload configuration: " + e.getMessage()));
        }
    }
    
    private void handleVersionCheck(HttpExchange exchange) throws IOException {
        if (!authHelper.isAuthenticated(exchange)) {
            sendResponse(exchange, 401, ApiResponse.failure("Authentication required"));
            return;
        }
        
        try {
            team.kitemc.verifymc.VerifyMC mainPlugin = (team.kitemc.verifymc.VerifyMC) plugin;
            team.kitemc.verifymc.service.VersionCheckService versionService = mainPlugin.getVersionCheckService();
            
            if (versionService != null) {
                versionService.checkForUpdatesAsync().thenAccept(result -> {
                    try {
                        Map<String, Object> data = new HashMap<>();
                        data.put("current_version", result.getCurrentVersion());
                        data.put("latest_version", result.getLatestVersion());
                        data.put("has_update", result.hasUpdate());
                        data.put("download_url", result.getDownloadUrl());
                        data.put("changelog", result.getChangelog());
                        
                        sendResponseAsync(exchange, 200, ApiResponse.success("ok", data));
                    } catch (Exception e) {
                        try {
                            sendResponseAsync(exchange, 500, ApiResponse.failure("Failed to send version check response"));
                        } catch (Exception ignored) {}
                    }
                }).exceptionally(throwable -> {
                    try {
                        sendResponseAsync(exchange, 500, ApiResponse.failure("Version check failed: " + throwable.getMessage()));
                    } catch (Exception ignored) {}
                    return null;
                });
            } else {
                sendResponse(exchange, 500, ApiResponse.failure("Version check service not available"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Internal server error"));
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
    
    private void sendResponseAsync(HttpExchange exchange, int statusCode, ApiResponse response) throws IOException {
        sendResponse(exchange, statusCode, response);
    }
    
    public void setStaticDir(String staticDir) {
        this.staticDir = staticDir;
    }
    
    public String getStaticDir() {
        return staticDir;
    }
}
