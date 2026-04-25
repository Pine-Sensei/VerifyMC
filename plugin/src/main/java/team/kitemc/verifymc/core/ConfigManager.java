package team.kitemc.verifymc.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import team.kitemc.verifymc.security.AdminAuthMode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Type-safe configuration access, eliminating scattered getConfig().getString(...)
 * calls with magic strings throughout the codebase.
 */
public class ConfigManager {
    private final JavaPlugin plugin;

    private static final List<String> DEFAULT_EMAIL_DOMAIN_WHITELIST = Arrays.asList(
        "gmail.com", "qq.com", "163.com", "126.com", "outlook.com", "hotmail.com", "yahoo.com",
        "sina.com", "aliyun.com", "foxmail.com", "icloud.com", "yeah.net", "live.com", "mail.com",
        "protonmail.com", "zoho.com"
    );

    private static final Set<String> VALID_STORAGE_TYPES = new HashSet<>(Arrays.asList("file", "mysql"));
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        validateConfig();
    }

    /**
     * Validates configuration values and logs warnings for invalid settings.
     */
    private void validateConfig() {
        String adminAuthMode = getConfig().getString("admin_auth.mode", "op");
        String effectiveAuthMode;
        if (!"op".equalsIgnoreCase(adminAuthMode) && !"permission".equalsIgnoreCase(adminAuthMode)) {
            plugin.getLogger().log(Level.WARNING,
                "Invalid admin_auth.mode: {0}. Must be one of: op, permission. Using default ''op''.",
                adminAuthMode);
            effectiveAuthMode = "op";
        } else {
            effectiveAuthMode = adminAuthMode.toLowerCase();
        }
        plugin.getLogger().log(Level.INFO, "Admin auth mode: {0}", effectiveAuthMode);

        // Validate web port
        int webPort = getWebPort();
        if (webPort < MIN_PORT || webPort > MAX_PORT) {
            plugin.getLogger().log(Level.WARNING,
                "Invalid web_port: {0}. Must be between {1} and {2}. Using default 8080.",
                new Object[]{webPort, MIN_PORT, MAX_PORT});
        }

        // Validate WebSocket port
        int wsPort = getWsPort();
        if (wsPort < MIN_PORT || wsPort > MAX_PORT) {
            plugin.getLogger().log(Level.WARNING,
                "Invalid ws_port: {0}. Must be between {1} and {2}. Using default 8081.",
                new Object[]{wsPort, MIN_PORT, MAX_PORT});
        }

        if (isSslEnabled()) {
            validateSslKeystorePath();

            if (getSslKeystoreType().isEmpty()) {
                plugin.getLogger().warning("SSL is enabled but ssl.keystore.type is empty. Using default PKCS12.");
            }

            if (getSslKeystorePassword().isEmpty()) {
                plugin.getLogger().warning("SSL is enabled and ssl.keystore.password is empty. Make sure the keystore intentionally uses an empty password.");
            }
        }

        // Validate storage type
        String storageType = getStorageType();
        if (!VALID_STORAGE_TYPES.contains(storageType.toLowerCase())) {
            plugin.getLogger().log(Level.WARNING,
                "Invalid storage type: {0}. Must be one of: {1}. Using default ''file''.",
                new Object[]{storageType, String.join(", ", VALID_STORAGE_TYPES)});
        }

        // Validate MySQL port if using MySQL storage
        if ("mysql".equalsIgnoreCase(storageType)) {
            int mysqlPort = getConfig().getInt("mysql.port", 3306);
            if (mysqlPort < MIN_PORT || mysqlPort > MAX_PORT) {
                plugin.getLogger().log(Level.WARNING,
                    "Invalid mysql.port: {0}. Must be between {1} and {2}. Using default 3306.",
                    new Object[]{mysqlPort, MIN_PORT, MAX_PORT});
            }
        }

        plugin.getLogger().log(Level.INFO, "Configuration validated successfully");
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
    }

    // --- General config ---
    public boolean isDebug() {
        return getConfig().getBoolean("debug", false);
    }

    public AdminAuthMode getAdminAuthMode() {
        return AdminAuthMode.fromConfig(getConfig().getString("admin_auth.mode", "op"));
    }

    public boolean isAdminAuthByPermission() {
        return getAdminAuthMode() == AdminAuthMode.PERMISSION;
    }

    public String getStorageType() {
        return getConfig().getString("storage", "file");
    }

    public String getLanguage() {
        return getConfig().getString("language", "en");
    }

    // --- Web server ---
    public int getWebPort() {
        int port = getConfig().getInt("web_port", 8080);
        return (port >= MIN_PORT && port <= MAX_PORT) ? port : 8080;
    }

    public int getWsPort() {
        int port = getConfig().getInt("ws_port", 8081);
        return (port >= MIN_PORT && port <= MAX_PORT) ? port : 8081;
    }

    public String getWebServerPrefix() {
        return getConfig().getString("web_server_prefix", "[VerifyMC]");
    }

    public boolean isSslEnabled() {
        return getConfig().getBoolean("ssl.enabled", false);
    }

    public String getSslKeystorePath() {
        return getConfig().getString("ssl.keystore.path", "").trim();
    }

    public String getSslKeystorePassword() {
        return getConfig().getString("ssl.keystore.password", "");
    }

    public String getSslKeystoreType() {
        return getConfig().getString("ssl.keystore.type", "PKCS12").trim();
    }

    public Path resolveSslKeystorePath() throws IOException {
        return team.kitemc.verifymc.web.ServerSslContextFactory.resolveKeystorePath(
                plugin.getDataFolder().toPath(),
                getSslKeystorePath());
    }

    private void validateSslKeystorePath() {
        try {
            resolveSslKeystorePath();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "SSL is enabled but {0}", e.getMessage());
        }
    }

    // --- Frontend ---
    public String getTheme() {
        return getConfig().getString("frontend.theme", "glassx");
    }

    public String getLogoUrl() {
        return getConfig().getString("frontend.logo_url", "");
    }

    public String getAnnouncement() {
        return getConfig().getString("frontend.announcement", "");
    }

    public boolean isServeStaticEnabled() {
        return getConfig().getBoolean("frontend.serve_static", true);
    }

    public List<String> getAllowedOrigins() {
        List<String> origins = getConfig().getStringList("frontend.allowed_origins");
        if (origins == null || origins.isEmpty()) {
            return Collections.emptyList();
        }
        return origins.stream()
            .map(origin -> origin == null ? "" : origin.trim())
            .filter(origin -> !origin.isEmpty())
            .toList();
    }

    public String getUsernameRegex() {
        return getConfig().getString("username_regex", "^[a-zA-Z0-9_-]{3,16}$");
    }

    public boolean isUsernameCaseSensitive() {
        return getConfig().getBoolean("username_case_sensitive", false);
    }

    // --- Auth methods ---
    public List<String> getAuthMethods() {
        List<String> legacy = getConfig().getStringList("auth_methods");
        if (legacy == null || legacy.isEmpty()) {
            return getMustAuthMethods();
        }
        return legacy;
    }

    public List<String> getMustAuthMethods() {
        List<String> methods = getConfig().getStringList("auth.must_auth_methods");
        if (methods == null || methods.isEmpty()) {
            methods = getConfig().getStringList("auth_methods");
        }
        return normalizeAuthMethods(methods);
    }

    public List<String> getOptionAuthMethods() {
        return normalizeAuthMethods(getConfig().getStringList("auth.option_auth_methods"));
    }

    public int getMinOptionAuthMethods() {
        int configured = getConfig().getInt("auth.min_option_auth_methods", 0);
        return Math.max(0, Math.min(configured, getOptionAuthMethods().size()));
    }

    private List<String> normalizeAuthMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return Collections.emptyList();
        }
        return methods.stream()
            .filter(method -> method != null && !method.trim().isEmpty())
            .map(method -> method.trim().toLowerCase())
            .distinct()
            .toList();
    }

    public boolean isEmailAuthEnabled() {
        return getMustAuthMethods().contains("email") || getOptionAuthMethods().contains("email");
    }

    public boolean isCaptchaAuthEnabled() {
        return getMustAuthMethods().contains("captcha") || getOptionAuthMethods().contains("captcha");
    }

    public boolean isSmsAuthEnabled() {
        return getMustAuthMethods().contains("sms") || getOptionAuthMethods().contains("sms");
    }

    // --- Login methods ---
    public List<String> getAllowedLoginMethods() {
        List<String> configured = normalizeLoginMethods(getConfig().getStringList("login.allowed_methods"));
        if (!configured.isEmpty()) {
            return configured;
        }

        List<String> legacy = normalizeAuthMethods(getConfig().getStringList("auth_methods"));
        if (legacy.isEmpty()) {
            return List.of("username");
        }

        java.util.LinkedHashSet<String> mapped = new java.util.LinkedHashSet<>();
        mapped.add("username");
        for (String method : legacy) {
            switch (method) {
                case "username" -> mapped.add("username");
                case "email" -> {
                    mapped.add("email_password");
                    mapped.add("email_code");
                }
                case "phone", "sms" -> {
                    mapped.add("phone_password");
                    mapped.add("phone_code");
                }
                default -> {
                    if (isValidLoginMethod(method)) {
                        mapped.add(method);
                    }
                }
            }
        }
        if (mapped.isEmpty()) {
            mapped.add("username");
        }
        return List.copyOf(mapped);
    }

    public boolean isLoginMethodEnabled(String method) {
        if (method == null || method.isBlank()) {
            return false;
        }
        return getAllowedLoginMethods().contains(method.trim().toLowerCase());
    }

    public boolean isLoginCodeEnabled(VerifyIdentifier identifier) {
        return isLoginMethodEnabled(identifier.configPrefix() + "_code");
    }

    public boolean isLoginPasswordEnabled(VerifyIdentifier identifier) {
        if (identifier == VerifyIdentifier.USERNAME) {
            return isLoginMethodEnabled("username");
        }
        return isLoginMethodEnabled(identifier.configPrefix() + "_password");
    }

    private List<String> normalizeLoginMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return Collections.emptyList();
        }
        return methods.stream()
                .filter(method -> method != null && !method.trim().isEmpty())
                .map(method -> method.trim().toLowerCase())
                .flatMap(method -> switch (method) {
                    case "email" -> java.util.stream.Stream.of("email_password", "email_code");
                    case "phone" -> java.util.stream.Stream.of("phone_password", "phone_code");
                    default -> java.util.stream.Stream.of(method);
                })
                .filter(this::isValidLoginMethod)
                .distinct()
                .toList();
    }

    private boolean isValidLoginMethod(String method) {
        return "username".equals(method)
                || "email_password".equals(method)
                || "email_code".equals(method)
                || "phone_password".equals(method)
                || "phone_code".equals(method);
    }

    // --- Password reset ---
    public boolean isForgotPasswordEnabled() {
        return getConfig().getBoolean("forgot_password.enabled", false);
    }

    public List<String> getForgotPasswordMethods() {
        List<String> methods = normalizeCodeMethods(getConfig().getStringList("forgot_password.allowed_methods"));
        if (methods.isEmpty()) {
            methods = normalizeCodeMethods(getConfig().getStringList("forgot_password.forgot_password_methods"));
        }
        if (methods.isEmpty()) {
            methods = normalizeCodeMethods(getConfig().getStringList("forgot_password.methods"));
        }
        return methods.isEmpty() ? List.of("email_code") : methods;
    }

    public boolean isForgotPasswordMethodEnabled(String method) {
        return method != null && getForgotPasswordMethods().contains(method.trim().toLowerCase());
    }

    public List<String> getUserPasswordResetMethods() {
        List<String> methods = normalizePasswordResetMethods(getConfig().getStringList("user.password_reset_methods"));
        return methods.isEmpty() ? List.of("current_password") : methods;
    }

    public boolean isUserPasswordResetMethodEnabled(String method) {
        return method != null && getUserPasswordResetMethods().contains(method.trim().toLowerCase());
    }

    private List<String> normalizeCodeMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return Collections.emptyList();
        }
        return methods.stream()
                .filter(method -> method != null && !method.trim().isEmpty())
                .map(method -> method.trim().toLowerCase())
                .map(method -> "email".equals(method) ? "email_code" : method)
                .map(method -> "phone".equals(method) || "sms".equals(method) ? "phone_code" : method)
                .filter(method -> "email_code".equals(method) || "phone_code".equals(method))
                .distinct()
                .toList();
    }

    private List<String> normalizePasswordResetMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return Collections.emptyList();
        }
        return methods.stream()
                .filter(method -> method != null && !method.trim().isEmpty())
                .map(method -> method.trim().toLowerCase())
                .map(method -> "email".equals(method) ? "email_code" : method)
                .map(method -> "phone".equals(method) || "sms".equals(method) ? "phone_code" : method)
                .filter(method -> "current_password".equals(method) || "email_code".equals(method) || "phone_code".equals(method))
                .distinct()
                .toList();
    }

    public int getEmailCodeLength() {
        int length = getConfig().getInt("verification.email.code_length",
                getConfig().getInt("forgot_password.code_length", 6));
        return Math.max(4, Math.min(length, 8));
    }

    public int getEmailCodeExpireSeconds() {
        return Math.max(60, getConfig().getInt("verification.email.expire_seconds",
                getConfig().getInt("forgot_password.expire_seconds", 300)));
    }

    public int getEmailCodeCooldownSeconds() {
        return Math.max(1, getConfig().getInt("verification.email.send_cooldown_seconds",
                getConfig().getInt("forgot_password.send_cooldown_seconds", 60)));
    }

    public enum VerifyIdentifier {
        USERNAME("username"),
        EMAIL("email"),
        PHONE("phone");

        private final String configPrefix;

        VerifyIdentifier(String configPrefix) {
            this.configPrefix = configPrefix;
        }

        public String configPrefix() {
            return configPrefix;
        }
    }

    // --- Email ---
    public String getEmailSubject() {
        return getConfig().getString("email_subject", "VerifyMC Verification Code");
    }

    public boolean isEmailDomainWhitelistEnabled() {
        return getConfig().getBoolean("enable_email_domain_whitelist", true);
    }

    public List<String> getEmailDomainWhitelist() {
        List<String> list = null;
        try {
            list = getConfig().getStringList("email_domain_whitelist");
        } catch (Exception ignored) {}
        if (list == null || list.isEmpty()) {
            return DEFAULT_EMAIL_DOMAIN_WHITELIST;
        }
        return list;
    }

    public boolean isEmailAliasLimitEnabled() {
        return getConfig().getBoolean("enable_email_alias_limit", false);
    }

    public int getMaxAccountsPerEmail() {
        return getConfig().getInt("max_accounts_per_email", 2);
    }

    // --- SMS ---
    public String getSmsProvider() {
        return getConfig().getString("sms.provider", "aliyun").trim().toLowerCase();
    }

    public int getSmsCodeLength() {
        int length = getConfig().getInt("sms.code_length", 6);
        return Math.max(4, Math.min(length, 8));
    }

    public int getSmsExpireSeconds() {
        return Math.max(60, getConfig().getInt("sms.expire_seconds", 300));
    }

    public int getSmsMaxAttempts() {
        return Math.max(1, getConfig().getInt("sms.max_attempts", 5));
    }

    public int getSmsSendCooldownSeconds() {
        return Math.max(1, getConfig().getInt("sms.send_cooldown_seconds", 60));
    }

    public int getSmsRateLimitIpMax() {
        return Math.max(1, getConfig().getInt("sms.rate_limit.ip.max", 5));
    }

    public long getSmsRateLimitIpWindowMs() {
        return Math.max(1000L, getConfig().getLong("sms.rate_limit.ip.window_ms", 60000L));
    }

    public int getSmsConnectTimeoutMs() {
        return Math.max(1000, getConfig().getInt("sms.connect_timeout_ms", 5000));
    }

    public int getSmsRequestTimeoutMs() {
        return Math.max(1000, getConfig().getInt("sms.request_timeout_ms", 10000));
    }

    public int getMaxAccountsPerPhone() {
        return Math.max(1, getConfig().getInt("sms.phone.max_accounts_per_phone", 2));
    }

    public String getAliyunSmsAccessKeyId() {
        return getConfig().getString("sms.aliyun.access_key_id", "");
    }

    public String getAliyunSmsAccessKeySecret() {
        return getConfig().getString("sms.aliyun.access_key_secret", "");
    }

    public String getAliyunSmsEndpoint() {
        return getConfig().getString("sms.aliyun.endpoint", "dysmsapi.aliyuncs.com");
    }

    public String getAliyunSmsSignName() {
        return getConfig().getString("sms.aliyun.sign_name", "");
    }

    public String getAliyunSmsTemplateCode() {
        return getConfig().getString("sms.aliyun.template_code", "");
    }

    public String getAliyunSmsCodeParamName() {
        return getConfig().getString("sms.aliyun.template_param_name_code", "code");
    }

    public String getAliyunSmsExpireParamName() {
        return getConfig().getString("sms.aliyun.template_param_name_expire", "expire");
    }

    public String getTencentSmsSecretId() {
        return getConfig().getString("sms.tencent.secret_id", "");
    }

    public String getTencentSmsSecretKey() {
        return getConfig().getString("sms.tencent.secret_key", "");
    }

    public String getTencentSmsEndpoint() {
        return getConfig().getString("sms.tencent.endpoint", "sms.tencentcloudapi.com");
    }

    public String getTencentSmsRegion() {
        return getConfig().getString("sms.tencent.region", "ap-guangzhou");
    }

    public String getTencentSmsSdkAppId() {
        return getConfig().getString("sms.tencent.sdk_app_id", "");
    }

    public String getTencentSmsSignName() {
        return getConfig().getString("sms.tencent.sign_name", "");
    }

    public String getTencentSmsTemplateId() {
        return getConfig().getString("sms.tencent.template_id", "");
    }

    // --- Whitelist ---
    public String getWhitelistMode() {
        return getConfig().getString("whitelist_mode", "bukkit");
    }

    // --- Auto Update Resources ---
    public boolean isAutoUpdateResources() {
        return getConfig().getBoolean("auto_update_resources", true);
    }

    // --- AuthMe ---
    public boolean isAuthmeEnabled() {
        return getConfig().getBoolean("authme.enabled", false);
    }

    public String getAuthmePasswordRegex() {
        return getConfig().getString("authme.password_regex", "^[a-zA-Z0-9_]{8,26}$");
    }

    public int getAuthmeSyncInterval() {
        return getConfig().getInt("authme.database.sync_interval_seconds", 30);
    }

    // --- Captcha ---
    public String getCaptchaType() {
        return getConfig().getString("captcha.type", "math");
    }

    // --- Bedrock ---
    public boolean isBedrockEnabled() {
        return getConfig().getBoolean("bedrock.enabled", false);
    }

    public String getBedrockPrefix() {
        return getConfig().getString("bedrock.prefix", ".");
    }

    public String getBedrockUsernameRegex() {
        return getConfig().getString("bedrock.username_regex", "^[a-zA-Z0-9._-]{3,15}$");
    }

    // --- Registration ---
    public boolean isAutoApprove() {
        return getConfig().getBoolean("register.auto_approve", false);
    }

    // --- Questionnaire rate limit ---
    public int getQuestionnaireRateLimitIpMax() {
        return getConfig().getInt("questionnaire.rate_limit.ip.max", 20);
    }

    public int getQuestionnaireRateLimitUuidMax() {
        return getConfig().getInt("questionnaire.rate_limit.uuid.max", 8);
    }

    public int getQuestionnaireRateLimitEmailMax() {
        return getConfig().getInt("questionnaire.rate_limit.email.max", 6);
    }

    public long getQuestionnaireRateLimitWindowMs() {
        return getConfig().getLong("questionnaire.rate_limit.window_ms", 300000L);
    }

    // --- MySQL config ---
    public java.util.Properties getMysqlProperties() {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("host", getConfig().getString("mysql.host", "localhost"));
        props.setProperty("port", String.valueOf(getConfig().getInt("mysql.port", 3306)));
        props.setProperty("database", getConfig().getString("mysql.database", "verifymc"));
        props.setProperty("user", getConfig().getString("mysql.user", "root"));
        props.setProperty("password", getConfig().getString("mysql.password", ""));
        props.setProperty("useSSL", String.valueOf(getMysqlUseSSL()));
        props.setProperty("allowPublicKeyRetrieval", String.valueOf(getMysqlAllowPublicKeyRetrieval()));
        return props;
    }

    /**
     * Get MySQL SSL setting. Default is true for security.
     */
    public boolean getMysqlUseSSL() {
        return getConfig().getBoolean("mysql.useSSL", true);
    }

    /**
     * Get MySQL allowPublicKeyRetrieval setting. Default is false for security.
     * Enable this if you need to connect to MySQL 8.0+ with default authentication.
     */
    public boolean getMysqlAllowPublicKeyRetrieval() {
        return getConfig().getBoolean("mysql.allowPublicKeyRetrieval", false);
    }

    public java.util.List<java.util.Map<String, Object>> getDownloadResources() {
        java.util.List<java.util.Map<String, Object>> resources = new java.util.ArrayList<>();
        org.bukkit.configuration.ConfigurationSection section = getConfig().getConfigurationSection("downloads");
        if (section == null) {
            return resources;
        }
        for (String key : section.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection resourceSection = section.getConfigurationSection(key);
            if (resourceSection != null) {
                java.util.Map<String, Object> resource = new java.util.HashMap<>();
                resource.put("id", key);
                resource.put("name", resourceSection.getString("name", key));
                resource.put("description", resourceSection.getString("description", ""));
                resource.put("version", resourceSection.getString("version", ""));
                resource.put("size", resourceSection.getString("size", ""));
                resource.put("url", resourceSection.getString("url", ""));
                resource.put("icon", resourceSection.getString("icon", "package"));
                resources.add(resource);
            }
        }
        return resources;
    }
}
