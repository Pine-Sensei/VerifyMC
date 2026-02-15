package team.kitemc.verifymc.infrastructure.config.model;

import team.kitemc.verifymc.infrastructure.config.ConfigValidator;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

public final class QuestionnaireConfig {

    private final boolean enabled;
    private final int passScore;
    private final RateLimitConfig rateLimit;

    public static final class RateLimitConfig {
        private final long windowMs;
        private final int ipMax;
        private final int uuidMax;
        private final int emailMax;

        public RateLimitConfig(long windowMs, int ipMax, int uuidMax, int emailMax) {
            this.windowMs = windowMs;
            this.ipMax = ipMax;
            this.uuidMax = uuidMax;
            this.emailMax = emailMax;
        }

        public long getWindowMs() { return windowMs; }
        public int getIpMax() { return ipMax; }
        public int getUuidMax() { return uuidMax; }
        public int getEmailMax() { return emailMax; }

        public void validate(ConfigValidator validator) {
            validator.validatePositive("questionnaire.rate_limit.window_ms", windowMs)
                     .validatePositive("questionnaire.rate_limit.ip.max", ipMax)
                     .validatePositive("questionnaire.rate_limit.uuid.max", uuidMax)
                     .validatePositive("questionnaire.rate_limit.email.max", emailMax);
        }
    }

    private QuestionnaireConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.passScore = builder.passScore;
        this.rateLimit = builder.rateLimit;
    }

    public static QuestionnaireConfig fromConfiguration(ConfigurationService config) {
        ConfigurationService questionnaireConfig = config.getChild("questionnaire");
        
        boolean enabled = questionnaireConfig.getBoolean("enabled", false);
        int passScore = questionnaireConfig.getInt("pass_score", 60);
        
        ConfigurationService rateLimitConfig = questionnaireConfig.getChild("rate_limit");
        RateLimitConfig rateLimit = new RateLimitConfig(
            rateLimitConfig.getLong("window_ms", 300000),
            rateLimitConfig.getChild("ip").getInt("max", 20),
            rateLimitConfig.getChild("uuid").getInt("max", 8),
            rateLimitConfig.getChild("email").getInt("max", 6)
        );
        
        return new Builder()
            .enabled(enabled)
            .passScore(passScore)
            .rateLimit(rateLimit)
            .build();
    }

    public boolean isEnabled() { return enabled; }
    public int getPassScore() { return passScore; }
    public RateLimitConfig getRateLimit() { return rateLimit; }

    public void validate(ConfigValidator validator) {
        if (!enabled) {
            return;
        }
        
        validator.validateRange("questionnaire.pass_score", passScore, 0, 100);
        
        if (rateLimit != null) {
            rateLimit.validate(validator);
        }
    }

    public void validateIfEnabled(ConfigValidator validator) {
        if (enabled) {
            validate(validator);
        }
    }

    public static class Builder {
        private boolean enabled = false;
        private int passScore = 60;
        private RateLimitConfig rateLimit;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder passScore(int passScore) {
            this.passScore = passScore;
            return this;
        }

        public Builder rateLimit(RateLimitConfig rateLimit) {
            this.rateLimit = rateLimit;
            return this;
        }

        public QuestionnaireConfig build() {
            return new QuestionnaireConfig(this);
        }
    }
}
