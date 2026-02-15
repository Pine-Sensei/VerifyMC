package team.kitemc.verifymc.infrastructure.web;

import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.Plugin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebAuthHelper {
    private static final Logger LOGGER = Logger.getLogger(WebAuthHelper.class.getName());
    private static final long DEFAULT_TOKEN_EXPIRY_MS = 3600000;
    private static final long CLEANUP_INTERVAL_MS = 300000;

    private final Plugin plugin;
    private final ConcurrentHashMap<String, TokenInfo> validTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final long tokenExpiryMs;
    private ScheduledExecutorService cleanupExecutor;

    public WebAuthHelper(Plugin plugin) {
        this(plugin, DEFAULT_TOKEN_EXPIRY_MS);
    }

    public WebAuthHelper(Plugin plugin, long tokenExpiryMs) {
        this.plugin = plugin;
        this.tokenExpiryMs = tokenExpiryMs;
    }

    public boolean isAuthenticated(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7).trim();
        return validateToken(token);
    }

    public AuthResult authenticate(String username, String password) {
        String adminPassword = plugin.getConfig().getString("admin.password", "");
        String adminUsername = plugin.getConfig().getString("admin.username", "admin");

        if (adminPassword.isEmpty()) {
            LOGGER.warning("Admin password not configured");
            return AuthResult.failure("Admin authentication not configured");
        }

        if (!adminUsername.equals(username)) {
            return AuthResult.failure("Invalid credentials");
        }

        if (!verifyPassword(password, adminPassword)) {
            return AuthResult.failure("Invalid credentials");
        }

        String token = generateSecureToken();
        return AuthResult.success(token, tokenExpiryMs);
    }

    public String generateSecureToken() {
        try {
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);

            String timestamp = String.valueOf(System.currentTimeMillis());
            String secret = plugin.getConfig().getString("admin.password", "default_secret");

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(randomBytes);
            md.update(timestamp.getBytes());
            md.update(secret.getBytes());
            byte[] hash = md.digest();

            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            long expiryTime = System.currentTimeMillis() + tokenExpiryMs;
            validTokens.put(token, new TokenInfo(expiryTime, "admin"));

            LOGGER.log(Level.FINE, "Generated new auth token, expires at {0}", expiryTime);
            return token;
        } catch (NoSuchAlgorithmException e) {
            String token = "fallback_" + System.currentTimeMillis() + "_" + secureRandom.nextInt();
            validTokens.put(token, new TokenInfo(System.currentTimeMillis() + tokenExpiryMs, "admin"));
            return token;
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        TokenInfo tokenInfo = validTokens.get(token);
        if (tokenInfo == null) {
            return false;
        }

        if (System.currentTimeMillis() > tokenInfo.expiryTime) {
            validTokens.remove(token);
            LOGGER.log(Level.FINE, "Token expired and removed");
            return false;
        }

        return true;
    }

    public void invalidateToken(String token) {
        if (token != null) {
            validTokens.remove(token);
            LOGGER.log(Level.FINE, "Token invalidated");
        }
    }

    public void invalidateAllTokens() {
        validTokens.clear();
        LOGGER.info("All tokens invalidated");
    }

    public void startTokenCleanupTask() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }

        cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredTokens,
            CLEANUP_INTERVAL_MS,
            CLEANUP_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );

        LOGGER.info("Token cleanup task started");
    }

    public void stopTokenCleanupTask() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            cleanupExecutor = null;
        }
    }

    private void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        for (var entry : validTokens.entrySet()) {
            if (entry.getValue().expiryTime < currentTime) {
                validTokens.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            LOGGER.log(Level.FINE, "Cleaned up {0} expired tokens", removedCount);
        }
    }

    private boolean verifyPassword(String inputPassword, String storedPassword) {
        return inputPassword.equals(storedPassword);
    }

    public int getActiveTokenCount() {
        return validTokens.size();
    }

    private static class TokenInfo {
        final long expiryTime;
        final String username;

        TokenInfo(long expiryTime, String username) {
            this.expiryTime = expiryTime;
            this.username = username;
        }
    }

    public static class AuthResult {
        private final boolean success;
        private final String token;
        private final long expiresIn;
        private final String error;

        private AuthResult(boolean success, String token, long expiresIn, String error) {
            this.success = success;
            this.token = token;
            this.expiresIn = expiresIn;
            this.error = error;
        }

        public static AuthResult success(String token, long expiresIn) {
            return new AuthResult(true, token, expiresIn, null);
        }

        public static AuthResult failure(String error) {
            return new AuthResult(false, null, 0, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getToken() {
            return token;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public String getError() {
            return error;
        }
    }
}
