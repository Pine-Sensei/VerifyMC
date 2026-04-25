package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import team.kitemc.verifymc.core.ConfigManager;
import team.kitemc.verifymc.db.AuditRecord;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.sms.SmsSendResult;
import team.kitemc.verifymc.util.EmailAddressUtil;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

public class ForgotPasswordHandler implements HttpHandler {
    private final team.kitemc.verifymc.core.PluginContext ctx;
    private final boolean sendMode;

    public ForgotPasswordHandler(team.kitemc.verifymc.core.PluginContext ctx, boolean sendMode) {
        this.ctx = ctx;
        this.sendMode = sendMode;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        JSONObject req;
        try {
            req = WebResponseHelper.readJson(exchange);
        } catch (JSONException e) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("error.invalid_json", "en")), 400);
            return;
        }

        if (!ctx.getConfigManager().isForgotPasswordEnabled()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("forgot_password.not_enabled", req.optString("language", "en"))), 403);
            return;
        }

        if (sendMode) {
            sendCode(exchange, req);
        } else {
            resetPassword(exchange, req);
        }
    }

    private void sendCode(HttpExchange exchange, JSONObject req) throws IOException {
        String language = req.optString("language", "en");
        ConfigManager.VerifyIdentifier identifierType = resolveIdentifier(req);
        String identifier = normalize(exchange, req, identifierType, language);
        if (identifier == null) {
            return;
        }

        String method = identifierType == ConfigManager.VerifyIdentifier.PHONE ? "phone_code" : "email_code";
        if (!ctx.getConfigManager().isForgotPasswordMethodEnabled(method)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("forgot_password.method_not_enabled", language)), 400);
            return;
        }

        List<Map<String, Object>> users = AuthFlowSupport.findUsers(ctx.getUserDao(), identifierType, identifier);
        if (users.isEmpty()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("login.user_not_found", language)));
            return;
        }

        VerifyCodeService.Channel channel = identifierType == ConfigManager.VerifyIdentifier.PHONE
                ? VerifyCodeService.Channel.SMS
                : VerifyCodeService.Channel.EMAIL;
        String ip = exchange.getRemoteAddress() != null && exchange.getRemoteAddress().getAddress() != null
                ? exchange.getRemoteAddress().getAddress().getHostAddress()
                : "";
        VerifyCodeService.CodeIssueResult issue = ctx.getVerifyCodeService()
                .issueCode(channel, VerifyCodeService.Purpose.FORGOT_PASSWORD, identifier, ip);
        if (!issue.issued()) {
            JSONObject response = ApiResponseFactory.failure(ctx.getMessage(
                    channel == VerifyCodeService.Channel.SMS ? "sms.rate_limited" : "email.rate_limited", language)
                    .replace("{seconds}", String.valueOf(issue.remainingSeconds())));
            response.put("remainingSeconds", issue.remainingSeconds());
            WebResponseHelper.sendJson(exchange, response, 429);
            return;
        }

        boolean sent;
        if (channel == VerifyCodeService.Channel.SMS) {
            SmsSendResult result = ctx.getSmsService().sendVerificationCode(identifier, issue.code());
            sent = result.success();
        } else {
            sent = ctx.getMailService().sendVerificationCode(identifier, issue.code(), language);
        }

        if (!sent) {
            ctx.getVerifyCodeService().revokeCode(channel, VerifyCodeService.Purpose.FORGOT_PASSWORD, identifier);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(ctx.getMessage(
                    channel == VerifyCodeService.Channel.SMS ? "sms.failed" : "email.failed", language)));
            return;
        }

        JSONObject response = ApiResponseFactory.success(ctx.getMessage(
                channel == VerifyCodeService.Channel.SMS ? "sms.sent" : "email.sent", language));
        response.put("remainingSeconds", issue.remainingSeconds());
        response.put("accountCount", users.size());
        WebResponseHelper.sendJson(exchange, response);
    }

    private void resetPassword(HttpExchange exchange, JSONObject req) throws IOException {
        String language = req.optString("language", "en");
        ConfigManager.VerifyIdentifier identifierType = resolveIdentifier(req);
        String identifier = normalize(exchange, req, identifierType, language);
        if (identifier == null) {
            return;
        }

        String newPassword = req.optString("newPassword", "");
        if (!newPassword.matches(ctx.getConfigManager().getAuthmePasswordRegex())) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("admin.invalid_password", language)
                            .replace("{regex}", ctx.getConfigManager().getAuthmePasswordRegex())), 400);
            return;
        }

        List<Map<String, Object>> users = AuthFlowSupport.findUsers(ctx.getUserDao(), identifierType, identifier);
        if (users.isEmpty()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(ctx.getMessage("login.user_not_found", language)));
            return;
        }

        String selectedUsername = req.optString("selectedUsername", "").trim();
        if (users.size() > 1 && selectedUsername.isBlank()) {
            JSONObject response = ApiResponseFactory.failure(ctx.getMessage("login.account_selection_required", language));
            response.put("code", "ACCOUNT_SELECTION_REQUIRED");
            response.put("accounts", AuthFlowSupport.accountSummaries(users));
            WebResponseHelper.sendJson(exchange, response, 409);
            return;
        }

        Map<String, Object> user = selectUser(users, selectedUsername);
        if (user == null) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(ctx.getMessage("login.user_not_found", language)));
            return;
        }

        VerifyCodeService.Channel channel = identifierType == ConfigManager.VerifyIdentifier.PHONE
                ? VerifyCodeService.Channel.SMS
                : VerifyCodeService.Channel.EMAIL;
        VerifyCodeService.VerifyResult verify = ctx.getVerifyCodeService()
                .verifyCode(channel, VerifyCodeService.Purpose.FORGOT_PASSWORD, identifier, req.optString("code", ""));
        if (!verify.success()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(ctx.getMessage("verify.wrong_code", language)));
            return;
        }

        String username = String.valueOf(user.get("username"));
        boolean updated = ctx.getUserDao().updateUserPassword(username, newPassword);
        if (updated && ctx.getAuthmeService() != null && ctx.getAuthmeService().isAuthmeEnabled()) {
            ctx.getAuthmeService().syncUserPasswordToAuthme(username, newPassword);
        }
        if (updated && ctx.getAuditDao() != null) {
            ctx.getAuditDao().addAudit(new AuditRecord(
                    "forgot_password_reset", username, username,
                    "User reset password by " + identifierType.configPrefix(), System.currentTimeMillis()));
        }

        WebResponseHelper.sendJson(exchange, updated
                ? ApiResponseFactory.success(ctx.getMessage("forgot_password.reset_success", language))
                : ApiResponseFactory.failure(ctx.getMessage("admin.password_change_failed", language)));
    }

    private ConfigManager.VerifyIdentifier resolveIdentifier(JSONObject req) {
        String input = req.optString("identifier", req.optString("email", req.optString("phone", "")));
        return AuthFlowSupport.parseIdentifier(req.optString("identifierType", input.contains("@") ? "email" : "phone"));
    }

    private String normalize(HttpExchange exchange, JSONObject req, ConfigManager.VerifyIdentifier identifierType, String language) throws IOException {
        String raw = req.optString("identifier", identifierType == ConfigManager.VerifyIdentifier.EMAIL
                ? req.optString("email", "")
                : req.optString("phone", ""));
        if (identifierType == ConfigManager.VerifyIdentifier.PHONE
                && AuthFlowSupport.missingRequiredCountryCode(raw, req.optString("countryCode", ""))) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("sms.country_code_required", language)), 400);
            return null;
        }
        String normalized = AuthFlowSupport.normalizeIdentifier(identifierType, raw, req.optString("countryCode", ""));
        if (identifierType == ConfigManager.VerifyIdentifier.EMAIL && !EmailAddressUtil.isValid(normalized)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("email.invalid_format", language)), 400);
            return null;
        }
        if (identifierType == ConfigManager.VerifyIdentifier.PHONE && !team.kitemc.verifymc.util.PhoneNumberUtil.isValid(normalized)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("sms.invalid_phone", language)), 400);
            return null;
        }
        return normalized;
    }

    private Map<String, Object> selectUser(List<Map<String, Object>> users, String selectedUsername) {
        if (users.size() == 1 && selectedUsername.isBlank()) {
            return users.get(0);
        }
        for (Map<String, Object> user : users) {
            Object username = user.get("username");
            if (username != null && username.toString().equalsIgnoreCase(selectedUsername)) {
                return user;
            }
        }
        return null;
    }
}
