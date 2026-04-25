package team.kitemc.verifymc.sms;

import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.core.ConfigManager;
import team.kitemc.verifymc.util.PhoneNumberUtil;

public class SmsService {
    private final ConfigManager config;
    private final SmsProvider provider;
    private final boolean debug;
    private final Plugin plugin;

    public SmsService(Plugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.debug = config.isDebug();
        this.provider = "tencent".equalsIgnoreCase(config.getSmsProvider())
                ? new TencentSmsProvider(config)
                : new AliyunSmsProvider(config);
    }

    public SmsSendResult sendVerificationCode(String phone, String code) {
        String normalizedPhone = PhoneNumberUtil.normalize(phone);
        int expireMinutes = Math.max(1, (config.getSmsExpireSeconds() + 59) / 60);
        SmsSendResult result = provider.sendVerificationCode(normalizedPhone, code, expireMinutes);
        if (!result.success()) {
            debugLog("SMS send failed for " + normalizedPhone + ": " + result.message());
        }
        return result;
    }

    private void debugLog(String msg) {
        if (debug && plugin != null) {
            plugin.getLogger().info("[DEBUG] SmsService: " + msg);
        }
    }
}
