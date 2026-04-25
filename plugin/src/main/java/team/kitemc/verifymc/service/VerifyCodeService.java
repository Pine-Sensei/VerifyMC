package team.kitemc.verifymc.service;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.core.ConfigManager;
import team.kitemc.verifymc.util.EmailAddressUtil;
import team.kitemc.verifymc.util.PhoneNumberUtil;

public class VerifyCodeService {
    private static final int DEFAULT_EMAIL_MAX_ATTEMPTS = 5;
    private static final long DEFAULT_EMAIL_EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final long DEFAULT_EMAIL_RATE_LIMIT_MILLIS = 60 * 1000L;

    private final ConcurrentHashMap<String, CodeEntry> codeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> targetRateLimitMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IpRateEntry> ipRateLimitMap = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private final ConfigManager configManager;
    private final boolean debug;
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile boolean running = true;

    public VerifyCodeService(Plugin plugin) {
        this(plugin, plugin instanceof org.bukkit.plugin.java.JavaPlugin javaPlugin ? new ConfigManager(javaPlugin) : null);
    }

    public VerifyCodeService(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.debug = plugin.getConfig().getBoolean("debug", false);
        startCleanupTask();
    }

    public VerifyCodeService() {
        this.plugin = null;
        this.configManager = null;
        this.debug = false;
        startCleanupTask();
    }

    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(300000);
                    cleanupExpiredEntries();
                } catch (InterruptedException e) {
                    debugLog("Cleanup task interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("VerifyCodeService-Cleanup");
        cleanupThread.start();
        debugLog("Cleanup task started");
    }

    public void stop() {
        running = false;
        debugLog("Cleanup task stopped");
    }

    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        codeMap.entrySet().removeIf(entry -> entry.getValue().expire < currentTime);
        targetRateLimitMap.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > getRateLimitMillis(channelFromStoredKey(entry.getKey())));
        ipRateLimitMap.entrySet().removeIf(entry -> entry.getValue().windowStart + getSmsRateLimitIpWindowMs() < currentTime);
        debugLog("Cleanup completed. Active codes: " + codeMap.size()
                + ", target limits: " + targetRateLimitMap.size()
                + ", ip limits: " + ipRateLimitMap.size());
    }

    private void debugLog(String msg) {
        if (debug && plugin != null) {
            plugin.getLogger().info("[DEBUG] VerifyCodeService: " + msg);
        }
    }

    public boolean canSendCode(String email) {
        return getRemainingCooldownSeconds(Channel.EMAIL, email) <= 0;
    }

    public long getRemainingCooldownSeconds(String email) {
        return getRemainingCooldownSeconds(Channel.EMAIL, email);
    }

    public long getRemainingCooldownSeconds(Channel channel, String target) {
        Long lastSentTime = targetRateLimitMap.get(storedKey(channel, target));
        if (lastSentTime == null) {
            return 0;
        }
        long remainingMillis = getRateLimitMillis(channel) - (System.currentTimeMillis() - lastSentTime);
        return toRemainingSeconds(remainingMillis);
    }

    public boolean canSendFromIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        IpRateEntry entry = ipRateLimitMap.get(ip);
        if (entry == null) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - entry.windowStart >= getSmsRateLimitIpWindowMs()) {
            ipRateLimitMap.remove(ip);
            return true;
        }
        return entry.count < getSmsRateLimitIpMax();
    }

    public long getRemainingIpCooldownSeconds(String ip) {
        IpRateEntry entry = ipRateLimitMap.get(ip);
        if (entry == null) {
            return 0;
        }
        return toRemainingSeconds(getSmsRateLimitIpWindowMs() - (System.currentTimeMillis() - entry.windowStart));
    }

    public CodeIssueResult issueCode(String email) {
        return issueCode(Channel.EMAIL, email, null);
    }

    public CodeIssueResult issueCode(Channel channel, String target, String ip) {
        String normalizedTarget = normalize(channel, target);
        if (channel == Channel.SMS && ip != null && !canSendFromIp(ip)) {
            return CodeIssueResult.rateLimited(getRemainingIpCooldownSeconds(ip));
        }

        long currentTime = System.currentTimeMillis();
        AtomicReference<CodeIssueResult> resultRef = new AtomicReference<>();
        String key = storedKey(channel, normalizedTarget);

        targetRateLimitMap.compute(key, (ignored, lastSentTime) -> {
            if (lastSentTime != null) {
                long remainingMillis = getRateLimitMillis(channel) - (currentTime - lastSentTime);
                long remainingSeconds = toRemainingSeconds(remainingMillis);
                if (remainingSeconds > 0) {
                    resultRef.set(CodeIssueResult.rateLimited(remainingSeconds));
                    return lastSentTime;
                }
            }

            String code = generateNumericCode(getCodeLength(channel));
            long expireTime = currentTime + getExpireMillis(channel);
            codeMap.put(key, new CodeEntry(code, expireTime, getMaxAttempts(channel)));
            if (channel == Channel.SMS && ip != null && !ip.isEmpty()) {
                recordIpSend(ip, currentTime);
            }
            resultRef.set(CodeIssueResult.issued(code, toRemainingSeconds(getRateLimitMillis(channel))));
            debugLog("Issued " + channel + " code for key: " + key);
            return currentTime;
        });

        return resultRef.get();
    }

    public String generateCode(String key) {
        CodeIssueResult result = issueCode(key);
        return result.issued() ? result.code() : null;
    }

    public void revokeCode(String key) {
        revokeCode(Channel.EMAIL, key);
    }

    public void revokeCode(Channel channel, String target) {
        String storedKey = storedKey(channel, target);
        codeMap.remove(storedKey);
        targetRateLimitMap.remove(storedKey);
        debugLog("Revoked code and cooldown for key: " + storedKey);
    }

    public boolean checkCode(String key, String code) {
        return verifyCode(Channel.EMAIL, key, code).success();
    }

    public VerifyResult verifyCode(Channel channel, String target, String code) {
        String storedKey = storedKey(channel, target);
        CodeEntry entry = codeMap.get(storedKey);
        if (entry == null) {
            return VerifyResult.failure(0, false, false);
        }

        if (entry.expire < System.currentTimeMillis()) {
            codeMap.remove(storedKey);
            return VerifyResult.failure(0, true, false);
        }

        if (entry.attempts >= entry.maxAttempts) {
            codeMap.remove(storedKey);
            return VerifyResult.failure(0, false, true);
        }

        entry.attempts++;
        boolean ok = entry.code.equals(code == null ? "" : code.trim());
        int remainingAttempts = Math.max(0, entry.maxAttempts - entry.attempts);
        if (ok) {
            codeMap.remove(storedKey);
        } else if (remainingAttempts <= 0) {
            codeMap.remove(storedKey);
        }
        return new VerifyResult(ok, remainingAttempts, false, !ok && remainingAttempts <= 0);
    }

    private void recordIpSend(String ip, long currentTime) {
        ipRateLimitMap.compute(ip, (ignored, entry) -> {
            if (entry == null || currentTime - entry.windowStart >= getSmsRateLimitIpWindowMs()) {
                return new IpRateEntry(currentTime, 1);
            }
            entry.count++;
            return entry;
        });
    }

    private String generateNumericCode(int length) {
        int bound = (int) Math.pow(10, length);
        return String.format("%0" + length + "d", secureRandom.nextInt(bound));
    }

    private String storedKey(Channel channel, String target) {
        return channel.name().toLowerCase() + ":" + normalize(channel, target);
    }

    private String normalize(Channel channel, String target) {
        return channel == Channel.SMS
                ? PhoneNumberUtil.normalize(target)
                : EmailAddressUtil.normalize(target);
    }

    private Channel channelFromStoredKey(String key) {
        if (key != null && key.startsWith("sms:")) {
            return Channel.SMS;
        }
        return Channel.EMAIL;
    }

    private int getCodeLength(Channel channel) {
        return channel == Channel.SMS && configManager != null ? configManager.getSmsCodeLength() : 6;
    }

    private long getExpireMillis(Channel channel) {
        return channel == Channel.SMS && configManager != null
                ? configManager.getSmsExpireSeconds() * 1000L
                : DEFAULT_EMAIL_EXPIRE_MILLIS;
    }

    private long getRateLimitMillis(Channel channel) {
        return channel == Channel.SMS && configManager != null
                ? configManager.getSmsSendCooldownSeconds() * 1000L
                : DEFAULT_EMAIL_RATE_LIMIT_MILLIS;
    }

    private int getMaxAttempts(Channel channel) {
        return channel == Channel.SMS && configManager != null
                ? configManager.getSmsMaxAttempts()
                : DEFAULT_EMAIL_MAX_ATTEMPTS;
    }

    private int getSmsRateLimitIpMax() {
        return configManager != null ? configManager.getSmsRateLimitIpMax() : 5;
    }

    private long getSmsRateLimitIpWindowMs() {
        return configManager != null ? configManager.getSmsRateLimitIpWindowMs() : 60000L;
    }

    private long toRemainingSeconds(long remainingMillis) {
        if (remainingMillis <= 0) {
            return 0;
        }
        return (remainingMillis + 999) / 1000;
    }

    public enum Channel {
        EMAIL,
        SMS
    }

    public record CodeIssueResult(boolean issued, String code, long remainingSeconds) {
        public static CodeIssueResult issued(String code, long remainingSeconds) {
            return new CodeIssueResult(true, code, remainingSeconds);
        }

        public static CodeIssueResult rateLimited(long remainingSeconds) {
            return new CodeIssueResult(false, null, remainingSeconds);
        }
    }

    public record VerifyResult(boolean success, int remainingAttempts, boolean expired, boolean tooManyAttempts) {
        public static VerifyResult failure(int remainingAttempts, boolean expired, boolean tooManyAttempts) {
            return new VerifyResult(false, remainingAttempts, expired, tooManyAttempts);
        }
    }

    static class CodeEntry {
        String code;
        long expire;
        int attempts;
        int maxAttempts;

        CodeEntry(String code, long expire, int maxAttempts) {
            this.code = code;
            this.expire = expire;
            this.attempts = 0;
            this.maxAttempts = maxAttempts;
        }
    }

    static class IpRateEntry {
        long windowStart;
        int count;

        IpRateEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
