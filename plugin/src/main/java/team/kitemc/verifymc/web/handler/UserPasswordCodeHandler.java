package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import team.kitemc.verifymc.service.VerifyCodeService;
import team.kitemc.verifymc.sms.SmsSendResult;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

public class UserPasswordCodeHandler implements HttpHandler {
    private final team.kitemc.verifymc.core.PluginContext ctx;

    public UserPasswordCodeHandler(team.kitemc.verifymc.core.PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "POST")) return;

        String username = AdminAuthUtil.getAuthenticatedUser(exchange, ctx);
        if (username == null) return;

        JSONObject req;
        try {
            req = WebResponseHelper.readJson(exchange);
        } catch (JSONException e) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("error.invalid_json", "en")), 400);
            return;
        }

        String language = req.optString("language", "en");
        String method = req.optString("method", req.optString("authMethod", "email_code")).trim().toLowerCase();
        if (!ctx.getConfigManager().isUserPasswordResetMethodEnabled(method)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(
                    ctx.getMessage("user.password_reset_method_not_enabled", language)), 400);
            return;
        }

        Map<String, Object> user = ctx.getUserDao().getUserByUsername(username);
        if (user == null) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(ctx.getMessage("error.user_not_found", language)));
            return;
        }

        VerifyCodeService.Channel channel;
        String target;
        if ("phone_code".equals(method)) {
            channel = VerifyCodeService.Channel.SMS;
            target = String.valueOf(user.getOrDefault("phone", ""));
            if (target.isBlank()) {
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(ctx.getMessage("sms.required", language)), 400);
                return;
            }
        } else {
            channel = VerifyCodeService.Channel.EMAIL;
            target = String.valueOf(user.getOrDefault("email", ""));
            if (target.isBlank()) {
                WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(ctx.getMessage("email.required", language)), 400);
                return;
            }
        }

        String ip = exchange.getRemoteAddress() != null && exchange.getRemoteAddress().getAddress() != null
                ? exchange.getRemoteAddress().getAddress().getHostAddress()
                : "";
        VerifyCodeService.CodeIssueResult issue = ctx.getVerifyCodeService()
                .issueCode(channel, VerifyCodeService.Purpose.PROFILE_PASSWORD_RESET, target, ip);
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
            SmsSendResult result = ctx.getSmsService().sendVerificationCode(target, issue.code());
            sent = result.success();
        } else {
            sent = ctx.getMailService().sendVerificationCode(target, issue.code(), language);
        }

        if (!sent) {
            ctx.getVerifyCodeService().revokeCode(channel, VerifyCodeService.Purpose.PROFILE_PASSWORD_RESET, target);
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure(ctx.getMessage(
                    channel == VerifyCodeService.Channel.SMS ? "sms.failed" : "email.failed", language)));
            return;
        }

        JSONObject response = ApiResponseFactory.success(ctx.getMessage(
                channel == VerifyCodeService.Channel.SMS ? "sms.sent" : "email.sent", language));
        response.put("remainingSeconds", issue.remainingSeconds());
        WebResponseHelper.sendJson(exchange, response);
    }
}
