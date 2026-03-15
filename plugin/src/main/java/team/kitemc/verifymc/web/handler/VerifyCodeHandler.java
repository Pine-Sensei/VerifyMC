package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.util.EmailAddressUtil;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

/**
 * Sends an email verification code.
 * Extracted from WebServer.start() — the "/api/verify/send" context.
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

        // Check email domain whitelist
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

        VerifyCodeService.CodeIssueResult issueResult = ctx.getVerifyCodeService().issueCode(email);
        if (!issueResult.issued()) {
            long remainingSeconds = issueResult.remainingSeconds();
            String message = ctx.getMessage("email.rate_limited", language)
                    .replace("{seconds}", String.valueOf(remainingSeconds));
            JSONObject response = ApiResponseFactory.failure(message);
            response.put("remainingSeconds", remainingSeconds);
            WebResponseHelper.sendJson(exchange, response);
            return;
        }

        boolean sent = ctx.getMailService().sendVerificationCode(email, issueResult.code(), language);

        if (sent) {
            JSONObject response = ApiResponseFactory.success(ctx.getMessage("email.sent", language));
            response.put("remainingSeconds", issueResult.remainingSeconds());
            WebResponseHelper.sendJson(exchange, response);
        } else {
            ctx.getVerifyCodeService().revokeCode(email);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("email.failed", language)));
        }
    }
}
