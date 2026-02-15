package team.kitemc.verifymc.infrastructure.web.controller;

import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.service.DiscordIntegrationService;
import team.kitemc.verifymc.domain.service.QuestionnaireEvaluationService;
import team.kitemc.verifymc.domain.service.UserService;
import team.kitemc.verifymc.infrastructure.annotation.Service;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;
import team.kitemc.verifymc.infrastructure.web.ApiResponse;
import team.kitemc.verifymc.infrastructure.web.RequestContext;
import team.kitemc.verifymc.infrastructure.web.RouteHandler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ConfigController implements RouteHandler {

    private final Plugin plugin;
    private final ConfigurationService configService;
    private final UserService userService;
    private final QuestionnaireEvaluationService questionnaireEvaluationService;
    private final DiscordIntegrationService discordIntegrationService;
    private final boolean debug;

    public ConfigController(Plugin plugin,
                            ConfigurationService configService,
                            UserService userService,
                            QuestionnaireEvaluationService questionnaireEvaluationService,
                            DiscordIntegrationService discordIntegrationService) {
        this.plugin = plugin;
        this.configService = configService;
        this.userService = userService;
        this.questionnaireEvaluationService = questionnaireEvaluationService;
        this.discordIntegrationService = discordIntegrationService;
        this.debug = configService.getBoolean("debug", false);
    }

    @Override
    public void handle(RequestContext ctx) throws Exception {
        String path = ctx.getPath();

        if ("/api/config".equals(path)) {
            handleGetConfig(ctx);
        } else if ("/api/ping".equals(path)) {
            handlePing(ctx);
        } else if ("/api/check-whitelist".equals(path)) {
            handleCheckWhitelist(ctx);
        } else {
            ctx.sendNotFound("Endpoint not found");
        }
    }

    private void handleGetConfig(RequestContext ctx) throws IOException {
        JSONObject resp = new JSONObject();

        JSONObject login = new JSONObject();
        List<String> authMethods = configService.getStringList("auth_methods");
        login.put("enable_email", authMethods.contains("email"));
        login.put("email_smtp", configService.getString("smtp.host", ""));

        JSONObject admin = new JSONObject();

        JSONObject frontend = new JSONObject();
        frontend.put("theme", configService.getString("frontend.theme", "default"));
        frontend.put("logo_url", configService.getString("frontend.logo_url", ""));
        frontend.put("announcement", configService.getString("frontend.announcement", ""));
        frontend.put("web_server_prefix", configService.getString("web_server_prefix", "[VerifyMC]"));
        frontend.put("current_theme", configService.getString("frontend.theme", "default"));
        frontend.put("username_regex", configService.getString("username_regex", "^[a-zA-Z0-9_-]{3,16}$"));

        JSONObject authme = new JSONObject();
        authme.put("enabled", configService.getBoolean("authme.enabled", false));
        authme.put("require_password", configService.getBoolean("authme.require_password", false));
        authme.put("password_regex", configService.getString("authme.password_regex", "^[a-zA-Z0-9_]{8,26}$"));

        JSONObject captcha = new JSONObject();
        captcha.put("enabled", authMethods.contains("captcha"));
        captcha.put("email_enabled", authMethods.contains("email"));
        captcha.put("type", configService.getString("captcha.type", "math"));

        JSONObject bedrock = new JSONObject();
        bedrock.put("enabled", configService.getBoolean("bedrock.enabled", false));
        bedrock.put("prefix", configService.getString("bedrock.prefix", "."));
        bedrock.put("username_regex", configService.getString("bedrock.username_regex", "^\\.[a-zA-Z0-9_\\s]{3,16}$"));

        JSONObject questionnaire = new JSONObject();
        questionnaire.put("enabled", questionnaireEvaluationService.isEnabled());
        questionnaire.put("pass_score", questionnaireEvaluationService.getPassScore());
        questionnaire.put("has_text_questions", questionnaireEvaluationService.hasTextQuestions());

        JSONObject discord = new JSONObject();
        discord.put("enabled", discordIntegrationService.isEnabled());
        discord.put("required", discordIntegrationService.isRequired());

        resp.put("login", login);
        resp.put("admin", admin);
        resp.put("frontend", frontend);
        resp.put("authme", authme);
        resp.put("captcha", captcha);
        resp.put("bedrock", bedrock);
        resp.put("questionnaire", questionnaire);
        resp.put("discord", discord);

        ctx.sendJson(resp);
    }

    private void handlePing(RequestContext ctx) throws IOException {
        JSONObject resp = new JSONObject();
        resp.put("msg", "pong");
        ctx.sendJson(resp);
    }

    private void handleCheckWhitelist(RequestContext ctx) throws IOException {
        if (!"GET".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        String username = ctx.getQueryParam("username", "");

        JSONObject resp = new JSONObject();

        if (username == null || username.trim().isEmpty()) {
            resp.put("success", false);
            resp.put("msg", "Username parameter is required");
            ctx.sendJson(resp);
            return;
        }

        try {
            Optional<User> userOpt = userService.getUserByUsername(username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                resp.put("success", true);
                resp.put("found", true);
                resp.put("username", user.getUsername());
                resp.put("status", user.getStatus().name().toLowerCase());
                resp.put("email", user.getEmail());
                debugLog("Whitelist check for " + username + ": found, status=" + user.getStatus());
            } else {
                resp.put("success", true);
                resp.put("found", false);
                resp.put("status", "not_registered");
                debugLog("Whitelist check for " + username + ": not found");
            }
        } catch (Exception e) {
            debugLog("Whitelist check error: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", "Failed to check whitelist status");
        }

        ctx.sendJson(resp);
    }

    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] ConfigController: " + msg);
        }
    }
}
