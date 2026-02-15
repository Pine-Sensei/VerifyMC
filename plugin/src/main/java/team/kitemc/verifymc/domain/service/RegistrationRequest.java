package team.kitemc.verifymc.domain.service;

import org.json.JSONObject;
import team.kitemc.verifymc.domain.model.UserStatus;

import java.util.function.BiFunction;

public final class RegistrationRequest {
    private final String uuid;
    private final String username;
    private final String normalizedUsername;
    private final String email;
    private final String password;
    private final String platform;
    private final String verificationCode;
    private final String captchaToken;
    private final String captchaAnswer;
    private final JSONObject questionnaire;
    private final JSONObject discordData;
    private final String language;

    private RegistrationRequest(Builder builder) {
        this.uuid = builder.uuid;
        this.username = builder.username;
        this.normalizedUsername = builder.normalizedUsername;
        this.email = builder.email;
        this.password = builder.password;
        this.platform = builder.platform;
        this.verificationCode = builder.verificationCode;
        this.captchaToken = builder.captchaToken;
        this.captchaAnswer = builder.captchaAnswer;
        this.questionnaire = builder.questionnaire;
        this.discordData = builder.discordData;
        this.language = builder.language;
    }

    public String getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getNormalizedUsername() {
        return normalizedUsername;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPlatform() {
        return platform;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public String getCaptchaToken() {
        return captchaToken;
    }

    public String getCaptchaAnswer() {
        return captchaAnswer;
    }

    public JSONObject getQuestionnaire() {
        return questionnaire;
    }

    public JSONObject getDiscordData() {
        return discordData;
    }

    public String getLanguage() {
        return language;
    }

    public static RegistrationRequest fromJson(JSONObject json, BiFunction<String, String, String> usernameNormalizer) {
        Builder builder = new Builder();
        builder.uuid(json.optString("uuid", ""));
        
        String username = json.optString("username", "");
        String platform = json.optString("platform", "java");
        builder.username(username);
        builder.platform(platform);
        
        if (usernameNormalizer != null) {
            builder.normalizedUsername(usernameNormalizer.apply(username, platform));
        } else {
            builder.normalizedUsername(username);
        }
        
        builder.email(json.optString("email", ""));
        builder.password(json.optString("password", ""));
        builder.verificationCode(json.optString("code", ""));
        builder.captchaToken(json.optString("captcha_token", ""));
        builder.captchaAnswer(json.optString("captcha_answer", ""));
        builder.questionnaire(json.optJSONObject("questionnaire"));
        builder.discordData(json.optJSONObject("discord"));
        builder.language(json.optString("language", "en"));
        
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String uuid;
        private String username;
        private String normalizedUsername;
        private String email;
        private String password;
        private String platform = "java";
        private String verificationCode = "";
        private String captchaToken = "";
        private String captchaAnswer = "";
        private JSONObject questionnaire;
        private JSONObject discordData;
        private String language = "en";

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder normalizedUsername(String normalizedUsername) {
            this.normalizedUsername = normalizedUsername;
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

        public Builder platform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder verificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
            return this;
        }

        public Builder captchaToken(String captchaToken) {
            this.captchaToken = captchaToken;
            return this;
        }

        public Builder captchaAnswer(String captchaAnswer) {
            this.captchaAnswer = captchaAnswer;
            return this;
        }

        public Builder questionnaire(JSONObject questionnaire) {
            this.questionnaire = questionnaire;
            return this;
        }

        public Builder discordData(JSONObject discordData) {
            this.discordData = discordData;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public RegistrationRequest build() {
            return new RegistrationRequest(this);
        }
    }
}
