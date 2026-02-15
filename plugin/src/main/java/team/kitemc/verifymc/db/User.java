package team.kitemc.verifymc.db;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uuid;
    private String username;
    private String email;
    private String password;
    private String status;
    private String discordId;
    private Long createdAt;
    private Long updatedAt;
    private Integer questionnaireScore;
    private Boolean questionnairePassed;
    private String questionnaireReviewSummary;
    private Long questionnaireScoredAt;
    
    public User() {}
    
    public User(String uuid, String username, String email, String status) {
        this.uuid = uuid;
        this.username = username;
        this.email = email;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }
    
    public static User fromMap(Map<String, Object> map) {
        if (map == null) return null;
        User user = new User();
        user.setUuid((String) map.get("uuid"));
        user.setUsername((String) map.get("username"));
        user.setEmail((String) map.get("email"));
        user.setPassword((String) map.get("password"));
        user.setStatus((String) map.get("status"));
        user.setDiscordId((String) map.get("discord_id"));
        
        Object createdAt = map.get("created_at");
        if (createdAt instanceof Long) {
            user.setCreatedAt((Long) createdAt);
        } else if (createdAt instanceof Integer) {
            user.setCreatedAt(((Integer) createdAt).longValue());
        }
        
        Object updatedAt = map.get("updated_at");
        if (updatedAt instanceof Long) {
            user.setUpdatedAt((Long) updatedAt);
        } else if (updatedAt instanceof Integer) {
            user.setUpdatedAt(((Integer) updatedAt).longValue());
        }
        
        Object score = map.get("questionnaire_score");
        if (score instanceof Integer) {
            user.setQuestionnaireScore((Integer) score);
        }
        
        Object passed = map.get("questionnaire_passed");
        if (passed instanceof Boolean) {
            user.setQuestionnairePassed((Boolean) passed);
        }
        
        user.setQuestionnaireReviewSummary((String) map.get("questionnaire_review_summary"));
        
        Object scoredAt = map.get("questionnaire_scored_at");
        if (scoredAt instanceof Long) {
            user.setQuestionnaireScoredAt((Long) scoredAt);
        } else if (scoredAt instanceof Integer) {
            user.setQuestionnaireScoredAt(((Integer) scoredAt).longValue());
        }
        
        return user;
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (uuid != null) map.put("uuid", uuid);
        if (username != null) map.put("username", username);
        if (email != null) map.put("email", email);
        if (password != null) map.put("password", password);
        if (status != null) map.put("status", status);
        if (discordId != null) map.put("discord_id", discordId);
        if (createdAt != null) map.put("created_at", createdAt);
        if (updatedAt != null) map.put("updated_at", updatedAt);
        if (questionnaireScore != null) map.put("questionnaire_score", questionnaireScore);
        if (questionnairePassed != null) map.put("questionnaire_passed", questionnairePassed);
        if (questionnaireReviewSummary != null) map.put("questionnaire_review_summary", questionnaireReviewSummary);
        if (questionnaireScoredAt != null) map.put("questionnaire_scored_at", questionnaireScoredAt);
        return map;
    }
    
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDiscordId() { return discordId; }
    public void setDiscordId(String discordId) { this.discordId = discordId; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getQuestionnaireScore() { return questionnaireScore; }
    public void setQuestionnaireScore(Integer questionnaireScore) { this.questionnaireScore = questionnaireScore; }
    
    public Boolean getQuestionnairePassed() { return questionnairePassed; }
    public void setQuestionnairePassed(Boolean questionnairePassed) { this.questionnairePassed = questionnairePassed; }
    
    public String getQuestionnaireReviewSummary() { return questionnaireReviewSummary; }
    public void setQuestionnaireReviewSummary(String questionnaireReviewSummary) { this.questionnaireReviewSummary = questionnaireReviewSummary; }
    
    public Long getQuestionnaireScoredAt() { return questionnaireScoredAt; }
    public void setQuestionnaireScoredAt(Long questionnaireScoredAt) { this.questionnaireScoredAt = questionnaireScoredAt; }
    
    public boolean isApproved() {
        return "approved".equalsIgnoreCase(status);
    }
    
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }
    
    public boolean isBanned() {
        return "banned".equalsIgnoreCase(status);
    }
}
