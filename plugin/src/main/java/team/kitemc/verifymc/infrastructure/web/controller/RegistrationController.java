package team.kitemc.verifymc.infrastructure.web.controller;

import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import team.kitemc.verifymc.domain.service.MailIntegrationService;
import team.kitemc.verifymc.domain.service.RegistrationService;
import team.kitemc.verifymc.domain.service.VerificationCodeService;
import team.kitemc.verifymc.infrastructure.annotation.Service;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;
import team.kitemc.verifymc.infrastructure.web.ApiResponse;
import team.kitemc.verifymc.infrastructure.web.RequestContext;
import team.kitemc.verifymc.infrastructure.web.RouteHandler;
import team.kitemc.verifymc.service.CaptchaService;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class RegistrationController implements RouteHandler {

    private final Plugin plugin;
    private final RegistrationService registrationService;
    private final VerificationCodeService verificationCodeService;
    private final MailIntegrationService mailIntegrationService;
    private final CaptchaService captchaService;
    private final ConfigurationService configService;
    private final boolean debug;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final List<String> DEFAULT_EMAIL_DOMAIN_WHITELIST = List.of(
        "gmail.com", "qq.com", "163.com", "126.com", "outlook.com", "hotmail.com", "yahoo.com",
        "sina.com", "aliyun.com", "foxmail.com", "icloud.com", "yeah.net", "live.com", "mail.com",
        "protonmail.com", "zoho.com"
    );

    public RegistrationController(Plugin plugin,
                                   RegistrationService registrationService,
                                   VerificationCodeService verificationCodeService,
                                   MailIntegrationService mailIntegrationService,
                                   CaptchaService captchaService,
                                   ConfigurationService configService) {
        this.plugin = plugin;
        this.registrationService = registrationService;
        this.verificationCodeService = verificationCodeService;
        this.mailIntegrationService = mailIntegrationService;
        this.captchaService = captchaService;
        this.configService = configService;
        this.debug = configService.getBoolean("debug", false);
    }

    @Override
    public void handle(RequestContext ctx) throws Exception {
        String path = ctx.getPath();
        
        if ("/api/send_code".equals(path)) {
            handleSendCode(ctx);
        } else if ("/api/register".equals(path)) {
            handleRegister(ctx);
        } else if ("/api/captcha".equals(path)) {
            handleCaptcha(ctx);
        } else {
            ctx.sendNotFound("Endpoint not found");
        }
    }

    private void handleSendCode(RequestContext ctx) throws IOException {
        if (!"POST".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        JSONObject body = ctx.getBody();
        String email = body.optString("email", "").trim().toLowerCase();
        String language = body.optString("language", "en");

        if (!isValidEmail(email)) {
            ctx.sendJson(createErrorResponse("email.invalid_format", language));
            return;
        }

        if (!verificationCodeService.canSendCode(email)) {
            long remainingSeconds = verificationCodeService.getRemainingCooldownSeconds(email);
            JSONObject resp = createErrorResponse("email.rate_limited", language);
            resp.put("remaining_seconds", remainingSeconds);
            ctx.sendJson(resp);
            return;
        }

        if (isEmailAliasLimitEnabled() && email.contains("+")) {
            ctx.sendJson(createErrorResponse("register.alias_not_allowed", language));
            return;
        }

        if (isEmailDomainWhitelistEnabled()) {
            String domain = email.contains("@") ? email.substring(email.indexOf('@') + 1) : "";
            List<String> allowedDomains = getEmailDomainWhitelist();
            if (!allowedDomains.contains(domain)) {
                ctx.sendJson(createErrorResponse("register.domain_not_allowed", language));
                return;
            }
        }

        String code = verificationCodeService.generateCode(email);
        debugLog("Generated verification code for email: " + maskEmail(email));

        String emailSubject = configService.getString("email_subject", "VerifyMC Verification Code");
        boolean sent = mailIntegrationService.sendCode(email, code, language);

        JSONObject resp = new JSONObject();
        resp.put("success", sent);
        resp.put("msg", sent ? "Verification code sent" : "Failed to send verification code");
        ctx.sendJson(resp);
    }

    private void handleRegister(RequestContext ctx) throws IOException {
        if (!"POST".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        JSONObject body = ctx.getBody();
        String language = body.optString("language", "en");

        try {
            String uuid = body.optString("uuid", "").trim().toLowerCase();
            String username = body.optString("username", "").trim();
            String email = body.optString("email", "").trim().toLowerCase();
            String code = body.optString("code", "").trim();
            String password = body.optString("password", "").trim();
            String platform = body.optString("platform", "java");
            String captchaToken = body.optString("captcha_token", "");
            String captchaAnswer = body.optString("captcha_answer", "");

            if (!isValidUUID(uuid)) {
                ctx.sendJson(createErrorResponse("register.invalid_uuid", language));
                return;
            }

            if (!isValidEmail(email)) {
                ctx.sendJson(createErrorResponse("email.invalid_format", language));
                return;
            }

            if (!verificationCodeService.checkCode(email, code)) {
                ctx.sendJson(createErrorResponse("register.invalid_code", language));
                return;
            }

            if (captchaService.isCaptchaEnabled()) {
                if (!captchaService.validateCaptcha(captchaToken, captchaAnswer)) {
                    ctx.sendJson(createErrorResponse("register.invalid_captcha", language));
                    return;
                }
            }

            team.kitemc.verifymc.domain.service.RegistrationRequest request = 
                new team.kitemc.verifymc.domain.service.RegistrationRequest.Builder()
                    .uuid(uuid)
                    .username(username)
                    .email(email)
                    .password(password)
                    .platform(platform)
                    .build();

            team.kitemc.verifymc.domain.service.RegistrationResult result = registrationService.register(request);

            JSONObject resp = new JSONObject();
            resp.put("success", result.isSuccess());
            resp.put("msg", result.getMessage());
            if (result.getUser() != null) {
                resp.put("status", result.getStatus().name().toLowerCase());
            }
            ctx.sendJson(resp);

        } catch (Exception e) {
            debugLog("Registration error: " + e.getMessage());
            ctx.sendJson(createErrorResponse("register.failed", language));
        }
    }

    private void handleCaptcha(RequestContext ctx) throws IOException {
        if (!"GET".equals(ctx.getMethod())) {
            ctx.sendMethodNotAllowed();
            return;
        }

        JSONObject resp = new JSONObject();
        try {
            CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
            resp.put("success", true);
            resp.put("token", result.getToken());
            resp.put("image", result.getImageBase64());
        } catch (Exception e) {
            debugLog("Captcha generation failed: " + e.getMessage());
            resp.put("success", false);
            resp.put("msg", "Failed to generate captcha");
        }
        ctx.sendJson(resp);
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isEmailDomainWhitelistEnabled() {
        return configService.getBoolean("enable_email_domain_whitelist", true);
    }

    private boolean isEmailAliasLimitEnabled() {
        return configService.getBoolean("enable_email_alias_limit", false);
    }

    private List<String> getEmailDomainWhitelist() {
        List<String> list = configService.getStringList("email_domain_whitelist");
        if (list == null || list.isEmpty()) {
            return DEFAULT_EMAIL_DOMAIN_WHITELIST;
        }
        return list;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return "**@" + domain;
        }
        return local.substring(0, 1) + "***" + local.substring(local.length() - 1) + "@" + domain;
    }

    private JSONObject createErrorResponse(String key, String language) {
        JSONObject resp = new JSONObject();
        resp.put("success", false);
        resp.put("msg", key);
        return resp;
    }

    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] RegistrationController: " + msg);
        }
    }
}
