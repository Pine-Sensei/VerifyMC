package team.kitemc.verifymc.service;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import team.kitemc.verifymc.util.EmailAddressUtil;

public class VerifyCodeService {
    private static final int MAX_ATTEMPTS = 5;

    private final ConcurrentHashMap<String, CodeEntry> codeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> rateLimitMap = new ConcurrentHashMap<>();
    private final long expireMillis = 5 * 60 * 1000;
    private final long rateLimitMillis = 60 * 1000;
    private final boolean debug;
    private final org.bukkit.plugin.Plugin plugin;
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile boolean running = true;

    public VerifyCodeService(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
        startCleanupTask();
    }

    public VerifyCodeService() {
        this.plugin = null;
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

        codeMap.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().expire < currentTime;
            if (expired) {
                debugLog("Removed expired code for key: " + entry.getKey());
            }
            return expired;
        });

        rateLimitMap.entrySet().removeIf(entry -> {
            boolean expired = (currentTime - entry.getValue()) > rateLimitMillis;
            if (expired) {
                debugLog("Removed expired rate limit for email: " + entry.getKey());
            }
            return expired;
        });

        debugLog("Cleanup completed. Active codes: " + codeMap.size() + ", Active rate limits: " + rateLimitMap.size());
    }

    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] VerifyCodeService: " + msg);
        }
    }

    public boolean canSendCode(String email) {
        String normalizedEmail = EmailAddressUtil.normalize(email);
        debugLog("canSendCode called for email: " + normalizedEmail);
        Long lastSentTime = rateLimitMap.get(normalizedEmail);
        if (lastSentTime == null) {
            debugLog("No previous send record for email: " + normalizedEmail);
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastSent = currentTime - lastSentTime;
        boolean canSend = timeSinceLastSent >= rateLimitMillis;

        debugLog("Email: " + normalizedEmail + ", last sent: " + lastSentTime + ", time since: " + timeSinceLastSent + "ms, can send: " + canSend);

        if (canSend) {
            rateLimitMap.remove(normalizedEmail);
        }

        return canSend;
    }

    public long getRemainingCooldownSeconds(String email) {
        Long lastSentTime = rateLimitMap.get(EmailAddressUtil.normalize(email));
        if (lastSentTime == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastSent = currentTime - lastSentTime;
        long remainingMillis = rateLimitMillis - timeSinceLastSent;
        return toRemainingSeconds(remainingMillis);
    }

    public CodeIssueResult issueCode(String email) {
        String normalizedEmail = EmailAddressUtil.normalize(email);
        long currentTime = System.currentTimeMillis();
        AtomicReference<CodeIssueResult> resultRef = new AtomicReference<>();

        rateLimitMap.compute(normalizedEmail, (key, lastSentTime) -> {
            if (lastSentTime != null) {
                long remainingMillis = rateLimitMillis - (currentTime - lastSentTime);
                long remainingSeconds = toRemainingSeconds(remainingMillis);
                if (remainingSeconds > 0) {
                    resultRef.set(CodeIssueResult.rateLimited(remainingSeconds));
                    return lastSentTime;
                }
            }

            String code = String.format("%06d", secureRandom.nextInt(1000000));
            long expireTime = currentTime + expireMillis;
            codeMap.put(key, new CodeEntry(code, expireTime));
            resultRef.set(CodeIssueResult.issued(code, toRemainingSeconds(rateLimitMillis)));
            debugLog("Issued code for key: " + key + ", expires at: " + expireTime + ", rate limit recorded at: " + currentTime);
            return currentTime;
        });

        return resultRef.get();
    }

    public String generateCode(String key) {
        CodeIssueResult result = issueCode(key);
        return result.issued() ? result.code() : null;
    }

    public void revokeCode(String key) {
        String normalizedKey = EmailAddressUtil.normalize(key);
        codeMap.remove(normalizedKey);
        rateLimitMap.remove(normalizedKey);
        debugLog("Revoked code and cooldown for key: " + normalizedKey);
    }

    public boolean checkCode(String key, String code) {
        String normalizedKey = EmailAddressUtil.normalize(key);
        debugLog("checkCode called: key=" + normalizedKey + ", code=" + code);
        CodeEntry entry = codeMap.get(normalizedKey);
        if (entry == null) {
            debugLog("No code found for key: " + normalizedKey);
            return false;
        }

        if (entry.attempts >= MAX_ATTEMPTS) {
            debugLog("Too many attempts for key: " + normalizedKey + ", attempts: " + entry.attempts);
            codeMap.remove(normalizedKey);
            return false;
        }

        if (entry.expire < System.currentTimeMillis()) {
            debugLog("Code expired for key: " + normalizedKey + ", expired at: " + entry.expire);
            codeMap.remove(normalizedKey);
            return false;
        }

        entry.attempts++;
        boolean ok = entry.code.equals(code);
        debugLog("Code verification result: " + ok + " (attempts: " + entry.attempts + ")");
        if (ok) {
            debugLog("Removing used code for key: " + normalizedKey);
            codeMap.remove(normalizedKey);
        }
        return ok;
    }

    private long toRemainingSeconds(long remainingMillis) {
        if (remainingMillis <= 0) {
            return 0;
        }
        return (remainingMillis + 999) / 1000;
    }

    public record CodeIssueResult(boolean issued, String code, long remainingSeconds) {
        public static CodeIssueResult issued(String code, long remainingSeconds) {
            return new CodeIssueResult(true, code, remainingSeconds);
        }

        public static CodeIssueResult rateLimited(long remainingSeconds) {
            return new CodeIssueResult(false, null, remainingSeconds);
        }
    }

    static class CodeEntry {
        String code;
        long expire;
        int attempts;

        CodeEntry(String code, long expire) {
            this.code = code;
            this.expire = expire;
            this.attempts = 0;
        }
    }
}
