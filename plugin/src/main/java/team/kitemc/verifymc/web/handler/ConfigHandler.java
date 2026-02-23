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
        config.put("theme", ctx.getConfigManager().getTheme());
        config.put("logoUrl", ctx.getConfigManager().getLogoUrl());
        config.put("announcement", ctx.getConfigManager().getAnnouncement());
        config.put("usernameRegex", ctx.getConfigManager().getUsernameRegex());
        config.put("webServerPrefix", ctx.getConfigManager().getWebServerPrefix());
        config.put("wsPort", ctx.getConfigManager().getWsPort());

        JSONObject authmeConfig = new JSONObject();
        authmeConfig.put("enabled", ctx.getConfigManager().isAuthmeEnabled());
        authmeConfig.put("requirePassword", ctx.getConfigManager().isAuthmePasswordRequired());
        authmeConfig.put("passwordRegex", ctx.getConfigManager().getAuthmePasswordRegex());
        config.put("authme", authmeConfig);

        List<String> authMethods = ctx.getConfigManager().getAuthMethods();
        boolean captchaEnabled = authMethods.contains("captcha");
        boolean emailEnabled = authMethods.contains("email");
        JSONObject captchaConfig = new JSONObject();
        captchaConfig.put("enabled", captchaEnabled);
        captchaConfig.put("emailEnabled", emailEnabled);
        captchaConfig.put("type", ctx.getConfigManager().getCaptchaType());
        config.put("captcha", captchaConfig);

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
