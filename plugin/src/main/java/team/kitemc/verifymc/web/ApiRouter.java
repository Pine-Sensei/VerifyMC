package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpServer;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.handler.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Central API router that maps HTTP paths to modular handler classes.
 * <p>
 * Replaces the 1271-line {@code WebServer.start()} method which defined
 * all routes inline as anonymous HttpHandler classes.
 * <p>
 * Each endpoint group is now a dedicated handler class, making the
 * routing topology visible at a glance and each handler independently testable.
 */
public class ApiRouter {
    private final PluginContext ctx;
    private final ConcurrentHashMap<String, RegistrationProcessingHandler.QuestionnaireSubmissionRecord> questionnaireSubmissionStore;

    public ApiRouter(PluginContext ctx) {
        this.ctx = ctx;
        this.questionnaireSubmissionStore = new ConcurrentHashMap<>();
    }

    /**
     * Register all API routes on the given HttpServer.
     */
    public void registerRoutes(HttpServer server) {
        // --- Configuration endpoint ---
        server.createContext("/api/config", new ConfigHandler(ctx));

        // --- Captcha endpoints ---
        server.createContext("/api/captcha/generate", new CaptchaHandler(ctx));
        server.createContext("/api/captcha", new CaptchaHandler(ctx));

        // --- Email verification ---
        server.createContext("/api/verify/send", new VerifyCodeHandler(ctx));

        // --- Questionnaire endpoints ---
        server.createContext("/api/questionnaire/config", new QuestionnaireConfigHandler(ctx));
        server.createContext("/api/questionnaire/submit", new QuestionnaireSubmitHandler(ctx, questionnaireSubmissionStore));

        // --- Registration ---
        server.createContext("/api/register", new RegistrationProcessingHandler(
                ctx.getPlugin(),
                ctx.getVerifyCodeService(),
                ctx.getUserDao(),
                ctx.getAuthmeService(),
                ctx.getCaptchaService(),
                ctx.getQuestionnaireService(),
                ctx.getDiscordService(),
                ctx.getRegistrationApplicationService(),
                questionnaireSubmissionStore,
                () -> ctx.getConfigManager().getEmailDomainWhitelist(),
                (key, lang) -> ctx.getMessage(key, lang),
                (username, platform) -> resolveUsernameRegex(username, platform),
                (username, platform) -> isValidUsername(username, platform),
                (username) -> hasUsernameCaseConflict(username),
                () -> ctx.getConfigManager().isUsernameCaseSensitive(),
                (username, platform) -> normalizeUsername(username, platform),
                (email) -> isValidEmail(email),
                (uuid) -> isValidUuid(uuid),
                (msg) -> ctx.debugLog(msg)
        ));

        // --- Review status check ---
        server.createContext("/api/review/status", new ReviewStatusHandler(ctx));

        // --- Admin endpoints ---
        server.createContext("/api/admin/login", new AdminLoginHandler(ctx));
        server.createContext("/api/admin/verify", new AdminVerifyHandler(ctx));
        server.createContext("/api/admin/users", new AdminUserListHandler(ctx));
        server.createContext("/api/admin/user/approve", new AdminUserApproveHandler(ctx));
        server.createContext("/api/admin/user/reject", new AdminUserRejectHandler(ctx));
        server.createContext("/api/admin/user/delete", new AdminUserDeleteHandler(ctx));
        server.createContext("/api/admin/user/ban", new AdminUserBanHandler(ctx));
        server.createContext("/api/admin/user/unban", new AdminUserUnbanHandler(ctx));
        server.createContext("/api/admin/user/password", new AdminUserPasswordHandler(ctx));
        server.createContext("/api/admin/audits", new AdminAuditHandler(ctx));

        // --- Discord endpoints ---
        server.createContext("/api/discord/auth", new DiscordAuthHandler(ctx));
        server.createContext("/api/discord/callback", new DiscordCallbackHandler(ctx));
        server.createContext("/api/discord/status", new DiscordStatusHandler(ctx));
        server.createContext("/api/discord/unlink", new DiscordUnlinkHandler(ctx));

        // --- Version check ---
        server.createContext("/api/version", new VersionHandler(ctx));

        // --- User status query ---
        server.createContext("/api/user/status", new UserStatusHandler(ctx));

        // --- Static files (front-end) ---
        server.createContext("/", new StaticFileHandler(ctx));
    }

    // --- Utility methods used by route wiring (delegated from WebServer) ---

    private boolean isValidUsername(String username, String platform) {
        if (username == null || username.trim().isEmpty()) return false;
        String regex = resolveUsernameRegex(username, platform);
        return username.matches(regex);
    }

    private String resolveUsernameRegex(String username, String platform) {
        if ("bedrock".equalsIgnoreCase(platform) && ctx.getConfigManager().isBedrockEnabled()) {
            return ctx.getConfigManager().getBedrockUsernameRegex();
        }
        return ctx.getConfigManager().getUsernameRegex();
    }

    private boolean hasUsernameCaseConflict(String username) {
        // If username is case-sensitive, no conflict check needed
        if (ctx.getConfigManager().isUsernameCaseSensitive()) {
            return false;
        }
        // If username is case-insensitive, check for case conflicts
        var existing = ctx.getUserDao().getUserByUsername(username);
        if (existing == null) return false;
        String storedName = (String) existing.get("username");
        return storedName != null && !storedName.equals(username) && storedName.equalsIgnoreCase(username);
    }

    private String normalizeUsername(String username, String platform) {
        if (username == null) return "";
        String trimmed = username.trim();
        if ("bedrock".equalsIgnoreCase(platform) && ctx.getConfigManager().isBedrockEnabled()) {
            String prefix = ctx.getConfigManager().getBedrockPrefix();
            if (!trimmed.startsWith(prefix)) {
                trimmed = prefix + trimmed;
            }
        }
        return trimmed;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) return false;
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
