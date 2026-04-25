package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.sms.SmsSendResult;
import team.kitemc.verifymc.util.EmailAddressUtil;
import team.kitemc.verifymc.util.PhoneNumberUtil;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

/**
 * Sends email or SMS verification codes for registration.
 */
public class VerifyCodeHandler implements HttpHandler {
    private final PluginContext ctx;

    public VerifyCodeHandler(PluginContext ctx) {
        this.ctx = ctx;
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

        String channel = req.optString("channel", req.has("phone") ? "sms" : "email").trim().toLowerCase();
        if ("sms".equals(channel)) {
            handleSms(exchange, req);
            return;
        }
        handleEmail(exchange, req);
    }

    private void handleEmail(HttpExchange exchange, JSONObject req) throws IOException {
        String email = EmailAddressUtil.normalize(req.optString("email", ""));
        String language = req.optString("language", "en");

        if (!ctx.getConfigManager().isEmailAuthEnabled()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("email.not_enabled", language)));
            return;
        }

        if (!EmailAddressUtil.isValid(email)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("email.invalid_format", language)));
            return;
        }

        if (ctx.getConfigManager().isEmailAliasLimitEnabled() && EmailAddressUtil.hasAlias(email)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("register.alias_not_allowed", language)));
            return;
        }

        if (ctx.getConfigManager().isEmailDomainWhitelistEnabled()) {
            String domain = EmailAddressUtil.extractDomain(email);
            if (!ctx.getConfigManager().getEmailDomainWhitelist().contains(domain)) {
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                        ctx.getMessage("register.domain_not_allowed", language)));
                return;
            }
        }

        if (ctx.getUserDao().countUsersByEmail(email) >= ctx.getConfigManager().getMaxAccountsPerEmail()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("register.email_limit", language)));
            return;
        }

        VerifyCodeService.CodeIssueResult issueResult = ctx.getVerifyCodeService().issueCode(VerifyCodeService.Channel.EMAIL, email, null);
        if (!issueResult.issued()) {
            sendRateLimited(exchange, "email.rate_limited", language, issueResult.remainingSeconds());
            return;
        }

        boolean sent = ctx.getMailService().sendVerificationCode(email, issueResult.code(), language);
        if (sent) {
            sendIssued(exchange, ctx.getMessage("email.sent", language), issueResult.remainingSeconds());
        } else {
            ctx.getVerifyCodeService().revokeCode(VerifyCodeService.Channel.EMAIL, email);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("email.failed", language)));
        }
    }

    private void handleSms(HttpExchange exchange, JSONObject req) throws IOException {
        String phone = PhoneNumberUtil.normalize(req.optString("phone", ""));
        String language = req.optString("language", "en");
        String ip = exchange.getRemoteAddress() != null && exchange.getRemoteAddress().getAddress() != null
                ? exchange.getRemoteAddress().getAddress().getHostAddress()
                : "";

        if (!ctx.getConfigManager().isSmsAuthEnabled()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("sms.not_enabled", language)));
            return;
        }

        if (!PhoneNumberUtil.isValid(phone)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("sms.invalid_phone", language)));
            return;
        }

        if (ctx.getUserDao().countUsersByPhone(phone) >= ctx.getConfigManager().getMaxAccountsPerPhone()) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("register.phone_limit", language)));
            return;
        }

        VerifyCodeService.CodeIssueResult issueResult = ctx.getVerifyCodeService().issueCode(VerifyCodeService.Channel.SMS, phone, ip);
        if (!issueResult.issued()) {
            sendRateLimited(exchange, "sms.rate_limited", language, issueResult.remainingSeconds());
            return;
        }

        SmsSendResult sent = ctx.getSmsService().sendVerificationCode(phone, issueResult.code());
        if (sent.success()) {
            sendIssued(exchange, ctx.getMessage("sms.sent", language), issueResult.remainingSeconds());
        } else {
            ctx.getVerifyCodeService().revokeCode(VerifyCodeService.Channel.SMS, phone);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("sms.failed", language)));
        }
    }

    private void sendRateLimited(HttpExchange exchange, String key, String language, long remainingSeconds) throws IOException {
        String message = ctx.getMessage(key, language)
                .replace("{seconds}", String.valueOf(remainingSeconds));
        JSONObject response = ApiResponseFactory.failure(message);
        response.put("remainingSeconds", remainingSeconds);
        WebResponseHelper.sendJson(exchange, response);
    }

    private void sendIssued(HttpExchange exchange, String message, long remainingSeconds) throws IOException {
        JSONObject response = ApiResponseFactory.success(message);
        response.put("remainingSeconds", remainingSeconds);
        WebResponseHelper.sendJson(exchange, response);
    }
}
