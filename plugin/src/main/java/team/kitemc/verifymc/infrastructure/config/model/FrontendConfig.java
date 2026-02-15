package team.kitemc.verifymc.infrastructure.config.model;

import team.kitemc.verifymc.infrastructure.config.ConfigValidator;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

public final class FrontendConfig {

    private final String theme;
    private final String logoUrl;
    private final String announcement;

    private FrontendConfig(Builder builder) {
        this.theme = builder.theme;
        this.logoUrl = builder.logoUrl;
        this.announcement = builder.announcement;
    }

    public static FrontendConfig fromConfiguration(ConfigurationService config) {
        ConfigurationService frontendConfig = config.getChild("frontend");
        
        return new Builder()
            .theme(frontendConfig.getString("theme", "glassx"))
            .logoUrl(frontendConfig.getString("logo_url", "/logo.png"))
            .announcement(frontendConfig.getString("announcement", ""))
            .build();
    }

    public String getTheme() {
        return theme;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getAnnouncement() {
        return announcement;
    }

    public void validate(ConfigValidator validator) {
        validator.requireNonEmpty("frontend.theme", theme)
                 .requireNonEmpty("frontend.logo_url", logoUrl);
    }

    public static class Builder {
        private String theme = "glassx";
        private String logoUrl = "/logo.png";
        private String announcement = "";

        public Builder theme(String theme) {
            this.theme = theme;
            return this;
        }

        public Builder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public Builder announcement(String announcement) {
            this.announcement = announcement;
            return this;
        }

        public FrontendConfig build() {
            return new FrontendConfig(this);
        }
    }
}
