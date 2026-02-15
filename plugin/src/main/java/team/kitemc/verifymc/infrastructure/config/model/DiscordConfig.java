package team.kitemc.verifymc.infrastructure.config.model;

import team.kitemc.verifymc.infrastructure.config.ConfigValidator;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

public final class DiscordConfig {

    private final boolean enabled;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String guildId;
    private final boolean required;

    private DiscordConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.redirectUri = builder.redirectUri;
        this.guildId = builder.guildId;
        this.required = builder.required;
    }

    public static DiscordConfig fromConfiguration(ConfigurationService config) {
        ConfigurationService discordConfig = config.getChild("discord");
        
        return new Builder()
            .enabled(discordConfig.getBoolean("enabled", false))
            .clientId(discordConfig.getString("client_id", ""))
            .clientSecret(discordConfig.getString("client_secret", ""))
            .redirectUri(discordConfig.getString("redirect_uri", ""))
            .guildId(discordConfig.getString("guild_id", ""))
            .required(discordConfig.getBoolean("required", false))
            .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getGuildId() {
        return guildId;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
            && clientSecret != null && !clientSecret.isEmpty()
            && redirectUri != null && !redirectUri.isEmpty();
    }

    public boolean isFullyConfigured() {
        return isConfigured() && guildId != null && !guildId.isEmpty();
    }

    public void validate(ConfigValidator validator) {
        if (!enabled) {
            return;
        }
        
        validator.requireNonEmpty("discord.client_id", clientId)
                 .requireNonEmpty("discord.client_secret", clientSecret)
                 .requireNonEmpty("discord.redirect_uri", redirectUri)
                 .validateUrl("discord.redirect_uri", redirectUri);
        
        if (required) {
            validator.requireNonEmpty("discord.guild_id", guildId);
        }
    }

    public void validateIfEnabled(ConfigValidator validator) {
        if (enabled) {
            validate(validator);
        }
    }

    public static class Builder {
        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";
        private String guildId = "";
        private boolean required = false;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public Builder guildId(String guildId) {
            this.guildId = guildId;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public DiscordConfig build() {
            return new DiscordConfig(this);
        }
    }
}
