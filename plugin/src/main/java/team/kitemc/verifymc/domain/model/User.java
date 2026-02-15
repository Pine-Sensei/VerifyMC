package team.kitemc.verifymc.domain.model;

import java.util.HashMap;
import java.util.Map;

public final class User {
    private final String uuid;
    private final String username;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final long regTime;
    private final String discordId;
    private final Integer questionnaireScore;
    private final Boolean questionnairePassed;
    private final String questionnaireReviewSummary;
    private final Long questionnaireScoredAt;

    private User(Builder builder) {
        this.uuid = builder.uuid;
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.status = builder.status;
        this.regTime = builder.regTime;
        this.discordId = builder.discordId;
        this.questionnaireScore = builder.questionnaireScore;
        this.questionnairePassed = builder.questionnairePassed;
        this.questionnaireReviewSummary = builder.questionnaireReviewSummary;
        this.questionnaireScoredAt = builder.questionnaireScoredAt;
    }

    public String getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserStatus getStatus() {
        return status;
    }

    public long getRegTime() {
        return regTime;
    }

    public String getDiscordId() {
        return discordId;
    }

    public Integer getQuestionnaireScore() {
        return questionnaireScore;
    }

    public Boolean getQuestionnairePassed() {
        return questionnairePassed;
    }

    public String getQuestionnaireReviewSummary() {
        return questionnaireReviewSummary;
    }

    public Long getQuestionnaireScoredAt() {
        return questionnaireScoredAt;
    }

    public boolean isApproved() {
        return status == UserStatus.APPROVED;
    }

    public boolean isPending() {
        return status == UserStatus.PENDING;
    }

    public boolean isBanned() {
        return status == UserStatus.BANNED;
    }

    public boolean isRejected() {
        return status == UserStatus.REJECTED;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("username", username);
        map.put("email", email);
        map.put("password", password);
        map.put("status", status.getValue());
        map.put("regTime", regTime);
        map.put("discord_id", discordId);
        map.put("questionnaire_score", questionnaireScore);
        map.put("questionnaire_passed", questionnairePassed);
        map.put("questionnaire_review_summary", questionnaireReviewSummary);
        map.put("questionnaire_scored_at", questionnaireScoredAt);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static User fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Builder builder = new Builder();
        builder.uuid((String) map.get("uuid"));
        builder.username((String) map.get("username"));
        builder.email((String) map.get("email"));
        builder.password((String) map.get("password"));
        builder.status(UserStatus.fromString((String) map.get("status")));
        builder.regTime(getLongValue(map.get("regTime")));
        builder.discordId((String) map.get("discord_id"));
        builder.questionnaireScore(getIntegerValue(map.get("questionnaire_score")));
        builder.questionnairePassed(getBooleanValue(map.get("questionnaire_passed")));
        builder.questionnaireReviewSummary((String) map.get("questionnaire_review_summary"));
        builder.questionnaireScoredAt(getLongValue(map.get("questionnaire_scored_at")));
        return builder.build();
    }

    private static long getLongValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static Integer getIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean getBooleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    public Builder toBuilder() {
        return new Builder()
                .uuid(this.uuid)
                .username(this.username)
                .email(this.email)
                .password(this.password)
                .status(this.status)
                .regTime(this.regTime)
                .discordId(this.discordId)
                .questionnaireScore(this.questionnaireScore)
                .questionnairePassed(this.questionnairePassed)
                .questionnaireReviewSummary(this.questionnaireReviewSummary)
                .questionnaireScoredAt(this.questionnaireScoredAt);
    }

    public static class Builder {
        private String uuid;
        private String username;
        private String email;
        private String password;
        private UserStatus status = UserStatus.PENDING;
        private long regTime = System.currentTimeMillis();
        private String discordId;
        private Integer questionnaireScore;
        private Boolean questionnairePassed;
        private String questionnaireReviewSummary;
        private Long questionnaireScoredAt;

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder regTime(long regTime) {
            this.regTime = regTime;
            return this;
        }

        public Builder discordId(String discordId) {
            this.discordId = discordId;
            return this;
        }

        public Builder questionnaireScore(Integer questionnaireScore) {
            this.questionnaireScore = questionnaireScore;
            return this;
        }

        public Builder questionnairePassed(Boolean questionnairePassed) {
            this.questionnairePassed = questionnairePassed;
            return this;
        }

        public Builder questionnaireReviewSummary(String questionnaireReviewSummary) {
            this.questionnaireReviewSummary = questionnaireReviewSummary;
            return this;
        }

        public Builder questionnaireScoredAt(Long questionnaireScoredAt) {
            this.questionnaireScoredAt = questionnaireScoredAt;
            return this;
        }

        public User build() {
            if (uuid == null || uuid.isEmpty()) {
                throw new IllegalStateException("UUID is required");
            }
            if (username == null || username.isEmpty()) {
                throw new IllegalStateException("Username is required");
            }
            if (email == null || email.isEmpty()) {
                throw new IllegalStateException("Email is required");
            }
            return new User(this);
        }
    }
}
