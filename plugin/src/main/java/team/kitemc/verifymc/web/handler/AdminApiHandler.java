package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.web.ApiResponse;
import team.kitemc.verifymc.web.WebAuthHelper;
import team.kitemc.verifymc.web.WebResponseHelper;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.service.AuthmeService;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class AdminApiHandler implements HttpHandler {
    private final Plugin plugin;
    private final WebAuthHelper authHelper;
    private final UserDao userDao;
    private final AuthmeService authmeService;
    private final BiFunction<String, String, String> messageResolver;
    private final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    
    public AdminApiHandler(Plugin plugin, WebAuthHelper authHelper, UserDao userDao, 
                           AuthmeService authmeService, BiFunction<String, String, String> messageResolver) {
        this.plugin = plugin;
        this.authHelper = authHelper;
        this.userDao = userDao;
        this.authmeService = authmeService;
        this.messageResolver = messageResolver;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        if (!authHelper.isAuthenticated(exchange)) {
            sendResponse(exchange, 401, ApiResponse.failure("Authentication required"));
            return;
        }
        
        try {
            if (path.equals("/api/pending-list") && method.equals("GET")) {
                handlePendingList(exchange);
            } else if (path.equals("/api/review") && method.equals("POST")) {
                handleReview(exchange);
            } else if (path.equals("/api/delete-user") && method.equals("POST")) {
                handleDeleteUser(exchange);
            } else if (path.equals("/api/ban-user") && method.equals("POST")) {
                handleBanUser(exchange);
            } else if (path.equals("/api/unban-user") && method.equals("POST")) {
                handleUnbanUser(exchange);
            } else if (path.equals("/api/change-password") && method.equals("POST")) {
                handleChangePassword(exchange);
            } else if (path.equals("/api/users-paginated") && method.equals("GET")) {
                handleUsersPaginated(exchange);
            } else if (path.equals("/api/all-users") && method.equals("GET")) {
                handleAllUsers(exchange);
            } else {
                sendResponse(exchange, 404, ApiResponse.failure("Not found"));
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, ApiResponse.failure("Internal server error: " + e.getMessage()));
        }
    }
    
    private void handlePendingList(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String language = getQueryParam(query, "language", "en");
        
        List<Map<String, Object>> users = userDao.getPendingUsers();
        for (Map<String, Object> user : users) {
            if (!user.containsKey("regTime")) user.put("regTime", null);
            if (!user.containsKey("email")) user.put("email", "");
            if (!user.containsKey("questionnaire_score")) user.put("questionnaire_score", null);
            if (!user.containsKey("questionnaire_review_summary")) user.put("questionnaire_review_summary", null);
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("users", users);
        sendResponse(exchange, 200, ApiResponse.success("ok", data));
    }
    
    private void handleReview(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String uuid = req.optString("uuid");
        String action = req.optString("action");
        String reason = req.optString("reason", "");
        String language = req.optString("language", "en");
        
        if (!isValidUUID(uuid)) {
            sendResponse(exchange, 400, ApiResponse.failure("Invalid UUID format"));
            return;
        }
        
        if (!"approve".equals(action) && !"reject".equals(action)) {
            sendResponse(exchange, 400, ApiResponse.failure("Invalid action"));
            return;
        }
        
        Map<String, Object> user = userDao.getUserByUuid(uuid);
        if (user == null) {
            sendResponse(exchange, 404, ApiResponse.failure(messageResolver.apply("admin.user_not_found", language)));
            return;
        }
        
        String username = (String) user.get("username");
        String password = (String) user.get("password");
        String status = "approve".equals(action) ? "approved" : "rejected";
        boolean success = userDao.updateUserStatus(uuid, status);
        
        if (success && username != null) {
            if ("approve".equals(action)) {
                executeWhitelistCommand("whitelist add " + username);
                if (authmeService.isAuthmeEnabled() && password != null && !password.trim().isEmpty()) {
                    authmeService.registerToAuthme(username, password);
                }
            } else {
                executeWhitelistCommand("whitelist remove " + username);
                if (authmeService.isAuthmeEnabled()) {
                    authmeService.unregisterFromAuthme(username);
                }
            }
        }
        
        if (success) {
            sendResponse(exchange, 200, ApiResponse.success(
                "approve".equals(action) ? messageResolver.apply("admin.approve_success", language) : messageResolver.apply("admin.reject_success", language)
            ));
        } else {
            sendResponse(exchange, 500, ApiResponse.failure(messageResolver.apply("admin.review_failed", language)));
        }
    }
    
    private void handleDeleteUser(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String uuid = req.optString("uuid");
        String language = req.optString("language", "en");
        
        if (!isValidUUID(uuid)) {
            sendResponse(exchange, 400, ApiResponse.failure("Invalid UUID format"));
            return;
        }
        
        Map<String, Object> user = userDao.getUserByUuid(uuid);
        if (user == null) {
            sendResponse(exchange, 404, ApiResponse.failure(messageResolver.apply("admin.user_not_found", language)));
            return;
        }
        
        String username = (String) user.get("username");
        boolean success = userDao.deleteUser(uuid);
        
        if (success && username != null) {
            executeWhitelistCommand("whitelist remove " + username);
            if (authmeService.isAuthmeEnabled()) {
                authmeService.unregisterFromAuthme(username);
            }
        }
        
        if (success) {
            sendResponse(exchange, 200, ApiResponse.success(messageResolver.apply("admin.delete_success", language)));
        } else {
            sendResponse(exchange, 500, ApiResponse.failure(messageResolver.apply("admin.delete_failed", language)));
        }
    }
    
    private void handleBanUser(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String uuid = req.optString("uuid");
        String language = req.optString("language", "en");
        
        if (!isValidUUID(uuid)) {
            sendResponse(exchange, 400, ApiResponse.failure("Invalid UUID format"));
            return;
        }
        
        Map<String, Object> user = userDao.getUserByUuid(uuid);
        if (user == null) {
            sendResponse(exchange, 404, ApiResponse.failure(messageResolver.apply("admin.user_not_found", language)));
            return;
        }
        
        String username = (String) user.get("username");
        boolean success = userDao.updateUserStatus(uuid, "banned");
        
        if (success && username != null) {
            executeWhitelistCommand("whitelist remove " + username);
            if (authmeService.isAuthmeEnabled()) {
                authmeService.unregisterFromAuthme(username);
            }
        }
        
        if (success) {
            sendResponse(exchange, 200, ApiResponse.success(messageResolver.apply("admin.ban_success", language)));
        } else {
            sendResponse(exchange, 500, ApiResponse.failure(messageResolver.apply("admin.ban_failed", language)));
        }
    }
    
    private void handleUnbanUser(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String uuid = req.optString("uuid");
        String language = req.optString("language", "en");
        
        if (!isValidUUID(uuid)) {
            sendResponse(exchange, 400, ApiResponse.failure("Invalid UUID format"));
            return;
        }
        
        Map<String, Object> user = userDao.getUserByUuid(uuid);
        if (user == null) {
            sendResponse(exchange, 404, ApiResponse.failure(messageResolver.apply("admin.user_not_found", language)));
            return;
        }
        
        String username = (String) user.get("username");
        String password = (String) user.get("password");
        boolean success = userDao.updateUserStatus(uuid, "approved");
        
        if (success && username != null) {
            executeWhitelistCommand("whitelist add " + username);
            if (authmeService.isAuthmeEnabled() && password != null && !password.trim().isEmpty()) {
                authmeService.registerToAuthme(username, password);
            }
        }
        
        if (success) {
            sendResponse(exchange, 200, ApiResponse.success(messageResolver.apply("admin.unban_success", language)));
        } else {
            sendResponse(exchange, 500, ApiResponse.failure(messageResolver.apply("admin.unban_failed", language)));
        }
    }
    
    private void handleChangePassword(HttpExchange exchange) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        String uuid = req.optString("uuid");
        String username = req.optString("username");
        String newPassword = req.optString("newPassword");
        String language = req.optString("language", "en");
        
        if ((uuid == null || uuid.trim().isEmpty()) && (username == null || username.trim().isEmpty())) {
            sendResponse(exchange, 400, ApiResponse.failure(messageResolver.apply("admin.missing_user_identifier", language)));
            return;
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            sendResponse(exchange, 400, ApiResponse.failure(messageResolver.apply("admin.password_required", language)));
            return;
        }
        
        if (!authmeService.isValidPassword(newPassword)) {
            String passwordRegex = plugin.getConfig().getString("authme.password_regex", "^[a-zA-Z0-9_]{8,26}$");
            sendResponse(exchange, 400, ApiResponse.failure(messageResolver.apply("admin.invalid_password", language).replace("{regex}", passwordRegex)));
            return;
        }
        
        Map<String, Object> user = null;
        if (uuid != null && !uuid.trim().isEmpty()) {
            user = userDao.getUserByUuid(uuid);
        } else if (username != null && !username.trim().isEmpty()) {
            user = userDao.getUserByUsername(username);
        }
        
        if (user == null) {
            sendResponse(exchange, 404, ApiResponse.failure(messageResolver.apply("admin.user_not_found", language)));
            return;
        }
        
        String targetUsername = (String) user.get("username");
        String targetUuid = (String) user.get("uuid");
        
        String storedPassword = authmeService.encodePasswordForStorage(newPassword);
        boolean success = userDao.updateUserPassword(targetUuid, storedPassword);
        
        if (success) {
            if (authmeService.isAuthmeEnabled()) {
                authmeService.changePasswordInAuthme(targetUsername, newPassword);
            }
            sendResponse(exchange, 200, ApiResponse.success(messageResolver.apply("admin.password_change_success", language)));
        } else {
            sendResponse(exchange, 500, ApiResponse.failure(messageResolver.apply("admin.password_change_failed", language)));
        }
    }
    
    private void handleUsersPaginated(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int page = Math.max(1, getIntParam(query, "page", 1));
        int pageSize = Math.max(1, Math.min(100, getIntParam(query, "pageSize", 10)));
        String searchQuery = getQueryParam(query, "search", "");
        String language = getQueryParam(query, "language", "en");
        
        List<Map<String, Object>> users;
        int totalCount;
        
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            users = userDao.getApprovedUsersWithPaginationAndSearch(page, pageSize, searchQuery);
            totalCount = userDao.getApprovedUserCountWithSearch(searchQuery);
        } else {
            users = userDao.getApprovedUsersWithPagination(page, pageSize);
            totalCount = userDao.getApprovedUserCount();
        }
        
        for (Map<String, Object> user : users) {
            if (!user.containsKey("regTime")) user.put("regTime", null);
            if (!user.containsKey("email")) user.put("email", "");
            if (!user.containsKey("questionnaire_score")) user.put("questionnaire_score", null);
            if (!user.containsKey("questionnaire_review_summary")) user.put("questionnaire_review_summary", null);
        }
        
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page);
        pagination.put("pageSize", pageSize);
        pagination.put("totalCount", totalCount);
        pagination.put("totalPages", totalPages);
        pagination.put("hasNext", page < totalPages);
        pagination.put("hasPrev", page > 1);
        
        Map<String, Object> data = new HashMap<>();
        data.put("users", users);
        data.put("pagination", pagination);
        
        sendResponse(exchange, 200, ApiResponse.success("ok", data));
    }
    
    private void handleAllUsers(HttpExchange exchange) throws IOException {
        String language = "en";
        
        List<Map<String, Object>> users = userDao.getAllUsers();
        java.util.List<Map<String, Object>> filtered = new java.util.ArrayList<>();
        for (Map<String, Object> user : users) {
            if (!"pending".equalsIgnoreCase(String.valueOf(user.get("status")))) {
                if (!user.containsKey("regTime")) user.put("regTime", null);
                if (!user.containsKey("email")) user.put("email", "");
                filtered.add(user);
            }
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("users", filtered);
        sendResponse(exchange, 200, ApiResponse.success("ok", data));
    }
    
    private void executeWhitelistCommand(String command) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
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
    
    private String getQueryParam(String query, String param, String defaultValue) {
        if (query == null) return defaultValue;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(param)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return defaultValue;
    }
    
    private int getIntParam(String query, String param, int defaultValue) {
        String value = getQueryParam(query, param, null);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
