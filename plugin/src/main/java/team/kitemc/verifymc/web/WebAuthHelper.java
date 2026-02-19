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
    private final ConcurrentHashMap<String, Long> validTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile Thread cleanupThread;

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

    public String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        try {
            String secret = plugin.getConfig().getString("admin.password", "default_secret");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(randomBytes);
            md.update(secret.getBytes());
            byte[] hash = md.digest();
            String token = Base64.getEncoder().encodeToString(hash);
            validTokens.put(token, System.currentTimeMillis() + TOKEN_EXPIRY_TIME);
            return token;
        } catch (NoSuchAlgorithmException e) {
            String token = Base64.getEncoder().encodeToString(randomBytes);
            validTokens.put(token, System.currentTimeMillis() + TOKEN_EXPIRY_TIME);
            return token;
        }
    }

    /**
     * Generate a new authentication token (alias for generateSecureToken)
     * @return New token string
     */
    public String generateToken() {
        return generateSecureToken();
    }

    public void startTokenCleanupTask() {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(300000);
                    long currentTime = System.currentTimeMillis();
                    validTokens.entrySet().removeIf(entry -> entry.getValue() < currentTime);
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
        Long expiryTime = validTokens.get(token);
        if (expiryTime == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiryTime) {
            validTokens.remove(token);
            return false;
        }
        return true;
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
