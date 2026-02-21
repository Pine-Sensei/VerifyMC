package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.Plugin;

public class WebAuthHelper {
    private static final long TOKEN_EXPIRY_TIME = 3600000;
    private final Plugin plugin;
    private final ConcurrentHashMap<String, TokenInfo> validTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile Thread cleanupThread;

    private static class TokenInfo {
        final long expiryTime;
        final String username;

        TokenInfo(long expiryTime, String username) {
            this.expiryTime = expiryTime;
            this.username = username;
        }
    }

    public WebAuthHelper(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAuthenticated(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        return validateToken(token);
    }

    public String generateSecureToken(String username) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(randomBytes);
            md.update(Long.toString(System.currentTimeMillis()).getBytes());
            byte[] hash = md.digest();
            String token = Base64.getEncoder().encodeToString(hash);
            validTokens.put(token, new TokenInfo(System.currentTimeMillis() + TOKEN_EXPIRY_TIME, username));
            return token;
        } catch (NoSuchAlgorithmException e) {
            String token = Base64.getEncoder().encodeToString(randomBytes);
            validTokens.put(token, new TokenInfo(System.currentTimeMillis() + TOKEN_EXPIRY_TIME, username));
            return token;
        }
    }

    public String generateSecureToken() {
        return generateSecureToken("system");
    }

    public String generateToken(String username) {
        return generateSecureToken(username);
    }

    public String generateToken() {
        return generateSecureToken("system");
    }

    public void startTokenCleanupTask() {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(300000);
                    long currentTime = System.currentTimeMillis();
                    validTokens.entrySet().removeIf(entry -> entry.getValue().expiryTime < currentTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        t.setDaemon(true);
        t.setName("VerifyMC-TokenCleanup");
        t.start();
        this.cleanupThread = t;
    }

    public void stopTokenCleanupTask() {
        Thread t = this.cleanupThread;
        if (t != null) {
            t.interrupt();
        }
    }

    public boolean validateToken(String token) {
        TokenInfo tokenInfo = validTokens.get(token);
        if (tokenInfo == null) {
            return false;
        }
        if (System.currentTimeMillis() > tokenInfo.expiryTime) {
            validTokens.remove(token);
            return false;
        }
        return true;
    }

    public String getUsername(String token) {
        TokenInfo tokenInfo = validTokens.get(token);
        if (tokenInfo == null || System.currentTimeMillis() > tokenInfo.expiryTime) {
            return null;
        }
        return tokenInfo.username;
    }

    /**
     * Check if a token is valid (alias for validateToken)
     * @param token Token string to validate
     * @return true if token is valid
     */
    public boolean isValidToken(String token) {
        return validateToken(token);
    }
}
