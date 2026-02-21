package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

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

        JSONObject req = WebResponseHelper.readJson(exchange);
        String email = req.optString("email", "").trim().toLowerCase();
        String language = req.optString("language", "en");

        if (email.isEmpty() || !email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("verify.invalid_email", language)));
            return;
        }

        // Check email domain whitelist
        if (ctx.getConfigManager().isEmailDomainWhitelistEnabled()) {
            String domain = email.contains("@") ? email.substring(email.indexOf('@') + 1) : "";
            if (!ctx.getConfigManager().getEmailDomainWhitelist().contains(domain)) {
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                        ctx.getMessage("register.domain_not_allowed", language)));
                return;
            }
        }

        // Check rate limit
        if (!ctx.getVerifyCodeService().canSendCode(email)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("verify.rate_limit", language)));
            return;
        }

        // Generate and send code
        String code = ctx.getVerifyCodeService().generateCode(email);
        boolean sent = ctx.getMailService().sendVerifyCode(email, code, language);

        if (sent) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.success(
                    ctx.getMessage("verify.sent", language)));
        } else {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("verify.send_failed", language)));
        }
    }
}
