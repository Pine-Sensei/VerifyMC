package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;
import java.util.List;

/**
 * Serves front-end configuration: auth methods, theme settings, captcha/questionnaire/discord flags, etc.
 * Extracted from WebServer.start() — the "/api/config" context.
 */
public class ConfigHandler implements HttpHandler {
    private final PluginContext ctx;

    public ConfigHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        JSONObject config = new JSONObject();
        config.put("authMethods", new JSONArray(ctx.getConfigManager().getAuthMethods()));
        JSONObject loginConfig = new JSONObject();
        loginConfig.put("allowedMethods", new JSONArray(ctx.getConfigManager().getAllowedLoginMethods()));
        loginConfig.put("usernameCaseSensitive", ctx.getConfigManager().isUsernameCaseSensitive());
        config.put("login", loginConfig);

        JSONObject forgotPasswordConfig = new JSONObject();
        forgotPasswordConfig.put("enabled", ctx.getConfigManager().isForgotPasswordEnabled());
        forgotPasswordConfig.put("allowedMethods", new JSONArray(ctx.getConfigManager().getForgotPasswordMethods()));
        config.put("forgotPassword", forgotPasswordConfig);

        JSONObject userConfig = new JSONObject();
        userConfig.put("passwordResetMethods", new JSONArray(ctx.getConfigManager().getUserPasswordResetMethods()));
        config.put("user", userConfig);

        JSONObject authConfig = new JSONObject();
        authConfig.put("mustAuthMethods", new JSONArray(ctx.getConfigManager().getMustAuthMethods()));
        authConfig.put("optionAuthMethods", new JSONArray(ctx.getConfigManager().getOptionAuthMethods()));
        authConfig.put("minOptionAuthMethods", ctx.getConfigManager().getMinOptionAuthMethods());
        config.put("auth", authConfig);
        config.put("theme", ctx.getConfigManager().getTheme());
        config.put("logoUrl", ctx.getConfigManager().getLogoUrl());
        config.put("announcement", ctx.getConfigManager().getAnnouncement());
        config.put("usernameRegex", ctx.getConfigManager().getUsernameRegex());
        config.put("webServerPrefix", ctx.getConfigManager().getWebServerPrefix());
        config.put("wsPort", ctx.getConfigManager().getWsPort());

        JSONObject authmeConfig = new JSONObject();
        authmeConfig.put("enabled", ctx.getConfigManager().isAuthmeEnabled());
        authmeConfig.put("passwordRegex", ctx.getConfigManager().getAuthmePasswordRegex());
        config.put("authme", authmeConfig);

        JSONObject captchaConfig = new JSONObject();
        captchaConfig.put("enabled", ctx.getConfigManager().isCaptchaAuthEnabled());
        captchaConfig.put("emailEnabled", ctx.getConfigManager().isEmailAuthEnabled());
        captchaConfig.put("type", ctx.getConfigManager().getCaptchaType());
        config.put("captcha", captchaConfig);

        JSONObject smsConfig = new JSONObject();
        smsConfig.put("enabled", ctx.getConfigManager().isSmsAuthEnabled());
        smsConfig.put("provider", ctx.getConfigManager().getSmsProvider());
        smsConfig.put("codeLength", ctx.getConfigManager().getSmsCodeLength());
        smsConfig.put("cooldownSeconds", ctx.getConfigManager().getSmsSendCooldownSeconds());
        config.put("sms", smsConfig);

        JSONObject questionnaireConfig = new JSONObject();
        questionnaireConfig.put("enabled", ctx.getQuestionnaireService() != null && ctx.getQuestionnaireService().isEnabled());
        questionnaireConfig.put("passScore", ctx.getQuestionnaireService() != null ? ctx.getQuestionnaireService().getPassScore() : 60);
        questionnaireConfig.put("hasTextQuestions", ctx.getQuestionnaireService() != null && ctx.getQuestionnaireService().hasTextQuestions());
        config.put("questionnaire", questionnaireConfig);

        JSONObject discordConfig = new JSONObject();
        discordConfig.put("enabled", ctx.getDiscordService() != null && ctx.getDiscordService().isEnabled());
        discordConfig.put("required", ctx.getDiscordService() != null && ctx.getDiscordService().isRequired());
        config.put("discord", discordConfig);

        JSONObject bedrockConfig = new JSONObject();
        bedrockConfig.put("enabled", ctx.getConfigManager().isBedrockEnabled());
        bedrockConfig.put("prefix", ctx.getConfigManager().getBedrockPrefix());
        bedrockConfig.put("usernameRegex", ctx.getConfigManager().getBedrockUsernameRegex());
        config.put("bedrock", bedrockConfig);

        if (ctx.getConfigManager().isEmailDomainWhitelistEnabled()) {
            config.put("emailDomainWhitelist", new JSONArray(ctx.getConfigManager().getEmailDomainWhitelist()));
        }
        config.put("enableEmailDomainWhitelist", ctx.getConfigManager().isEmailDomainWhitelistEnabled());
        config.put("enableEmailAliasLimit", ctx.getConfigManager().isEmailAliasLimitEnabled());
        config.put("language", ctx.getConfigManager().getLanguage());

        JSONObject resp = new JSONObject();
        resp.put("success", true);
        resp.put("config", config);
        WebResponseHelper.sendJson(exchange, resp);
    }
}
