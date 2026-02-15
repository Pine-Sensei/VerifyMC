package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.web.ApiResponse;
import team.kitemc.verifymc.web.WebResponseHelper;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.mail.MailService;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class UserApiHandler implements HttpHandler {
    private final Plugin plugin;
    private final UserDao userDao;
    private final VerifyCodeService codeService;
    private final MailService mailService;
    private final BiFunction<String, String, String> messageResolver;
    private final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    
    public UserApiHandler(Plugin plugin, UserDao userDao, VerifyCodeService codeService, 
                          MailService mailService, BiFunction<String, String, String> messageResolver) {
        this.plugin = plugin;
        this.userDao = userDao;
        this.codeService = codeService;
        this.mailService = mailService;
        this.messageResolver = messageResolver;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (path.equals("/api/register") && method.equals("POST")) {
                handleRegister(exchange);
            } else if (path.equals("/api/send_code") && method.equals("POST")) {
                handleSendCode(exchange);
            } else if (path.equals("/api/user-status") && method.equals("GET")) {
                handleUserStatus(exchange);
            } else {
                sendResponse(exchange, 404, ApiResponse.failure("Not found"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Internal server error"));
        }
    }
    
    private void handleRegister(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String language = req.optString("language", "en");
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Registration endpoint - to be implemented with full logic");
        sendResponse(exchange, 200, ApiResponse.success("ok", result));
    }
    
    private void handleSendCode(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String email = req.optString("email", "").trim().toLowerCase();
        String language = req.optString("language", "en");
        
        if (!isValidEmail(email)) {
            sendResponse(exchange, 400, ApiResponse.failure(messageResolver.apply("email.invalid_format", language)));
            return;
        }
        
        if (!codeService.canSendCode(email)) {
            long remainingSeconds = codeService.getRemainingCooldownSeconds(email);
            Map<String, Object> data = new HashMap<>();
            data.put("remaining_seconds", remainingSeconds);
            sendResponse(exchange, 429, ApiResponse.failure(
                messageResolver.apply("email.rate_limited", language).replace("{seconds}", String.valueOf(remainingSeconds)), 
                data
            ));
            return;
        }
        
        String code = codeService.generateCode(email);
        String emailSubject = plugin.getConfig().getString("email_subject", "VerifyMC Verification Code");
        boolean sent = mailService.sendCode(email, emailSubject, code, language);
        
        if (sent) {
            sendResponse(exchange, 200, ApiResponse.success(messageResolver.apply("email.sent", language)));
        } else {
            sendResponse(exchange, 500, ApiResponse.failure(messageResolver.apply("email.failed", language)));
        }
    }
    
    private void handleUserStatus(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String uuid = getQueryParam(query, "uuid");
        
        if (uuid == null || uuid.isEmpty()) {
            sendResponse(exchange, 400, ApiResponse.failure("UUID parameter required"));
            return;
        }
        
        if (!isValidUUID(uuid)) {
            sendResponse(exchange, 400, ApiResponse.failure("Invalid UUID format"));
            return;
        }
        
        Map<String, Object> user = userDao.getUserByUuid(uuid);
        if (user != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("status", user.get("status"));
            data.put("reason", user.get("reason"));
            sendResponse(exchange, 200, ApiResponse.success("ok", data));
        } else {
            sendResponse(exchange, 404, ApiResponse.failure("User not found"));
        }
    }
    
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    private boolean isValidUUID(String uuid) {
        return uuid != null && UUID_PATTERN.matcher(uuid).matches();
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
