package team.kitemc.verifymc.infrastructure.config.model;

import team.kitemc.verifymc.infrastructure.config.ConfigValidator;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

public final class SmtpConfig {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final boolean enableSsl;

    private SmtpConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.from = builder.from;
        this.enableSsl = builder.enableSsl;
    }

    public static SmtpConfig fromConfiguration(ConfigurationService config) {
        ConfigurationService smtpConfig = config.getChild("smtp");
        
        return new Builder()
            .host(smtpConfig.getString("host", "smtp.qq.com"))
            .port(smtpConfig.getInt("port", 587))
            .username(smtpConfig.getString("username", ""))
            .password(smtpConfig.getString("password", ""))
            .from(smtpConfig.getString("from", ""))
            .enableSsl(smtpConfig.getBoolean("enable_ssl", true))
            .build();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFrom() {
        return from;
    }

    public boolean isEnableSsl() {
        return enableSsl;
    }

    public boolean isConfigured() {
        return host != null && !host.isEmpty()
            && username != null && !username.isEmpty()
            && password != null && !password.isEmpty()
            && from != null && !from.isEmpty();
    }

    public void validate(ConfigValidator validator) {
        validator.requireNonEmpty("smtp.host", host)
                 .validatePort("smtp.port", port)
                 .requireNonEmpty("smtp.username", username)
                 .requireNonEmpty("smtp.password", password)
                 .requireNonEmpty("smtp.from", from)
                 .validateEmail("smtp.from", from);
    }

    public void validateIfEnabled(ConfigValidator validator) {
        if (isConfigured()) {
            validate(validator);
        }
    }

    public static class Builder {
        private String host = "smtp.qq.com";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String from = "";
        private boolean enableSsl = true;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder enableSsl(boolean enableSsl) {
            this.enableSsl = enableSsl;
            return this;
        }

        public SmtpConfig build() {
            return new SmtpConfig(this);
        }
    }
}
