package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;
import team.kitemc.verifymc.core.OpsManager;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.core.ConfigManager;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.service.AccountSelectionService;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.security.AdminAuthMode;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebAuthHelper;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginHandler implements HttpHandler {
    private static final int MAX_ATTEMPTS_PER_IP = 5;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;

    private final PluginContext ctx;
    private final boolean isAdminLogin;
    private final ConcurrentHashMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    private static class LoginAttempt {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
    }

    public LoginHandler(PluginContext ctx) {
        this(ctx, false);
    }

    public LoginHandler(PluginContext ctx, boolean isAdminLogin) {
        this.ctx = ctx;
        this.isAdminLogin = isAdminLogin;
    }

    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        LoginAttempt attempt = loginAttempts.compute(ip, (k, v) -> {
            if (v == null || (now - v.windowStart) > RATE_LIMIT_WINDOW_MS) {
                LoginAttempt fresh = new LoginAttempt();
                fresh.windowStart = now;
                fresh.count.set(1);
                return fresh;
            }
            v.count.incrementAndGet();
            return v;
        });
        return attempt.count.get() > MAX_ATTEMPTS_PER_IP;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (isRateLimited(clientIp)) {
            ctx.getPlugin().getLogger().warning("[Security] Login rate limit exceeded for IP: " + clientIp);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    "Too many login attempts. Please try again later."), 429);
            return;
        }

        JSONObject req;
        try {
            req = WebResponseHelper.readJson(exchange);
        } catch (JSONException e) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("error.invalid_json", "en")), 400);
            return;
        }
        String identifierInput = req.optString("identifier", req.optString("username", ""));
        ConfigManager.VerifyIdentifier identifierType = AuthFlowSupport.parseIdentifier(req.optString("identifierType", inferIdentifier(identifierInput)));
        String authMethod = req.optString("authMethod", req.has("code") ? "code" : "password").trim().toLowerCase();
        String password = req.optString("password", "");
        String code = req.optString("code", "");
        String selectedUsername = req.optString("selectedUsername", "").trim();
        String selectionToken = req.optString("selectionToken", "").trim();
        String language = req.optString("language", "en");

        if (!isConfigured(identifierType, authMethod)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.method_not_enabled", language)), 400);
            return;
        }

        if (identifierType == ConfigManager.VerifyIdentifier.PHONE
                && AuthFlowSupport.missingRequiredCountryCode(identifierInput, req.optString("countryCode", ""))) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("sms.country_code_required", language)), 400);
            return;
        }

        String identifier = AuthFlowSupport.normalizeIdentifier(identifierType, identifierInput, req.optString("countryCode", ""));

        OpsManager opsManager = ctx.getOpsManager();
        if (isAdminLogin && ctx.getConfigManager().getAdminAuthMode() == AdminAuthMode.OP
                && (opsManager == null || opsManager.getOps().isEmpty())) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.system_error", language)));
            return;
        }

        UserDao userDao = ctx.getUserDao();
        List<Map<String, Object>> matches = AuthFlowSupport.findUsers(userDao, identifierType, identifier);

        if (matches.isEmpty()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.user_not_found", language)));
            return;
        }

        List<Map<String, Object>> eligibleMatches = matches;
        if (isAdminLogin) {
            eligibleMatches = matches.stream()
                    .filter(user -> ctx.getAdminAccessManager().hasAnyAdminAccess(String.valueOf(user.get("username"))))
                    .toList();
            if (eligibleMatches.isEmpty()) {
                ctx.getPlugin().getLogger().warning("[Security] Unauthorized admin login attempt for identifier: " + identifier);
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                        ctx.getMessage("login.not_authorized", language)));
                return;
            }
        }

        if (!selectionToken.isBlank()) {
            AccountSelectionService.ConsumeResult selection = ctx.getAccountSelectionService().consume(
                    selectionToken,
                    AccountSelectionService.Purpose.LOGIN,
                    identifierType.configPrefix(),
                    identifier,
                    selectedUsername);
            if (!selection.valid()) {
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                        ctx.getMessage("login.failed", language)), 409);
                return;
            }

            Map<String, Object> selectedUser = userDao.getUserByUsernameExact(selection.username());
            if (selectedUser == null) {
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                        ctx.getMessage("login.failed", language)), 409);
                return;
            }

            completeLogin(exchange, selectedUser, language);
            return;
        }

        if ("code".equals(authMethod)) {
            VerifyCodeService.Channel channel = identifierType == ConfigManager.VerifyIdentifier.PHONE
                    ? VerifyCodeService.Channel.SMS
                    : VerifyCodeService.Channel.EMAIL;
            VerifyCodeService.VerifyResult result = ctx.getVerifyCodeService()
                    .verifyCode(channel, VerifyCodeService.Purpose.LOGIN, identifier, code);
            if (!result.success()) {
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                        ctx.getMessage("verify.wrong_code", language)));
                return;
            }
        } else {
            Map<String, Object> verifiedUser = AuthFlowSupport.verifyPasswordAgainstUsers(ctx, eligibleMatches, password);
            if (verifiedUser == null) {
                ctx.getPlugin().getLogger().warning("[Security] Failed login attempt - Identifier: " + identifier + ", IP: " + clientIp + ", Reason: Invalid password");
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                        ctx.getMessage("login.failed", language)));
                return;
            }

            List<Map<String, Object>> sharedUsers = AuthFlowSupport.resolveSharedPasswordGroup(userDao, verifiedUser);
            AuthFlowSupport.SharedPasswordUpdateResult syncResult = AuthFlowSupport.synchronizeSharedPasswords(ctx, sharedUsers, password);
            if (!syncResult.success()) {
                ctx.getPlugin().getLogger().warning("[Security] Shared password synchronization failed - Identifier: " + identifier + ", IP: " + clientIp);
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                        ctx.getMessage("login.failed", language)));
                return;
            }
        }

        if (identifierType != ConfigManager.VerifyIdentifier.USERNAME && eligibleMatches.size() > 1) {
            JSONObject resp = AuthFlowSupport.accountSelectionRequiredResponse(
                    ctx,
                    AccountSelectionService.Purpose.LOGIN,
                    identifierType,
                    identifier,
                    eligibleMatches,
                    language);
            WebResponseHelper.sendJson(exchange, resp, 409);
            return;
        }

        Map<String, Object> user = eligibleMatches.get(0);
        completeLogin(exchange, user, language);
    }

    private String inferIdentifier(String identifier) {
        if (identifier == null) {
            return "username";
        }
        if (identifier.contains("@")) {
            return "email";
        }
        if (identifier.startsWith("+") || identifier.startsWith("00")) {
            return "phone";
        }
        return "username";
    }

    private boolean isConfigured(ConfigManager.VerifyIdentifier identifierType, String authMethod) {
        if ("code".equals(authMethod)) {
            return identifierType != ConfigManager.VerifyIdentifier.USERNAME && ctx.getConfigManager().isLoginCodeEnabled(identifierType);
        }
        return ctx.getConfigManager().isLoginPasswordEnabled(identifierType);
    }

    private void completeLogin(HttpExchange exchange, Map<String, Object> user, String language) throws IOException {
        String actualUsername = (String) user.get("username");
        if (actualUsername == null || actualUsername.isEmpty()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.user_not_found", language)));
            return;
        }

        if (isAdminLogin && !ctx.getAdminAccessManager().hasAnyAdminAccess(actualUsername)) {
            ctx.getPlugin().getLogger().warning("[Security] Unauthorized admin login attempt for user: " + actualUsername);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.not_authorized", language)));
            return;
        }

        WebAuthHelper webAuthHelper = ctx.getWebAuthHelper();
        String token = webAuthHelper.generateToken(actualUsername);
        boolean isAdmin = ctx.getAdminAccessManager().hasAnyAdminAccess(actualUsername);
        JSONObject resp = ApiResponseFactory.success(ctx.getMessage("login.success", language));
        resp.put("token", token);
        resp.put("username", actualUsername);
        resp.put("isAdmin", isAdmin);
        WebResponseHelper.sendJson(exchange, resp);
    }
}
