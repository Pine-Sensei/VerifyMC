package team.kitemc.verifymc.infrastructure.config.model;

import team.kitemc.verifymc.infrastructure.config.ConfigValidator;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AuthMeConfig {

    public enum DatabaseType {
        SQLITE("sqlite"),
        MYSQL("mysql");

        private final String value;

        DatabaseType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DatabaseType fromString(String value) {
            for (DatabaseType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return SQLITE;
        }
    }

    private final boolean enabled;
    private final boolean requirePassword;
    private final String passwordRegex;
    private final DatabaseType databaseType;
    private final String tableName;
    private final int syncIntervalSeconds;
    private final int saltLength;
    private final MySQLConfig mySqlConfig;
    private final SQLiteConfig sqliteConfig;
    private final ColumnConfig columnConfig;

    public static final class MySQLConfig {
        private final String host;
        private final int port;
        private final String database;
        private final String user;
        private final String password;

        public MySQLConfig(String host, int port, String database, String user, String password) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.user = user;
            this.password = password;
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDatabase() { return database; }
        public String getUser() { return user; }
        public String getPassword() { return password; }

        public String getJdbcUrl() {
            return String.format("jdbc:mysql://%s:%d/%s?useSSL=false", host, port, database);
        }

        public void validate(ConfigValidator validator) {
            validator.requireNonEmpty("authme.database.mysql.host", host)
                     .validatePort("authme.database.mysql.port", port)
                     .requireNonEmpty("authme.database.mysql.database", database)
                     .requireNonEmpty("authme.database.mysql.user", user);
        }
    }

    public static final class SQLiteConfig {
        private final String path;

        public SQLiteConfig(String path) {
            this.path = path;
        }

        public String getPath() { return path; }

        public void validate(ConfigValidator validator) {
            validator.requireNonEmpty("authme.database.sqlite.path", path);
        }
    }

    public static final class ColumnConfig {
        private final Map<String, String> columns;

        public ColumnConfig(Map<String, String> columns) {
            this.columns = Collections.unmodifiableMap(new HashMap<>(columns));
        }

        public String getIdColumn() { return columns.getOrDefault("mySQLColumnId", "id"); }
        public String getNameColumn() { return columns.getOrDefault("mySQLColumnName", "username"); }
        public String getRealNameColumn() { return columns.getOrDefault("mySQLRealName", "realname"); }
        public String getPasswordColumn() { return columns.getOrDefault("mySQLColumnPassword", "password"); }
        public String getSaltColumn() { return columns.getOrDefault("mySQLColumnSalt", ""); }
        public String getEmailColumn() { return columns.getOrDefault("mySQLColumnEmail", "email"); }
        public String getLoggedColumn() { return columns.getOrDefault("mySQLColumnLogged", "isLogged"); }
        public String getSessionColumn() { return columns.getOrDefault("mySQLColumnHasSession", "hasSession"); }
        public String getIpColumn() { return columns.getOrDefault("mySQLColumnIp", "ip"); }
        public String getLastLoginColumn() { return columns.getOrDefault("mySQLColumnLastLogin", "lastlogin"); }
        public String getRegisterDateColumn() { return columns.getOrDefault("mySQLColumnRegisterDate", "regdate"); }
        public String getRegisterIpColumn() { return columns.getOrDefault("mySQLColumnRegisterIp", "regip"); }
        public String getXColumn() { return columns.getOrDefault("mySQLlastlocX", "x"); }
        public String getYColumn() { return columns.getOrDefault("mySQLlastlocY", "y"); }
        public String getZColumn() { return columns.getOrDefault("mySQLlastlocZ", "z"); }
        public String getWorldColumn() { return columns.getOrDefault("mySQLlastlocWorld", "world"); }
        public String getYawColumn() { return columns.getOrDefault("mySQLlastlocYaw", "yaw"); }
        public String getPitchColumn() { return columns.getOrDefault("mySQLlastlocPitch", "pitch"); }
        public String getUuidColumn() { return columns.getOrDefault("mySQLPlayerUUID", ""); }

        public Map<String, String> getAllColumns() {
            return columns;
        }
    }

    private AuthMeConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.requirePassword = builder.requirePassword;
        this.passwordRegex = builder.passwordRegex;
        this.databaseType = builder.databaseType;
        this.tableName = builder.tableName;
        this.syncIntervalSeconds = builder.syncIntervalSeconds;
        this.saltLength = builder.saltLength;
        this.mySqlConfig = builder.mySqlConfig;
        this.sqliteConfig = builder.sqliteConfig;
        this.columnConfig = builder.columnConfig;
    }

    public static AuthMeConfig fromConfiguration(ConfigurationService config) {
        ConfigurationService authmeConfig = config.getChild("authme");
        
        boolean enabled = authmeConfig.getBoolean("enabled", true);
        boolean requirePassword = authmeConfig.getBoolean("require_password", true);
        String passwordRegex = authmeConfig.getString("password_regex", "^[a-zA-Z0-9_]{8,26}$");
        
        ConfigurationService dbConfig = authmeConfig.getChild("database");
        DatabaseType dbType = DatabaseType.fromString(dbConfig.getString("type", "sqlite"));
        String tableName = dbConfig.getString("table", "authme");
        int syncInterval = dbConfig.getInt("sync_interval_seconds", 30);
        int saltLength = dbConfig.getInt("salt_length", 16);
        
        ConfigurationService mysqlConfig = dbConfig.getChild("mysql");
        MySQLConfig mySqlConfig = new MySQLConfig(
            mysqlConfig.getString("host", "127.0.0.1"),
            mysqlConfig.getInt("port", 3306),
            mysqlConfig.getString("database", "authme"),
            mysqlConfig.getString("user", "root"),
            mysqlConfig.getString("password", "")
        );
        
        ConfigurationService sqliteConfig = dbConfig.getChild("sqlite");
        SQLiteConfig sqliteConf = new SQLiteConfig(
            sqliteConfig.getString("path", "plugins/AuthMe/authme.db")
        );
        
        ConfigurationService columnsConfig = dbConfig.getChild("columns");
        Map<String, String> columns = new HashMap<>();
        for (String key : columnsConfig.getKeys("")) {
            columns.put(key, columnsConfig.getString(key, ""));
        }
        ColumnConfig columnConfig = new ColumnConfig(columns);
        
        return new Builder()
            .enabled(enabled)
            .requirePassword(requirePassword)
            .passwordRegex(passwordRegex)
            .databaseType(dbType)
            .tableName(tableName)
            .syncIntervalSeconds(syncInterval)
            .saltLength(saltLength)
            .mySqlConfig(mySqlConfig)
            .sqliteConfig(sqliteConf)
            .columnConfig(columnConfig)
            .build();
    }

    public boolean isEnabled() { return enabled; }
    public boolean isRequirePassword() { return requirePassword; }
    public String getPasswordRegex() { return passwordRegex; }
    public DatabaseType getDatabaseType() { return databaseType; }
    public String getTableName() { return tableName; }
    public int getSyncIntervalSeconds() { return syncIntervalSeconds; }
    public int getSaltLength() { return saltLength; }
    public MySQLConfig getMySqlConfig() { return mySqlConfig; }
    public SQLiteConfig getSqliteConfig() { return sqliteConfig; }
    public ColumnConfig getColumnConfig() { return columnConfig; }

    public boolean isMySql() { return databaseType == DatabaseType.MYSQL; }
    public boolean isSqlite() { return databaseType == DatabaseType.SQLITE; }

    public void validate(ConfigValidator validator) {
        if (!enabled) {
            return;
        }
        
        validator.requireNonEmpty("authme.password_regex", passwordRegex);
        
        try {
            java.util.regex.Pattern.compile(passwordRegex);
        } catch (java.util.regex.PatternSyntaxException e) {
            validator.custom("authme.password_regex", false, "Invalid regex pattern: " + e.getMessage());
        }
        
        validator.validatePositive("authme.sync_interval_seconds", syncIntervalSeconds);
        validator.validateRange("authme.salt_length", saltLength, 1, 64);
        
        if (isMySql() && mySqlConfig != null) {
            mySqlConfig.validate(validator);
        } else if (isSqlite() && sqliteConfig != null) {
            sqliteConfig.validate(validator);
        }
    }

    public static class Builder {
        private boolean enabled = true;
        private boolean requirePassword = true;
        private String passwordRegex = "^[a-zA-Z0-9_]{8,26}$";
        private DatabaseType databaseType = DatabaseType.SQLITE;
        private String tableName = "authme";
        private int syncIntervalSeconds = 30;
        private int saltLength = 16;
        private MySQLConfig mySqlConfig;
        private SQLiteConfig sqliteConfig;
        private ColumnConfig columnConfig;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder requirePassword(boolean requirePassword) {
            this.requirePassword = requirePassword;
            return this;
        }

        public Builder passwordRegex(String passwordRegex) {
            this.passwordRegex = passwordRegex;
            return this;
        }

        public Builder databaseType(DatabaseType databaseType) {
            this.databaseType = databaseType;
            return this;
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder syncIntervalSeconds(int syncIntervalSeconds) {
            this.syncIntervalSeconds = syncIntervalSeconds;
            return this;
        }

        public Builder saltLength(int saltLength) {
            this.saltLength = saltLength;
            return this;
        }

        public Builder mySqlConfig(MySQLConfig mySqlConfig) {
            this.mySqlConfig = mySqlConfig;
            return this;
        }

        public Builder sqliteConfig(SQLiteConfig sqliteConfig) {
            this.sqliteConfig = sqliteConfig;
            return this;
        }

        public Builder columnConfig(ColumnConfig columnConfig) {
            this.columnConfig = columnConfig;
            return this;
        }

        public AuthMeConfig build() {
            return new AuthMeConfig(this);
        }
    }
}
