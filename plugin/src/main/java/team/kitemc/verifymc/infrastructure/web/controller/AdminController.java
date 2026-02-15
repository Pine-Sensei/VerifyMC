package team.kitemc.verifymc.infrastructure.web.controller;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.domain.service.AuthmeIntegrationService;
import team.kitemc.verifymc.domain.service.PageResult;
import team.kitemc.verifymc.domain.service.ReviewService;
import team.kitemc.verifymc.domain.service.UserService;
import team.kitemc.verifymc.infrastructure.annotation.Service;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;
import team.kitemc.verifymc.infrastructure.web.ApiResponse;
import team.kitemc.verifymc.infrastructure.web.RequestContext;
import team.kitemc.verifymc.infrastructure.web.RouteHandler;
import team.kitemc.verifymc.service.VersionCheckService;
import team.kitemc.verifymc.web.WebAuthHelper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class AdminController implements RouteHandler {

    private final Plugin plugin;
    private final UserService userService;
    private final ReviewService reviewService;
    private final ConfigurationService configService;
    private final WebAuthHelper webAuthHelper;
    private final AuthmeIntegrationService authmeIntegrationService;
    private final boolean debug;

    public AdminController(Plugin plugin,
                           UserService userService,
                           ReviewService reviewService,
                           ConfigurationService configService,
                           WebAuthHelper webAuthHelper,
                           AuthmeIntegrationService authmeIntegrationService) {
        this.plugin = plugin;
        this.userService = userService;
        this.reviewService = reviewService;
        this.configService = configService;
        this.webAuthHelper = webAuthHelper;
        this.authmeIntegrationService = authmeIntegrationService;
        this.debug = configService.getBoolean("debug", false);
    }

    @Override
    public void handle(RequestContext ctx) throws Exception {
        String path = ctx.getPath();

        switch (path) {
            case "/api/admin-login":
                handleAdminLogin(ctx);
                break;
            case "/api/admin-verify":
                handleAdminVerify(ctx);
                break;
            case "/api/users-paginated":
                handleUsersPaginated(ctx);
                break;
            case "/api/all-users":
                handleAllUsers(ctx);
                break;
            case "/api/delete-user":
                handleDeleteUser(ctx);
                break;
            case "/api/ban-user":
                handleBanUser(ctx);
                break;
            case "/api/unban-user":
                handleUnbanUser(ctx);
                break;
            case "/api/change-password":
                handleChangePassword(ctx);
                break;
            case "/api/user-status":
                handleUserStatus(ctx);
                break;
            case "/api/reload-config":
                handleReloadConfig(ctx);
                break;
            case "/api/version-check":
                handleVersionCheck(ctx);
                break;
            default:
                ctx.sendNotFound("Endpoint not found");
                break;
        }
    }

    private void handleAdminLogin(RequestContext ctx) throws Exception {
        if (!"POST".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        JSONObject body = ctx.getBody();
        String password = body.optString("password", "");

        String adminPassword = configService.getString("admin.password", "");

        JSONObject resp = new JSONObject();
        if (adminPassword != null && !adminPassword.isEmpty() && adminPassword.equals(password)) {
            String token = webAuthHelper.generateSecureToken();
            resp.put("success", true);
            resp.put("token", token);
            resp.put("msg", "Login successful");
        } else {
            resp.put("success", false);
            resp.put("msg", "admin.invalid_password");
        }

        ctx.sendJson(resp);
    }

    private void handleAdminVerify(RequestContext ctx) throws Exception {
        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        JSONObject resp = new JSONObject();
        resp.put("success", true);
        resp.put("msg", "Token is valid");
        ctx.sendJson(resp);
    }

    private void handleUsersPaginated(RequestContext ctx) throws Exception {
        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        int page = ctx.getQueryParamAsInt("page", 1);
        int pageSize = ctx.getQueryParamAsInt("pageSize", 10);
        String search = ctx.getQueryParam("search", "");

        if (pageSize > 100) pageSize = 100;
        if (pageSize < 1) pageSize = 10;
        if (page < 1) page = 1;

        JSONObject resp = new JSONObject();

        try {
            PageResult<User> pageResult;
            if (search != null && !search.trim().isEmpty()) {
                pageResult = userService.searchUsers(search, page, pageSize);
            } else {
                pageResult = userService.getUsersWithPagination(page, pageSize);
            }

            org.json.JSONArray usersArray = new org.json.JSONArray();
            for (User user : pageResult.getItems()) {
                if (user.getStatus() != UserStatus.PENDING) {
                    usersArray.put(userToJson(user));
                }
            }

            JSONObject pagination = new JSONObject();
            pagination.put("currentPage", pageResult.getPage());
            pagination.put("pageSize", pageResult.getPageSize());
            pagination.put("totalCount", pageResult.getTotal());
            pagination.put("totalPages", pageResult.getTotalPages());
            pagination.put("hasNext", pageResult.hasNext());
            pagination.put("hasPrev", pageResult.hasPrev());

            resp.put("success", true);
            resp.put("users", usersArray);
            resp.put("pagination", pagination);

        } catch (Exception e) {
            debugLog("Error in paginated users API: " + e.getMessage());
            resp.put("success", false);
            resp.put("message", "admin.load_failed");
        }

        ctx.sendJson(resp);
    }

    private void handleAllUsers(RequestContext ctx) throws Exception {
        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        JSONObject resp = new JSONObject();

        try {
            List<User> users = userService.getAllUsers();
            org.json.JSONArray usersArray = new org.json.JSONArray();

            for (User user : users) {
                if (user.getStatus() != UserStatus.PENDING) {
                    usersArray.put(userToJson(user));
                }
            }

            resp.put("success", true);
            resp.put("users", usersArray);
        } catch (Exception e) {
            debugLog("Error getting all users: " + e.getMessage());
            resp.put("success", false);
            resp.put("message", "admin.load_failed");
        }

        ctx.sendJson(resp);
    }

    private void handleDeleteUser(RequestContext ctx) throws Exception {
        if (!"POST".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        JSONObject body = ctx.getBody();
        String uuid = body.optString("uuid", "").trim();

        if (!isValidUUID(uuid)) {
            ctx.sendJson(ApiResponse.error(
                team.kitemc.verifymc.infrastructure.exception.ErrorCode.VALIDATION_ERROR,
                "Invalid UUID format"
            ).toJSONObject());
            return;
        }

        JSONObject resp = new JSONObject();

        try {
            Optional<User> userOpt = userService.getUserByUuid(uuid);
            if (userOpt.isEmpty()) {
                resp.put("success", false);
                resp.put("msg", "admin.user_not_found");
                ctx.sendJson(resp);
                return;
            }

            User user = userOpt.get();
            boolean success = userService.deleteUser(uuid);

            if (success) {
                syncToWhitelist(user.getUsername(), false);
                if (authmeIntegrationService.isEnabled()) {
                    authmeIntegrationService.unregisterFromAuthme(user.getUsername());
                }
            }

            resp.put("success", success);
            resp.put("msg", success ? "admin.delete_success" : "admin.delete_failed");

        } catch (Exception e) {
            debugLog("Delete user error: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", "admin.delete_failed");
        }

        ctx.sendJson(resp);
    }

    private void handleBanUser(RequestContext ctx) throws Exception {
        if (!"POST".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        JSONObject body = ctx.getBody();
        String uuid = body.optString("uuid", "").trim();

        if (!isValidUUID(uuid)) {
            ctx.sendJson(ApiResponse.error(
                team.kitemc.verifymc.infrastructure.exception.ErrorCode.VALIDATION_ERROR,
                "Invalid UUID format"
            ).toJSONObject());
            return;
        }

        JSONObject resp = new JSONObject();

        try {
            Optional<User> userOpt = userService.getUserByUuid(uuid);
            if (userOpt.isEmpty()) {
                resp.put("success", false);
                resp.put("msg", "admin.user_not_found");
                ctx.sendJson(resp);
                return;
            }

            User user = userOpt.get();
            boolean success = userService.updateUserStatus(uuid, UserStatus.BANNED);

            if (success) {
                syncToWhitelist(user.getUsername(), false);
                if (authmeIntegrationService.isEnabled()) {
                    authmeIntegrationService.unregisterFromAuthme(user.getUsername());
                }
            }

            resp.put("success", success);
            resp.put("msg", success ? "admin.ban_success" : "admin.ban_failed");

        } catch (Exception e) {
            debugLog("Ban user error: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", "admin.ban_failed");
        }

        ctx.sendJson(resp);
    }

    private void handleUnbanUser(RequestContext ctx) throws Exception {
        if (!"POST".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        JSONObject body = ctx.getBody();
        String uuid = body.optString("uuid", "").trim();

        if (!isValidUUID(uuid)) {
            ctx.sendJson(ApiResponse.error(
                team.kitemc.verifymc.infrastructure.exception.ErrorCode.VALIDATION_ERROR,
                "Invalid UUID format"
            ).toJSONObject());
            return;
        }

        JSONObject resp = new JSONObject();

        try {
            Optional<User> userOpt = userService.getUserByUuid(uuid);
            if (userOpt.isEmpty()) {
                resp.put("success", false);
                resp.put("msg", "admin.user_not_found");
                ctx.sendJson(resp);
                return;
            }

            User user = userOpt.get();
            boolean success = userService.updateUserStatus(uuid, UserStatus.APPROVED);

            if (success) {
                syncToWhitelist(user.getUsername(), true);
                if (authmeIntegrationService.isEnabled() && user.getPassword() != null && !user.getPassword().isEmpty()) {
                    authmeIntegrationService.registerToAuthme(user.getUsername(), user.getPassword());
                }
            }

            resp.put("success", success);
            resp.put("msg", success ? "admin.unban_success" : "admin.unban_failed");

        } catch (Exception e) {
            debugLog("Unban user error: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", "admin.unban_failed");
        }

        ctx.sendJson(resp);
    }

    private void handleChangePassword(RequestContext ctx) throws Exception {
        if (!"POST".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        JSONObject body = ctx.getBody();
        String uuid = body.optString("uuid", "").trim();
        String username = body.optString("username", "").trim();
        String newPassword = body.optString("newPassword", "");

        JSONObject resp = new JSONObject();

        if (uuid.isEmpty() && username.isEmpty()) {
            resp.put("success", false);
            resp.put("msg", "admin.missing_user_identifier");
            ctx.sendJson(resp);
            return;
        }

        if (newPassword.isEmpty()) {
            resp.put("success", false);
            resp.put("msg", "admin.password_required");
            ctx.sendJson(resp);
            return;
        }

        if (!authmeIntegrationService.isValidPassword(newPassword)) {
            resp.put("success", false);
            resp.put("msg", "admin.invalid_password");
            ctx.sendJson(resp);
            return;
        }

        try {
            Optional<User> userOpt;
            if (!uuid.isEmpty()) {
                userOpt = userService.getUserByUuid(uuid);
            } else {
                userOpt = userService.getUserByUsername(username);
            }

            if (userOpt.isEmpty()) {
                resp.put("success", false);
                resp.put("msg", "admin.user_not_found");
                ctx.sendJson(resp);
                return;
            }

            User user = userOpt.get();
            String storedPassword = authmeIntegrationService.encodePasswordForStorage(newPassword);
            boolean success = userService.updateUserPassword(user.getUuid(), storedPassword);

            if (success && authmeIntegrationService.isEnabled()) {
                authmeIntegrationService.changePasswordInAuthme(user.getUsername(), newPassword);
            }

            resp.put("success", success);
            resp.put("msg", success ? "admin.password_change_success" : "admin.password_change_failed");

        } catch (Exception e) {
            debugLog("Change password error: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", "admin.password_change_failed");
        }

        ctx.sendJson(resp);
    }

    private void handleUserStatus(RequestContext ctx) throws Exception {
        if (!"GET".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        String uuid = ctx.getQueryParam("uuid", "");

        if (!isValidUUID(uuid)) {
            JSONObject resp = new JSONObject();
            resp.put("success", false);
            resp.put("message", "Invalid UUID format");
            ctx.sendJson(resp);
            return;
        }

        JSONObject resp = new JSONObject();

        try {
            Optional<User> userOpt = userService.getUserByUuid(uuid);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                resp.put("success", true);
                JSONObject data = new JSONObject();
                data.put("status", user.getStatus().name().toLowerCase());
                data.put("reason", user.getReason() != null ? user.getReason() : "");
                resp.put("data", data);
            } else {
                resp.put("success", false);
                resp.put("message", "User not found");
            }
        } catch (Exception e) {
            debugLog("Get user status error: " + e.getMessage());
            resp.put("success", false);
            resp.put("message", "Failed to get user status");
        }

        ctx.sendJson(resp);
    }

    private void handleReloadConfig(RequestContext ctx) throws Exception {
        if (!"POST".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        JSONObject resp = new JSONObject();

        try {
            configService.reload();
            resp.put("success", true);
            resp.put("message", "Configuration reloaded successfully");
        } catch (Exception e) {
            debugLog("Reload config error: " + e.getMessage());
            resp.put("success", false);
            resp.put("message", "Failed to reload configuration: " + e.getMessage());
        }

        ctx.sendJson(resp);
    }

    private void handleVersionCheck(RequestContext ctx) throws Exception {
        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        if (!"GET".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        JSONObject resp = new JSONObject();

        try {
            if (plugin instanceof team.kitemc.verifymc.VerifyMC) {
                team.kitemc.verifymc.VerifyMC mainPlugin = (team.kitemc.verifymc.VerifyMC) plugin;
                VersionCheckService versionService = mainPlugin.getVersionCheckService();

                if (versionService != null) {
                    VersionCheckService.UpdateCheckResult result = versionService.checkForUpdatesAsync().get();
                    resp.put("success", true);
                    resp.put("current_version", result.getCurrentVersion());
                    resp.put("latest_version", result.getLatestVersion());
                    resp.put("has_update", result.isUpdateAvailable());
                    resp.put("download_url", versionService.getReleasesUrl());
                } else {
                    resp.put("success", false);
                    resp.put("error", "Version check service not available");
                }
            } else {
                resp.put("success", false);
                resp.put("error", "Version check service not available");
            }
        } catch (Exception e) {
            debugLog("Version check error: " + e.getMessage());
            resp.put("success", false);
            resp.put("error", "Internal server error");
        }

        ctx.sendJson(resp);
    }

    private void syncToWhitelist(String username, boolean add) {
        if (username == null || username.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            String command = add ? "whitelist add " + username : "whitelist remove " + username;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            debugLog("Executed: " + command);
        });
    }

    private JSONObject userToJson(User user) {
        JSONObject json = new JSONObject();
        json.put("uuid", user.getUuid());
        json.put("username", user.getUsername());
        json.put("email", user.getEmail() != null ? user.getEmail() : "");
        json.put("status", user.getStatus().name().toLowerCase());
        json.put("regTime", user.getRegTime());
        json.put("questionnaire_score", user.getQuestionnaireScore());
        json.put("questionnaire_review_summary", user.getQuestionnaireReviewSummary());
        return json;
    }

    private boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] AdminController: " + msg);
        }
    }
}
