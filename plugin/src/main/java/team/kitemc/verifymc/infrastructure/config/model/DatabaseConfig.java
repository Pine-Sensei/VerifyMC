package team.kitemc.verifymc.infrastructure.config.model;

import team.kitemc.verifymc.infrastructure.config.ConfigValidator;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import java.util.Collections;
import java.util.List;

public final class DatabaseConfig {

    public enum StorageType {
        DATA("data"),
        MYSQL("mysql"),
        SQLITE("sqlite");

        private final String value;

        StorageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static StorageType fromString(String value) {
            for (StorageType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return DATA;
        }
    }

    private final StorageType type;
    private final boolean autoMigrateOnSwitch;
    private final MySQLConfig mySqlConfig;

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

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getDatabase() {
            return database;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public String getJdbcUrl() {
            return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true", host, port, database);
        }

        public void validate(ConfigValidator validator) {
            validator.requireNonEmpty("storage.mysql.host", host)
                     .validatePort("storage.mysql.port", port)
                     .requireNonEmpty("storage.mysql.database", database)
                     .requireNonEmpty("storage.mysql.user", user);
        }
    }

    private DatabaseConfig(Builder builder) {
        this.type = builder.type;
        this.autoMigrateOnSwitch = builder.autoMigrateOnSwitch;
        this.mySqlConfig = builder.mySqlConfig;
    }

    public static DatabaseConfig fromConfiguration(ConfigurationService config) {
        ConfigurationService storageConfig = config.getChild("storage");
        
        StorageType type = StorageType.fromString(storageConfig.getString("type", "data"));
        boolean autoMigrate = storageConfig.getBoolean("auto_migrate_on_switch", false);
        
        ConfigurationService mysqlConfig = storageConfig.getChild("mysql");
        MySQLConfig mySqlConfig = new MySQLConfig(
            mysqlConfig.getString("host", "localhost"),
            mysqlConfig.getInt("port", 3306),
            mysqlConfig.getString("database", "verifymc"),
            mysqlConfig.getString("user", "root"),
            mysqlConfig.getString("password", "")
        );
        
        return new Builder()
            .type(type)
            .autoMigrateOnSwitch(autoMigrate)
            .mySqlConfig(mySqlConfig)
            .build();
    }

    public StorageType getType() {
        return type;
    }

    public boolean isAutoMigrateOnSwitch() {
        return autoMigrateOnSwitch;
    }

    public MySQLConfig getMySqlConfig() {
        return mySqlConfig;
    }

    public boolean isMySql() {
        return type == StorageType.MYSQL;
    }

    public boolean isData() {
        return type == StorageType.DATA;
    }

    public void validate(ConfigValidator validator) {
        validator.require("storage.type", type);
        
        if (type == StorageType.MYSQL && mySqlConfig != null) {
            mySqlConfig.validate(validator);
        }
    }

    public static class Builder {
        private StorageType type = StorageType.DATA;
        private boolean autoMigrateOnSwitch = false;
        private MySQLConfig mySqlConfig;

        public Builder type(StorageType type) {
            this.type = type;
            return this;
        }

        public Builder autoMigrateOnSwitch(boolean autoMigrateOnSwitch) {
            this.autoMigrateOnSwitch = autoMigrateOnSwitch;
            return this;
        }

        public Builder mySqlConfig(MySQLConfig mySqlConfig) {
            this.mySqlConfig = mySqlConfig;
            return this;
        }

        public DatabaseConfig build() {
            return new DatabaseConfig(this);
        }
    }
}
