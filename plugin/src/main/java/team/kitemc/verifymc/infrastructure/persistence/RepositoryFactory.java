package team.kitemc.verifymc.infrastructure.persistence;

import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.domain.repository.AuditRepository;
import team.kitemc.verifymc.domain.repository.UserRepository;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import java.io.File;
import java.util.Properties;

public class RepositoryFactory {
    private final ConfigurationService config;
    private final Plugin plugin;
    private volatile UserRepository userRepository;
    private volatile AuditRepository auditRepository;
    private volatile MysqlUserRepository mysqlUserRepository;
    private final Object lock = new Object();

    public RepositoryFactory(ConfigurationService config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    public UserRepository getUserRepository() {
        if (userRepository == null) {
            synchronized (lock) {
                if (userRepository == null) {
                    userRepository = createUserRepository();
                }
            }
        }
        return userRepository;
    }

    public AuditRepository getAuditRepository() {
        if (auditRepository == null) {
            synchronized (lock) {
                if (auditRepository == null) {
                    auditRepository = createAuditRepository();
                }
            }
        }
        return auditRepository;
    }

    private UserRepository createUserRepository() {
        String storageType = config.getString("storage.type", "data");
        plugin.getLogger().info("Initializing user repository with storage type: " + storageType);

        if ("mysql".equalsIgnoreCase(storageType)) {
            return createMysqlUserRepository();
        } else {
            return createFileUserRepository();
        }
    }

    private UserRepository createFileUserRepository() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dataFile = new File(dataFolder, "data.json");
        plugin.getLogger().info("Creating FileUserRepository with file: " + dataFile.getAbsolutePath());
        return new FileUserRepository(dataFile, plugin);
    }

    private UserRepository createMysqlUserRepository() {
        Properties mysqlConfig = getMysqlConfig();
        plugin.getLogger().info("Creating MysqlUserRepository with host: " + 
                mysqlConfig.getProperty("host") + ":" + mysqlConfig.getProperty("port"));
        mysqlUserRepository = new MysqlUserRepository(mysqlConfig, plugin);
        return mysqlUserRepository;
    }

    private AuditRepository createAuditRepository() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File auditFile = new File(dataFolder, "audit.json");
        plugin.getLogger().info("Creating FileAuditRepository with file: " + auditFile.getAbsolutePath());
        return new FileAuditRepository(auditFile, plugin);
    }

    private Properties getMysqlConfig() {
        Properties props = new Properties();
        props.setProperty("host", config.getString("storage.mysql.host", "localhost"));
        props.setProperty("port", String.valueOf(config.getInt("storage.mysql.port", 3306)));
        props.setProperty("database", config.getString("storage.mysql.database", "verifymc"));
        props.setProperty("user", config.getString("storage.mysql.user", "root"));
        props.setProperty("password", config.getString("storage.mysql.password", ""));
        return props;
    }

    public ConnectionPoolConfig getConnectionPoolConfig() {
        return ConnectionPoolConfig.fromMySqlConfig(
                config.getString("storage.mysql.host", "localhost"),
                config.getInt("storage.mysql.port", 3306),
                config.getString("storage.mysql.database", "verifymc"),
                config.getString("storage.mysql.user", "root"),
                config.getString("storage.mysql.password", "")
        );
    }

    public boolean isMysqlStorage() {
        return "mysql".equalsIgnoreCase(config.getString("storage.type", "data"));
    }

    public boolean isFileStorage() {
        return !"mysql".equalsIgnoreCase(config.getString("storage.type", "data"));
    }

    public void close() {
        synchronized (lock) {
            if (mysqlUserRepository != null) {
                try {
                    mysqlUserRepository.close();
                    plugin.getLogger().info("MySQL connection pool closed");
                } catch (Exception e) {
                    plugin.getLogger().warning("Error closing MySQL connection pool: " + e.getMessage());
                }
                mysqlUserRepository = null;
            }
            
            if (userRepository instanceof FileUserRepository) {
                try {
                    userRepository.flush();
                    plugin.getLogger().info("File user repository flushed");
                } catch (Exception e) {
                    plugin.getLogger().warning("Error flushing file user repository: " + e.getMessage());
                }
            }
            
            if (auditRepository != null) {
                try {
                    auditRepository.flush();
                    plugin.getLogger().info("Audit repository flushed");
                } catch (Exception e) {
                    plugin.getLogger().warning("Error flushing audit repository: " + e.getMessage());
                }
            }
            
            userRepository = null;
            auditRepository = null;
        }
    }

    public void reinitialize() {
        synchronized (lock) {
            close();
            plugin.getLogger().info("Reinitializing repositories");
        }
    }

    public MysqlUserRepository getMysqlUserRepository() {
        if (mysqlUserRepository == null) {
            UserRepository repo = getUserRepository();
            if (repo instanceof MysqlUserRepository) {
                return (MysqlUserRepository) repo;
            }
        }
        return mysqlUserRepository;
    }

    public FileUserRepository getFileUserRepository() {
        UserRepository repo = getUserRepository();
        if (repo instanceof FileUserRepository) {
            return (FileUserRepository) repo;
        }
        return null;
    }
}
