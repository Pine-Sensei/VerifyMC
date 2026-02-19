package team.kitemc.verifymc.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

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

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
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

    public String getStorageType() {
        return getConfig().getString("storage", "file");
    }

    public String getLanguage() {
        return getConfig().getString("language", "en");
    }

    // --- Web server ---
    public int getWebPort() {
        return getConfig().getInt("web_port", 8080);
    }

    public int getWsPort() {
        return getConfig().getInt("ws_port", 8081);
    }

    public String getWebServerPrefix() {
        return getConfig().getString("web_server_prefix", "[VerifyMC]");
    }

    // --- Frontend ---
    public String getTheme() {
        return getConfig().getString("frontend.theme", "default");
    }

    public String getLogoUrl() {
        return getConfig().getString("frontend.logo_url", "");
    }

    public String getAnnouncement() {
        return getConfig().getString("frontend.announcement", "");
    }

    public String getUsernameRegex() {
        return getConfig().getString("username_regex", "^[a-zA-Z0-9_-]{3,16}$");
    }

    public boolean isUsernameCaseSensitive() {
        return getConfig().getBoolean("username_case_sensitive", false);
    }

    // --- Auth methods ---
    public List<String> getAuthMethods() {
        return getConfig().getStringList("auth_methods");
    }

    public boolean isEmailAuthEnabled() {
        return getAuthMethods().contains("email");
    }

    public boolean isCaptchaAuthEnabled() {
        return getAuthMethods().contains("captcha");
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

    // --- Whitelist ---
    public String getWhitelistMode() {
        return getConfig().getString("whitelist_mode", "bukkit");
    }

    public boolean isWhitelistSyncEnabled() {
        return getConfig().getBoolean("whitelist_sync", false);
    }

    // --- AuthMe ---
    public boolean isAuthmeEnabled() {
        return getConfig().getBoolean("authme.enabled", false);
    }

    public boolean isAuthmePasswordRequired() {
        return getConfig().getBoolean("authme.require_password", false);
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
        return getConfig().getString("bedrock.username_regex", "^\\\\.[a-zA-Z0-9_\\\\s]{3,16}$");
    }

    // --- Registration ---
    public boolean isAutoApprove() {
        return getConfig().getBoolean("register.auto_approve", false);
    }

    // --- Admin ---
    public String getAdminPassword() {
        return getConfig().getString("admin.password", "");
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
        return props;
    }
}
