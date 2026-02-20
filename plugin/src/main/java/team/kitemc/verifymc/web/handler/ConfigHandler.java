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
        config.put("auth_methods", new JSONArray(ctx.getConfigManager().getAuthMethods()));
        config.put("theme", ctx.getConfigManager().getTheme());
        config.put("logo_url", ctx.getConfigManager().getLogoUrl());
        config.put("announcement", ctx.getConfigManager().getAnnouncement());
        config.put("username_regex", ctx.getConfigManager().getUsernameRegex());
        config.put("web_server_prefix", ctx.getConfigManager().getWebServerPrefix());

        JSONObject authmeConfig = new JSONObject();
        authmeConfig.put("enabled", ctx.getConfigManager().isAuthmeEnabled());
        authmeConfig.put("require_password", ctx.getConfigManager().isAuthmePasswordRequired());
        authmeConfig.put("password_regex", ctx.getConfigManager().getAuthmePasswordRegex());
        config.put("authme", authmeConfig);

        List<String> authMethods = ctx.getConfigManager().getAuthMethods();
        boolean captchaEnabled = authMethods.contains("captcha");
        boolean emailEnabled = authMethods.contains("email");
        JSONObject captchaConfig = new JSONObject();
        captchaConfig.put("enabled", captchaEnabled);
        captchaConfig.put("email_enabled", emailEnabled);
        captchaConfig.put("type", ctx.getConfigManager().getCaptchaType());
        config.put("captcha", captchaConfig);

        JSONObject questionnaireConfig = new JSONObject();
        questionnaireConfig.put("enabled", ctx.getQuestionnaireService() != null && ctx.getQuestionnaireService().isEnabled());
        questionnaireConfig.put("pass_score", ctx.getQuestionnaireService() != null ? ctx.getQuestionnaireService().getPassScore() : 60);
        questionnaireConfig.put("has_text_questions", ctx.getQuestionnaireService() != null && ctx.getQuestionnaireService().hasTextQuestions());
        config.put("questionnaire", questionnaireConfig);

        JSONObject discordConfig = new JSONObject();
        discordConfig.put("enabled", ctx.getDiscordService() != null && ctx.getDiscordService().isEnabled());
        discordConfig.put("required", ctx.getDiscordService() != null && ctx.getDiscordService().isRequired());
        config.put("discord", discordConfig);

        JSONObject bedrockConfig = new JSONObject();
        bedrockConfig.put("enabled", ctx.getConfigManager().isBedrockEnabled());
        bedrockConfig.put("prefix", ctx.getConfigManager().getBedrockPrefix());
        bedrockConfig.put("username_regex", ctx.getConfigManager().getBedrockUsernameRegex());
        config.put("bedrock", bedrockConfig);

        if (ctx.getConfigManager().isEmailDomainWhitelistEnabled()) {
            config.put("email_domain_whitelist", new JSONArray(ctx.getConfigManager().getEmailDomainWhitelist()));
        }
        config.put("enable_email_domain_whitelist", ctx.getConfigManager().isEmailDomainWhitelistEnabled());
        config.put("enable_email_alias_limit", ctx.getConfigManager().isEmailAliasLimitEnabled());
        config.put("language", ctx.getConfigManager().getLanguage());

        JSONObject resp = new JSONObject();
        resp.put("success", true);
        resp.put("config", config);
        WebResponseHelper.sendJson(exchange, resp);
    }
}
