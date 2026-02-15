package team.kitemc.verifymc.domain.service;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class VerificationCodeService {
    private final ConfigurationService configService;
    private final boolean debug;

    private final ConcurrentHashMap<String, CodeEntry> codeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> rateLimitMap = new ConcurrentHashMap<>();

    private final long expireMillis;
    private final long rateLimitMillis;

    public VerificationCodeService(ConfigurationService configService) {
        this.configService = configService;
        this.debug = configService.isDebug();
        this.expireMillis = configService.getLong("verification.code_expire_minutes", 5) * 60 * 1000;
        this.rateLimitMillis = configService.getLong("verification.rate_limit_seconds", 60) * 1000;
        startCleanupTask();
    }

    public String generateCode(String email) {
        debugLog("generateCode called for email: " + email);
        String code = String.format("%06d", new Random().nextInt(1000000));
        long expireTime = System.currentTimeMillis() + expireMillis;
        long currentTime = System.currentTimeMillis();

        rateLimitMap.put(email, currentTime);
        codeMap.put(email, new CodeEntry(code, email, currentTime, expireTime));

        debugLog("Generated code: " + code + " for email: " + email + ", expires at: " + expireTime);
        return code;
    }

    public boolean checkCode(String email, String code) {
        debugLog("checkCode called: email=" + email + ", code=" + code);
        CodeEntry entry = codeMap.get(email);
        if (entry == null) {
            debugLog("No code found for email: " + email);
            return false;
        }
        if (entry.expiresAt < System.currentTimeMillis()) {
            debugLog("Code expired for email: " + email + ", expired at: " + entry.expiresAt);
            codeMap.remove(email);
            return false;
        }
        boolean ok = entry.code.equals(code);
        debugLog("Code verification result: " + ok);
        if (ok) {
            debugLog("Removing used code for email: " + email);
            codeMap.remove(email);
        }
        return ok;
    }

    public boolean canSendCode(String email) {
        debugLog("canSendCode called for email: " + email);
        Long lastSentTime = rateLimitMap.get(email);
        if (lastSentTime == null) {
            debugLog("No previous send record for email: " + email);
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastSent = currentTime - lastSentTime;
        boolean canSend = timeSinceLastSent >= rateLimitMillis;

        debugLog("Email: " + email + ", time since last sent: " + timeSinceLastSent + "ms, can send: " + canSend);

        if (canSend) {
            rateLimitMap.remove(email);
        }

        return canSend;
    }

    public long getRemainingCooldownSeconds(String email) {
        Long lastSentTime = rateLimitMap.get(email);
        if (lastSentTime == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastSent = currentTime - lastSentTime;
        long remainingMillis = rateLimitMillis - timeSinceLastSent;

        return remainingMillis > 0 ? (remainingMillis / 1000) + 1 : 0;
    }

    public void cleanupExpiredCodes() {
        long currentTime = System.currentTimeMillis();
        int codesCleaned = 0;
        int rateLimitsCleaned = 0;

        Iterator<Map.Entry<String, CodeEntry>> codeIterator = codeMap.entrySet().iterator();
        while (codeIterator.hasNext()) {
            Map.Entry<String, CodeEntry> entry = codeIterator.next();
            if (entry.getValue().expiresAt < currentTime) {
                codeIterator.remove();
                codesCleaned++;
            }
        }

        Iterator<Map.Entry<String, Long>> rateLimitIterator = rateLimitMap.entrySet().iterator();
        while (rateLimitIterator.hasNext()) {
            Map.Entry<String, Long> entry = rateLimitIterator.next();
            if (currentTime - entry.getValue() > rateLimitMillis) {
                rateLimitIterator.remove();
                rateLimitsCleaned++;
            }
        }

        if (codesCleaned > 0 || rateLimitsCleaned > 0) {
            debugLog("Cleanup completed: " + codesCleaned + " codes, " + rateLimitsCleaned + " rate limits removed");
        }
    }

    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(300000);
                    cleanupExpiredCodes();
                } catch (InterruptedException e) {
                    debugLog("Cleanup task interrupted");
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        debugLog("Cleanup task started");
    }

    private void debugLog(String msg) {
        if (debug) {
            configService.getLogger().info("[DEBUG] VerificationCodeService: " + msg);
        }
    }

    public static class CodeEntry {
        public final String code;
        public final String email;
        public final long createdAt;
        public final long expiresAt;

        public CodeEntry(String code, String email, long createdAt, long expiresAt) {
            this.code = code;
            this.email = email;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }
}
