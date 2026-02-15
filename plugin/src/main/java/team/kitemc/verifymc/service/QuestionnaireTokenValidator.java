package team.kitemc.verifymc.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestionnaireTokenValidator {
    private static final Gson GSON = new GsonBuilder().create();
    private static final long TOKEN_EXPIRY_MS = 30 * 60 * 1000;
    
    private final Map<String, QuestionnaireResult> tokenStore = new ConcurrentHashMap<>();
    
    public static class QuestionnaireResult {
        String token;
        String answersJson;
        int score;
        boolean passed;
        long createdAt;
        
        public QuestionnaireResult(String token, String answersJson, int score, boolean passed) {
            this.token = token;
            this.answersJson = answersJson;
            this.score = score;
            this.passed = passed;
            this.createdAt = System.currentTimeMillis();
        }
        
        public String getToken() {
            return token;
        }
        
        public String getAnswersJson() {
            return answersJson;
        }
        
        public int getScore() {
            return score;
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > TOKEN_EXPIRY_MS;
        }
    }
    
    public String storeResult(String answersJson, int score, boolean passed) {
        String token = generateToken();
        tokenStore.put(token, new QuestionnaireResult(token, answersJson, score, passed));
        return token;
    }
    
    public QuestionnaireResult validateAndGet(String token, String currentAnswersJson) {
        QuestionnaireResult result = tokenStore.get(token);
        if (result == null) {
            return null;
        }
        if (result.isExpired()) {
            tokenStore.remove(token);
            return null;
        }
        if (!normalizeJson(result.answersJson).equals(normalizeJson(currentAnswersJson))) {
            return null;
        }
        return result;
    }
    
    public QuestionnaireResult getResult(String token) {
        QuestionnaireResult result = tokenStore.get(token);
        if (result == null || result.isExpired()) {
            tokenStore.remove(token);
            return null;
        }
        return result;
    }
    
    public void removeToken(String token) {
        tokenStore.remove(token);
    }
    
    private String normalizeJson(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return GSON.toJson(element);
        } catch (Exception e) {
            return json;
        }
    }
    
    private String generateToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    public void cleanup() {
        tokenStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    public int getActiveTokenCount() {
        return tokenStore.size();
    }
}
