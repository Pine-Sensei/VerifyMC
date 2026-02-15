package team.kitemc.verifymc.infrastructure.web.controller;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.domain.service.MailIntegrationService;
import team.kitemc.verifymc.domain.service.ReviewService;
import team.kitemc.verifymc.domain.service.UserService;
import team.kitemc.verifymc.infrastructure.annotation.Service;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;
import team.kitemc.verifymc.infrastructure.web.ApiResponse;
import team.kitemc.verifymc.infrastructure.web.RequestContext;
import team.kitemc.verifymc.infrastructure.web.RouteHandler;
import team.kitemc.verifymc.web.ReviewWebSocketServer;
import team.kitemc.verifymc.web.WebAuthHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReviewController implements RouteHandler {

    private final Plugin plugin;
    private final ReviewService reviewService;
    private final UserService userService;
    private final WebAuthHelper webAuthHelper;
    private final MailIntegrationService mailIntegrationService;
    private final ConfigurationService configService;
    private final ReviewWebSocketServer wsServer;
    private final boolean debug;

    public ReviewController(Plugin plugin,
                            ReviewService reviewService,
                            UserService userService,
                            WebAuthHelper webAuthHelper,
                            MailIntegrationService mailIntegrationService,
                            ConfigurationService configService,
                            ReviewWebSocketServer wsServer) {
        this.plugin = plugin;
        this.reviewService = reviewService;
        this.userService = userService;
        this.webAuthHelper = webAuthHelper;
        this.mailIntegrationService = mailIntegrationService;
        this.configService = configService;
        this.wsServer = wsServer;
        this.debug = configService.getBoolean("debug", false);
    }

    @Override
    public void handle(RequestContext ctx) throws Exception {
        String path = ctx.getPath();

        if ("/api/pending-list".equals(path)) {
            handlePendingList(ctx);
        } else if ("/api/review".equals(path)) {
            handleReview(ctx);
        } else {
            ctx.sendNotFound("Endpoint not found");
        }
    }

    private void handlePendingList(RequestContext ctx) throws IOException {
        if (!webAuthHelper.isAuthenticated(ctx.getExchange())) {
            ctx.sendUnauthorized();
            return;
        }

        String language = ctx.getLanguage();
        JSONObject resp = new JSONObject();

        try {
            List<User> pendingUsers = reviewService.getPendingUsers();
            org.json.JSONArray usersArray = new org.json.JSONArray();

            for (User user : pendingUsers) {
                JSONObject userJson = new JSONObject();
                userJson.put("uuid", user.getUuid());
                userJson.put("username", user.getUsername());
                userJson.put("email", user.getEmail() != null ? user.getEmail() : "");
                userJson.put("status", user.getStatus().name().toLowerCase());
                userJson.put("regTime", user.getRegTime());
                userJson.put("questionnaire_score", user.getQuestionnaireScore());
                userJson.put("questionnaire_review_summary", user.getQuestionnaireReviewSummary());
                usersArray.put(userJson);
            }

            resp.put("success", true);
            resp.put("users", usersArray);
        } catch (Exception e) {
            debugLog("Failed to get pending users: " + e.getMessage());
            resp.put("success", false);
            resp.put("message", "admin.load_failed");
        }

        ctx.sendJson(resp);
    }

    private void handleReview(RequestContext ctx) throws IOException {
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
        String action = body.optString("action", "").trim();
        String reason = body.optString("reason", "").trim();
        String language = body.optString("language", "en");

        if (!isValidUUID(uuid)) {
            ctx.sendJson(ApiResponse.error(
                team.kitemc.verifymc.infrastructure.exception.ErrorCode.VALIDATION_ERROR,
                "Invalid UUID format"
            ).toJSONObject());
            return;
        }

        if (!"approve".equals(action) && !"reject".equals(action)) {
            ctx.sendJson(ApiResponse.error(
                team.kitemc.verifymc.infrastructure.exception.ErrorCode.VALIDATION_ERROR,
                "Invalid action"
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
            boolean approved = "approve".equals(action);
            boolean success;

            if (approved) {
                success = reviewService.approveUser(uuid, "admin", reason);
                if (success) {
                    syncToWhitelist(user.getUsername(), true);
                }
            } else {
                success = reviewService.rejectUser(uuid, "admin", reason);
                if (success) {
                    syncToWhitelist(user.getUsername(), false);
                }
            }

            if (success && user.getEmail() != null && !user.getEmail().isEmpty()) {
                sendReviewNotification(user.getEmail(), user.getUsername(), approved, reason, language);
            }

            resp.put("success", success);
            resp.put("msg", success ? 
                (approved ? "admin.approve_success" : "admin.reject_success") :
                (approved ? "admin.approve_failed" : "admin.reject_failed"));

            if (success && wsServer != null) {
                JSONObject wsMsg = new JSONObject();
                wsMsg.put("type", action);
                wsMsg.put("uuid", uuid);
                wsMsg.put("msg", resp.getString("msg"));
                wsServer.broadcastMessage(wsMsg.toString());
            }

        } catch (Exception e) {
            debugLog("Review error: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", "admin.review_failed");
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

    private void sendReviewNotification(String email, String username, boolean approved, String reason, String language) {
        new Thread(() -> {
            try {
                mailIntegrationService.sendReviewResultNotification(email, username, approved, reason, language);
            } catch (Exception e) {
                debugLog("Failed to send review notification: " + e.getMessage());
            }
        }).start();
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
            plugin.getLogger().info("[DEBUG] ReviewController: " + msg);
        }
    }
}
