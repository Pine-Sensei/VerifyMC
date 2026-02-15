package team.kitemc.verifymc.domain.service;

import org.json.JSONObject;
import org.json.JSONArray;

import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.repository.UserRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordIntegrationService {
    private final ConfigurationService configService;
    private final UserRepository userRepository;
    private final boolean debug;

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String guildId;
    private boolean required;

    private final Map<String, StateData> stateTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenData> tokenCache = new ConcurrentHashMap<>();

    private static final String DISCORD_API_BASE = "https://discord.com/api/v10";
    private static final String DISCORD_OAUTH_AUTHORIZE = "https://discord.com/oauth2/authorize";
    private static final String DISCORD_OAUTH_TOKEN = DISCORD_API_BASE + "/oauth2/token";

    private static final long STATE_EXPIRY_MS = 600000;
    private static final long TOKEN_CACHE_EXPIRY_MS = 3600000;

    public DiscordIntegrationService(ConfigurationService configService, UserRepository userRepository) {
        this.configService = configService;
        this.userRepository = userRepository;
        this.debug = configService.isDebug();
        loadConfig();
    }

    public void loadConfig() {
        clientId = configService.getString("discord.client_id", "");
        clientSecret = configService.getString("discord.client_secret", "");
        redirectUri = configService.getString("discord.redirect_uri", "");
        guildId = configService.getString("discord.guild_id", "");
        required = configService.getBoolean("discord.required", false);

        debugLog("Discord config loaded: clientId=" + (clientId.isEmpty() ? "not set" : "***") +
                 ", guildId=" + (guildId.isEmpty() ? "not set" : guildId));
    }

    public boolean isEnabled() {
        return configService.getBoolean("discord.enabled", false) &&
               !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    public boolean isRequired() {
        return isEnabled() && required;
    }

    public String generateAuthUrl(String username) {
        if (!isEnabled()) {
            return null;
        }

        String state = generateState();
        stateTokens.put(state, new StateData(username, System.currentTimeMillis()));

        StringBuilder url = new StringBuilder(DISCORD_OAUTH_AUTHORIZE);
        url.append("?client_id=").append(clientId);
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        url.append("&response_type=code");
        url.append("&scope=identify%20guilds");
        url.append("&state=").append(state);

        debugLog("Generated auth URL for " + username);

        return url.toString();
    }

    public DiscordCallbackResult handleCallback(String code, String state) {
        StateData stateData = stateTokens.remove(state);
        if (stateData == null || stateData.isExpired()) {
            debugLog("Invalid or expired state token: " + state);
            return new DiscordCallbackResult(false, "Invalid or expired state", null, null);
        }

        String username = stateData.username;

        try {
            DiscordToken token = exchangeCodeForToken(code);
            if (token == null) {
                return new DiscordCallbackResult(false, "Failed to exchange code for token", username, null);
            }

            DiscordUser user = getUserInfo(token.accessToken);
            if (user == null) {
                return new DiscordCallbackResult(false, "Failed to get user info", username, null);
            }

            if (userRepository != null && userRepository.existsByDiscordId(user.id)) {
                Optional<User> existingUser = userRepository.findByDiscordId(user.id);
                if (existingUser.isPresent()) {
                    if (!existingUser.get().getUsername().equalsIgnoreCase(username)) {
                        debugLog("Discord account already linked to: " + existingUser.get().getUsername());
                        return new DiscordCallbackResult(false, "This Discord account is already linked to another user", username, user);
                    }
                }
            }

            if (!guildId.isEmpty()) {
                boolean inGuild = checkGuildMembership(token.accessToken, guildId);
                if (!inGuild) {
                    debugLog("User " + user.username + " is not in guild " + guildId);
                    return new DiscordCallbackResult(false, "You must be a member of the Discord server", username, user);
                }
            }

            tokenCache.put(username.toLowerCase(), new TokenData(token, System.currentTimeMillis()));

            if (userRepository != null) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    userRepository.updateDiscordId(userOpt.get().getUuid(), user.id);
                    userRepository.flush();
                    debugLog("Persisted Discord ID to database for " + username + ": " + user.id);
                }
            }

            debugLog("Successfully linked Discord for " + username + ": " + user.username + "#" + user.discriminator);

            return new DiscordCallbackResult(true, "Discord account linked successfully", username, user);

        } catch (Exception e) {
            debugLog("OAuth callback error: " + e.getMessage());
            return new DiscordCallbackResult(false, "OAuth error: " + e.getMessage(), username, null);
        }
    }

    public boolean isLinked(String username) {
        if (userRepository != null) {
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isPresent()) {
                String discordId = user.get().getDiscordId();
                if (discordId != null && !discordId.isEmpty()) {
                    return true;
                }
            }
        }
        return tokenCache.containsKey(username.toLowerCase());
    }

    public DiscordUser getLinkedUser(String username) {
        String accessToken = getValidAccessToken(username);
        if (accessToken != null) {
            try {
                return getUserInfo(accessToken);
            } catch (Exception e) {
                debugLog("Failed to get linked user info from API: " + e.getMessage());
            }
        }

        String discordId = getLinkedDiscordId(username);
        if (discordId != null) {
            return new DiscordUser(discordId, null, "0", null, null);
        }

        return null;
    }

    public boolean unlinkDiscord(String username) {
        tokenCache.remove(username.toLowerCase());

        if (userRepository != null) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                boolean updated = userRepository.updateDiscordId(userOpt.get().getUuid(), null);
                userRepository.flush();
                return updated;
            }
        }

        return true;
    }

    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int statesCleaned = 0;
        int tokensCleaned = 0;

        Iterator<Map.Entry<String, StateData>> stateIterator = stateTokens.entrySet().iterator();
        while (stateIterator.hasNext()) {
            Map.Entry<String, StateData> entry = stateIterator.next();
            if (entry.getValue().isExpired()) {
                stateIterator.remove();
                statesCleaned++;
            }
        }

        Iterator<Map.Entry<String, TokenData>> tokenIterator = tokenCache.entrySet().iterator();
        while (tokenIterator.hasNext()) {
            Map.Entry<String, TokenData> entry = tokenIterator.next();
            if (now - entry.getValue().cacheTime > TOKEN_CACHE_EXPIRY_MS) {
                tokenIterator.remove();
                tokensCleaned++;
            }
        }

        if (statesCleaned > 0 || tokensCleaned > 0) {
            debugLog("Cleanup completed: " + statesCleaned + " state tokens, " + tokensCleaned + " cached tokens removed");
        }
    }

    private String getLinkedDiscordId(String username) {
        if (userRepository != null) {
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isPresent()) {
                String discordId = user.get().getDiscordId();
                if (discordId != null && !discordId.isEmpty()) {
                    return discordId;
                }
            }
        }
        return null;
    }

    private DiscordToken exchangeCodeForToken(String code) throws Exception {
        URL url = new URL(DISCORD_OAUTH_TOKEN);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        String body = "client_id=" + clientId +
                     "&client_secret=" + clientSecret +
                     "&grant_type=authorization_code" +
                     "&code=" + code +
                     "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            debugLog("Token exchange failed with status: " + conn.getResponseCode());
            return null;
        }

        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);

        return new DiscordToken(
            json.getString("access_token"),
            json.getString("refresh_token"),
            System.currentTimeMillis() + json.getLong("expires_in") * 1000
        );
    }

    private DiscordToken refreshAccessToken(String refreshToken) {
        try {
            URL url = new URL(DISCORD_OAUTH_TOKEN);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String body = "client_id=" + clientId +
                         "&client_secret=" + clientSecret +
                         "&grant_type=refresh_token" +
                         "&refresh_token=" + refreshToken;

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                debugLog("Token refresh failed with status: " + conn.getResponseCode());
                return null;
            }

            String response = readResponse(conn);
            JSONObject json = new JSONObject(response);

            return new DiscordToken(
                json.getString("access_token"),
                json.getString("refresh_token"),
                System.currentTimeMillis() + json.getLong("expires_in") * 1000
            );
        } catch (Exception e) {
            debugLog("Token refresh error: " + e.getMessage());
            return null;
        }
    }

    private String getValidAccessToken(String username) {
        TokenData tokenData = tokenCache.get(username.toLowerCase());
        if (tokenData == null) {
            return null;
        }

        DiscordToken token = tokenData.token;

        if (System.currentTimeMillis() >= token.expiresAt - 300000) {
            debugLog("Token expired or expiring soon for " + username + ", refreshing...");
            DiscordToken newToken = refreshAccessToken(token.refreshToken);
            if (newToken != null) {
                tokenCache.put(username.toLowerCase(), new TokenData(newToken, System.currentTimeMillis()));
                debugLog("Token refreshed successfully for " + username);
                return newToken.accessToken;
            } else {
                debugLog("Token refresh failed for " + username);
                tokenCache.remove(username.toLowerCase());
                return null;
            }
        }

        return token.accessToken;
    }

    private DiscordUser getUserInfo(String accessToken) throws Exception {
        URL url = new URL(DISCORD_API_BASE + "/users/@me");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (conn.getResponseCode() != 200) {
            debugLog("Get user info failed with status: " + conn.getResponseCode());
            return null;
        }

        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);

        return new DiscordUser(
            json.getString("id"),
            json.getString("username"),
            json.optString("discriminator", "0"),
            json.optString("avatar", null),
            json.optString("global_name", null)
        );
    }

    private boolean checkGuildMembership(String accessToken, String guildId) throws Exception {
        URL url = new URL(DISCORD_API_BASE + "/users/@me/guilds");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (conn.getResponseCode() != 200) {
            debugLog("Get guilds failed with status: " + conn.getResponseCode());
            return false;
        }

        String response = readResponse(conn);
        JSONArray guilds = new JSONArray(response);

        for (int i = 0; i < guilds.length(); i++) {
            JSONObject guild = guilds.getJSONObject(i);
            if (guildId.equals(guild.getString("id"))) {
                return true;
            }
        }

        return false;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    private String generateState() {
        byte[] bytes = new byte[16];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void debugLog(String msg) {
        if (debug) {
            configService.getLogger().info("[DEBUG] DiscordIntegrationService: " + msg);
        }
    }

    private static class StateData {
        final String username;
        final long timestamp;

        StateData(String username, long timestamp) {
            this.username = username;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > STATE_EXPIRY_MS;
        }
    }

    private static class TokenData {
        final DiscordToken token;
        final long cacheTime;

        TokenData(DiscordToken token, long cacheTime) {
            this.token = token;
            this.cacheTime = cacheTime;
        }
    }

    private static class DiscordToken {
        final String accessToken;
        final String refreshToken;
        final long expiresAt;

        DiscordToken(String accessToken, String refreshToken, long expiresAt) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAt = expiresAt;
        }
    }

    public static class DiscordUser {
        public final String id;
        public final String username;
        public final String discriminator;
        public final String avatar;
        public final String globalName;

        public DiscordUser(String id, String username, String discriminator, String avatar, String globalName) {
            this.id = id;
            this.username = username;
            this.discriminator = discriminator;
            this.avatar = avatar;
            this.globalName = globalName;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("id", id);
            if (username != null) json.put("username", username);
            json.put("discriminator", discriminator);
            if (avatar != null) json.put("avatar", avatar);
            if (globalName != null) json.put("global_name", globalName);
            return json;
        }
    }

    public static class DiscordCallbackResult {
        public final boolean success;
        public final String message;
        public final String username;
        public final DiscordUser user;

        public DiscordCallbackResult(boolean success, String message, String username, DiscordUser user) {
            this.success = success;
            this.message = message;
            this.username = username;
            this.user = user;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("success", success);
            json.put("message", message);
            if (username != null) json.put("username", username);
            if (user != null) json.put("user", user.toJson());
            return json;
        }
    }
}
