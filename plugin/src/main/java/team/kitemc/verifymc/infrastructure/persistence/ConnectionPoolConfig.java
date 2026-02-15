package team.kitemc.verifymc.infrastructure.persistence;

import com.zaxxer.hikari.HikariConfig;

public class ConnectionPoolConfig {
    private String jdbcUrl;
    private String username;
    private String password;
    private int maximumPoolSize = 10;
    private int minimumIdle = 2;
    private long connectionTimeout = 30000;
    private long idleTimeout = 300000;
    private long maxLifetime = 1800000;
    private String poolName = "VerifyMC-Pool";
    private String connectionTestQuery = "SELECT 1";
    private boolean autoCommit = true;
    private boolean readOnly = false;

    public ConnectionPoolConfig() {
    }

    public static ConnectionPoolConfig fromMySqlConfig(String host, int port, String database, String user, String password) {
        ConnectionPoolConfig config = new ConnectionPoolConfig();
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&characterEncoding=utf8&serverTimezone=UTC";
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        return config;
    }

    public HikariConfig toHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(this.jdbcUrl);
        config.setUsername(this.username);
        config.setPassword(this.password);
        config.setMaximumPoolSize(this.maximumPoolSize);
        config.setMinimumIdle(this.minimumIdle);
        config.setConnectionTimeout(this.connectionTimeout);
        config.setIdleTimeout(this.idleTimeout);
        config.setMaxLifetime(this.maxLifetime);
        config.setPoolName(this.poolName);
        config.setConnectionTestQuery(this.connectionTestQuery);
        config.setAutoCommit(this.autoCommit);
        config.setReadOnly(this.readOnly);
        return config;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public void setMinimumIdle(int minimumIdle) {
        this.minimumIdle = minimumIdle;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getConnectionTestQuery() {
        return connectionTestQuery;
    }

    public void setConnectionTestQuery(String connectionTestQuery) {
        this.connectionTestQuery = connectionTestQuery;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public static class Builder {
        private final ConnectionPoolConfig config = new ConnectionPoolConfig();

        public Builder jdbcUrl(String jdbcUrl) {
            config.setJdbcUrl(jdbcUrl);
            return this;
        }

        public Builder username(String username) {
            config.setUsername(username);
            return this;
        }

        public Builder password(String password) {
            config.setPassword(password);
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            config.setMaximumPoolSize(maximumPoolSize);
            return this;
        }

        public Builder minimumIdle(int minimumIdle) {
            config.setMinimumIdle(minimumIdle);
            return this;
        }

        public Builder connectionTimeout(long connectionTimeout) {
            config.setConnectionTimeout(connectionTimeout);
            return this;
        }

        public Builder idleTimeout(long idleTimeout) {
            config.setIdleTimeout(idleTimeout);
            return this;
        }

        public Builder maxLifetime(long maxLifetime) {
            config.setMaxLifetime(maxLifetime);
            return this;
        }

        public Builder poolName(String poolName) {
            config.setPoolName(poolName);
            return this;
        }

        public Builder connectionTestQuery(String connectionTestQuery) {
            config.setConnectionTestQuery(connectionTestQuery);
            return this;
        }

        public Builder autoCommit(boolean autoCommit) {
            config.setAutoCommit(autoCommit);
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            config.setReadOnly(readOnly);
            return this;
        }

        public ConnectionPoolConfig build() {
            if (config.getJdbcUrl() == null || config.getJdbcUrl().isEmpty()) {
                throw new IllegalStateException("JDBC URL is required");
            }
            if (config.getUsername() == null) {
                throw new IllegalStateException("Username is required");
            }
            return config;
        }
    }
}
