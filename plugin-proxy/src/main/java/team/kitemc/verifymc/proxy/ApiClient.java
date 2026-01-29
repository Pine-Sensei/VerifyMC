package team.kitemc.verifymc.proxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * HTTP API client for communicating with VerifyMC backend
 */
public class ApiClient {
    private final ProxyConfig config;
    private final Logger logger;
    private final Gson gson = new Gson();
    
    // Simple cache for whitelist status
    private final Map<String, CachedStatus> statusCache = new ConcurrentHashMap<>();
    
    public ApiClient(ProxyConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        
        // Start cache cleanup thread
        if (config.isCacheEnabled()) {
            startCacheCleanup();
        }
    }
    
    /**
     * Check if a player is on the whitelist
     * @param username Player's username
     * @return WhitelistStatus or null if error
     */
    public WhitelistStatus checkWhitelist(String username) {
        // Check cache first
        if (config.isCacheEnabled()) {
            CachedStatus cached = statusCache.get(username.toLowerCase());
            if (cached != null && !cached.isExpired()) {
                if (config.isDebug()) {
                    logger.info("[DEBUG] Cache hit for: " + username);
                }
                return cached.status;
            }
        }
        
        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String url = config.getBackendUrl() + "/api/check-whitelist?username=" + encodedUsername;
            
            if (config.isDebug()) {
                logger.info("[DEBUG] API Request: " + url);
            }
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(config.getTimeout());
            conn.setReadTimeout(config.getTimeout());
            conn.setRequestProperty("Accept", "application/json");
            
            // Add API key header if configured
            String apiKey = config.getApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("X-API-Key", apiKey);
            }
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                if (config.isDebug()) {
                    logger.info("[DEBUG] API Response: " + response);
                }
                
                // Parse response
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                WhitelistStatus status = new WhitelistStatus();
                
                if (json.has("success") && json.get("success").getAsBoolean()) {
                    status.setFound(true);
                    
                    if (json.has("status")) {
                        status.setStatus(json.get("status").getAsString());
                    }
                    if (json.has("username")) {
                        status.setUsername(json.get("username").getAsString());
                    }
                } else {
                    status.setFound(false);
                }
                
                // Cache the result
                if (config.isCacheEnabled()) {
                    statusCache.put(username.toLowerCase(), new CachedStatus(status, config.getCacheExpireSeconds()));
                }
                
                return status;
            } else {
                logger.warning("API returned status code: " + responseCode);
                return null;
            }
            
        } catch (Exception e) {
            logger.warning("Failed to check whitelist: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Start background thread to clean up expired cache entries
     */
    private void startCacheCleanup() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Clean every minute
                    
                    long now = System.currentTimeMillis();
                    statusCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
                    
                    if (config.isDebug()) {
                        logger.info("[DEBUG] Cache cleanup completed. Cache size: " + statusCache.size());
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("VerifyMC-CacheCleanup");
        cleanupThread.start();
    }
    
    /**
     * Clear the cache
     */
    public void clearCache() {
        statusCache.clear();
    }
    
    /**
     * Whitelist status response class
     */
    public static class WhitelistStatus {
        private boolean found;
        private String status;
        private String username;
        
        public boolean isFound() {
            return found;
        }
        
        public void setFound(boolean found) {
            this.found = found;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        /**
         * Check if player is approved for login
         */
        public boolean isApproved() {
            return found && "approved".equalsIgnoreCase(status);
        }
    }
    
    /**
     * Cache entry with expiration
     */
    private static class CachedStatus {
        final WhitelistStatus status;
        final long expireTime;
        
        CachedStatus(WhitelistStatus status, int expireSeconds) {
            this.status = status;
            this.expireTime = System.currentTimeMillis() + (expireSeconds * 1000L);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}

