package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.web.ApiResponse;
import team.kitemc.verifymc.web.WebResponseHelper;
import team.kitemc.verifymc.service.DiscordService;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DiscordApiHandler implements HttpHandler {
    private final Plugin plugin;
    private final DiscordService discordService;
    
    public DiscordApiHandler(Plugin plugin, DiscordService discordService) {
        this.plugin = plugin;
        this.discordService = discordService;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (path.equals("/api/discord/auth") && method.equals("POST")) {
                handleDiscordAuth(exchange);
            } else if (path.equals("/api/discord/callback")) {
                handleDiscordCallback(exchange);
            } else if (path.equals("/api/discord/status") && method.equals("GET")) {
                handleDiscordStatus(exchange);
            } else {
                sendResponse(exchange, 404, ApiResponse.failure("Not found"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Internal server error"));
        }
    }
    
    private void handleDiscordAuth(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String username = req.optString("username", "");
        
        if (!discordService.isEnabled()) {
            sendResponse(exchange, 400, ApiResponse.failure("Discord integration is not enabled"));
            return;
        }
        
        if (username.isEmpty()) {
            sendResponse(exchange, 400, ApiResponse.failure("Username is required"));
            return;
        }
        
        String authUrl = discordService.generateAuthUrl(username);
        if (authUrl != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("auth_url", authUrl);
            sendResponse(exchange, 200, ApiResponse.success("ok", data));
        } else {
            sendResponse(exchange, 500, ApiResponse.failure("Failed to generate auth URL"));
        }
    }
    
    private void handleDiscordCallback(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String code = null;
        String state = null;
        
        if (query != null) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    if ("code".equals(keyValue[0])) {
                        code = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    } else if ("state".equals(keyValue[0])) {
                        state = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    }
                }
            }
        }
        
        String accept = exchange.getRequestHeaders().getFirst("Accept");
        boolean wantsHtml = accept == null || accept.contains("text/html");
        
        if (code == null || state == null) {
            if (wantsHtml) {
                sendDiscordCallbackHtml(exchange, false, "Missing code or state parameter", null);
            } else {
                sendResponse(exchange, 400, ApiResponse.failure("Missing code or state parameter"));
            }
            return;
        }
        
        DiscordService.DiscordCallbackResult result = discordService.handleCallback(code, state);
        
        if (wantsHtml) {
            String discordUsername = result.user != null ? 
                (result.user.globalName != null ? result.user.globalName : result.user.username) : null;
            sendDiscordCallbackHtml(exchange, result.success, result.message, discordUsername);
        } else {
            if (result.success) {
                Map<String, Object> data = new HashMap<>();
                if (result.user != null) {
                    data.put("discord_user", result.user.username);
                    data.put("discord_id", result.user.id);
                }
                sendResponse(exchange, 200, ApiResponse.success(result.message, data));
            } else {
                sendResponse(exchange, 400, ApiResponse.failure(result.message));
            }
        }
    }
    
    private void handleDiscordStatus(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String username = getQueryParam(query, "username");
        
        if (username == null || username.isEmpty()) {
            sendResponse(exchange, 400, ApiResponse.failure("Username is required"));
            return;
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("linked", discordService.isLinked(username));
        
        if (discordService.isLinked(username)) {
            DiscordService.DiscordUser user = discordService.getLinkedUser(username);
            if (user != null) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.id);
                userData.put("username", user.username);
                userData.put("discriminator", user.discriminator);
                userData.put("global_name", user.globalName);
                data.put("user", userData);
            }
        }
        
        sendResponse(exchange, 200, ApiResponse.success("ok", data));
    }
    
    private void sendDiscordCallbackHtml(HttpExchange exchange, boolean success, String message, String discordUsername) throws IOException {
        String serverName = plugin.getConfig().getString("web_server_prefix", "Server");
        String statusIcon = success ? "✓" : "✗";
        String statusColor = success ? "#4ade80" : "#f87171";
        String statusText = success ? "Success" : "Failed";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>Discord Link - ").append(escapeHtml(serverName)).append("</title>");
        html.append("<style>");
        html.append("*{margin:0;padding:0;box-sizing:border-box}");
        html.append("body{min-height:100vh;display:flex;align-items:center;justify-content:center;");
        html.append("background:linear-gradient(135deg,#1a1a2e 0%,#16213e 50%,#0f3460 100%);");
        html.append("font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#fff}");
        html.append(".card{background:rgba(255,255,255,0.1);backdrop-filter:blur(10px);");
        html.append("border:1px solid rgba(255,255,255,0.2);border-radius:16px;padding:40px;");
        html.append("text-align:center;max-width:400px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,0.3)}");
        html.append(".icon{font-size:64px;margin-bottom:20px;color:").append(statusColor).append("}");
        html.append(".title{font-size:24px;font-weight:600;margin-bottom:8px;color:").append(statusColor).append("}");
        html.append(".message{color:rgba(255,255,255,0.8);margin-bottom:20px;line-height:1.5}");
        html.append(".discord-user{background:rgba(88,101,242,0.3);border:1px solid rgba(88,101,242,0.5);");
        html.append("border-radius:8px;padding:12px 20px;margin-bottom:20px;display:inline-flex;align-items:center;gap:8px}");
        html.append(".discord-icon{width:24px;height:24px}");
        html.append(".btn{display:inline-block;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);");
        html.append("color:#fff;text-decoration:none;padding:12px 32px;border-radius:8px;font-weight:500;");
        html.append("transition:transform 0.2s,box-shadow 0.2s}");
        html.append(".btn:hover{transform:translateY(-2px);box-shadow:0 4px 20px rgba(102,126,234,0.4)}");
        html.append("</style></head><body>");
        html.append("<div class=\"card\">");
        html.append("<div class=\"icon\">").append(statusIcon).append("</div>");
        html.append("<div class=\"title\">").append(statusText).append("</div>");
        html.append("<div class=\"message\">").append(escapeHtml(message)).append("</div>");
        
        if (success && discordUsername != null) {
            html.append("<div class=\"discord-user\">");
            html.append("<svg class=\"discord-icon\" viewBox=\"0 0 24 24\" fill=\"#5865F2\">");
            html.append("<path d=\"M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03z\"/>");
            html.append("</svg>");
            html.append("<span>").append(escapeHtml(discordUsername)).append("</span>");
            html.append("</div>");
        }
        
        html.append("<a href=\"/\" class=\"btn\">Return to Registration</a>");
        html.append("</div></body></html>");
        
        byte[] data = html.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
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
