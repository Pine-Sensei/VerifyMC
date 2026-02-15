package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.Plugin;

public class WebAuthHelper {
    private final Plugin plugin;
    private final long tokenExpiryTime;
    private final ConcurrentHashMap<String, Long> validTokens = new ConcurrentHashMap<>();

    public WebAuthHelper(Plugin plugin) {
        this.plugin = plugin;
        this.tokenExpiryTime = plugin.getConfig().getInt("web.admin_token_expire_seconds", 3600) * 1000L;
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
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String random = String.valueOf(Math.random());
            String secret = plugin.getConfig().getString("admin.password", "default_secret");

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String combined = timestamp + random + secret;
            byte[] hash = md.digest(combined.getBytes());
            String token = Base64.getEncoder().encodeToString(hash);
            validTokens.put(token, System.currentTimeMillis() + tokenExpiryTime);
            return token;
        } catch (NoSuchAlgorithmException e) {
            String token = "admin_token_" + System.currentTimeMillis() + "_" + Math.random();
            validTokens.put(token, System.currentTimeMillis() + tokenExpiryTime);
            return token;
        }
    }

    public void startTokenCleanupTask() {
        final long cleanupInterval = plugin.getConfig().getInt("web.token_cleanup_interval_seconds", 300) * 1000L;
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(cleanupInterval);
                    long currentTime = System.currentTimeMillis();
                    validTokens.entrySet().removeIf(entry -> entry.getValue() < currentTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private boolean validateToken(String token) {
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
}
