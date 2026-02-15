package team.kitemc.verifymc;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.db.AuditDao;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.domain.repository.AuditRepository;
import team.kitemc.verifymc.domain.repository.UserRepository;
import team.kitemc.verifymc.domain.service.*;
import team.kitemc.verifymc.infrastructure.DIContainer;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;
import team.kitemc.verifymc.infrastructure.lifecycle.Lifecycle;
import team.kitemc.verifymc.infrastructure.lifecycle.LifecycleManager;
import team.kitemc.verifymc.infrastructure.persistence.RepositoryFactory;
import team.kitemc.verifymc.service.*;
import team.kitemc.verifymc.web.ReviewWebSocketServer;
import team.kitemc.verifymc.web.WebServer;

import java.io.File;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceInitializer {
    private static final Logger LOGGER = Logger.getLogger("VerifyMC-Initializer");

    private final Plugin plugin;
    private final DIContainer container;
    private final LifecycleManager lifecycleManager;
    private final ConfigurationService configService;
    private final ResourceManager resourceManager;

    private WebServer webServer;
    private ReviewWebSocketServer wsServer;
    private UserDao userDao;
    private AuditDao auditDao;

    public ServiceInitializer(Plugin plugin) {
        this.plugin = plugin;
        this.container = new DIContainer();
        this.lifecycleManager = new LifecycleManager();
        this.configService = new ConfigurationService((org.bukkit.plugin.java.JavaPlugin) plugin);
        this.resourceManager = new ResourceManager((org.bukkit.plugin.java.JavaPlugin) plugin);
    }

    public void initialize() {
        LOGGER.info("Starting service initialization...");

        resourceManager.initializeResources();

        container.registerInstance(ConfigurationService.class.getSimpleName(), configService);
        container.registerInstance(Plugin.class.getSimpleName(), plugin);

        initializeRepositories();
        initializeDomainServices();
        initializeLegacyServices();
        initializeWebServer();

        container.initializeSingletons();

        initializeLifecycleComponents();

        LOGGER.info("Service initialization completed.");
    }

    private void initializeRepositories() {
        String storageType = configService.getString("storage.type", "data");
        String lang = configService.getLanguage();
        ResourceBundle messages = loadMessages(lang);

        if ("mysql".equalsIgnoreCase(storageType)) {
            initializeMysqlRepositories(messages);
        } else {
            initializeFileRepositories(messages);
        }

        container.registerInstance(UserDao.class.getSimpleName(), userDao);
        container.registerInstance(AuditDao.class.getSimpleName(), auditDao);
    }

    private void initializeMysqlRepositories(ResourceBundle messages) {
        Properties mysqlConfig = new Properties();
        mysqlConfig.setProperty("host", configService.getString("storage.mysql.host", "localhost"));
        mysqlConfig.setProperty("port", String.valueOf(configService.getInt("storage.mysql.port", 3306)));
        mysqlConfig.setProperty("database", configService.getString("storage.mysql.database", "verifymc"));
        mysqlConfig.setProperty("user", configService.getString("storage.mysql.user", "root"));
        mysqlConfig.setProperty("password", configService.getString("storage.mysql.password", ""));

        try {
            userDao = new team.kitemc.verifymc.db.MysqlUserDao(mysqlConfig, messages, plugin);
            auditDao = new team.kitemc.verifymc.db.MysqlAuditDao(mysqlConfig);
            LOGGER.info(messages.getString("storage.mysql.enabled"));
        } catch (Exception e) {
            LOGGER.severe(messages.getString("storage.migrate.fail").replace("{0}", e.getMessage()));
            throw new RuntimeException("Failed to initialize MySQL repositories", e);
        }
    }

    private void initializeFileRepositories(ResourceBundle messages) {
        File dataFolder = plugin.getDataFolder();
        File userFile = new File(dataFolder, "data/users.json");
        File auditFile = new File(dataFolder, "data/audits.json");
        userFile.getParentFile().mkdirs();
        auditFile.getParentFile().mkdirs();

        userDao = new team.kitemc.verifymc.db.FileUserDao(userFile, plugin);
        auditDao = new team.kitemc.verifymc.db.FileAuditDao(auditFile);
        LOGGER.info(messages.getString("storage.file.enabled"));
    }

    private void initializeDomainServices() {
        RepositoryFactory repositoryFactory = new RepositoryFactory(configService, plugin);
        container.registerInstance(RepositoryFactory.class.getSimpleName(), repositoryFactory);

        UserRepository userRepo = repositoryFactory.getUserRepository();
        AuditRepository auditRepo = repositoryFactory.getAuditRepository();
        container.registerInstance(UserRepository.class.getSimpleName(), userRepo);
        container.registerInstance(AuditRepository.class.getSimpleName(), auditRepo);

        UserService userService = new UserService(userRepo, configService);
        container.registerInstance(UserService.class.getSimpleName(), userService);

        WhitelistService whitelistService = new WhitelistService(plugin, userService, configService);
        container.registerInstance(WhitelistService.class.getSimpleName(), whitelistService);

        VerificationCodeService verificationCodeService = new VerificationCodeService(configService);
        container.registerInstance(VerificationCodeService.class.getSimpleName(), verificationCodeService);

        MailIntegrationService mailService = new MailIntegrationService(configService);
        container.registerInstance(MailIntegrationService.class.getSimpleName(), mailService);

        AuthmeIntegrationService authmeService = new AuthmeIntegrationService(configService, userRepo);
        container.registerInstance(AuthmeIntegrationService.class.getSimpleName(), authmeService);

        ReviewService reviewService = new ReviewService(plugin, userService, auditRepo, configService);
        container.registerInstance(ReviewService.class.getSimpleName(), reviewService);

        RegistrationService registrationService = new RegistrationService(plugin, userService, configService);
        container.registerInstance(RegistrationService.class.getSimpleName(), registrationService);
    }

    private void initializeLegacyServices() {
        VerifyCodeService codeService = new VerifyCodeService(plugin);
        container.registerInstance(VerifyCodeService.class.getSimpleName(), codeService);

        AuthmeService authmeService = new AuthmeService(plugin);
        authmeService.setUserDao(userDao);
        container.registerInstance(AuthmeService.class.getSimpleName(), authmeService);

        VersionCheckService versionCheckService = new VersionCheckService(plugin);
        container.registerInstance(VersionCheckService.class.getSimpleName(), versionCheckService);

        CaptchaService captchaService = new CaptchaService(plugin);
        container.registerInstance(CaptchaService.class.getSimpleName(), captchaService);

        QuestionnaireService questionnaireService = new QuestionnaireService(plugin);
        container.registerInstance(QuestionnaireService.class.getSimpleName(), questionnaireService);

        DiscordService discordService = new DiscordService(plugin);
        discordService.setUserDao(userDao);
        container.registerInstance(DiscordService.class.getSimpleName(), discordService);

        team.kitemc.verifymc.mail.MailService mailService = new team.kitemc.verifymc.mail.MailService(plugin, this::getMessage);
        container.registerInstance(team.kitemc.verifymc.mail.MailService.class.getSimpleName(), mailService);
    }

    private void initializeWebServer() {
        int port = configService.getInt("web_port", 8080);
        int wsPort = configService.getInt("ws_port", port + 1);

        wsServer = new ReviewWebSocketServer(wsPort, plugin);
        container.registerInstance(ReviewWebSocketServer.class.getSimpleName(), wsServer);

        String theme = configService.getString("frontend.theme", "default");
        String staticDir = resourceManager.getThemeStaticDir(theme);

        String lang = configService.getLanguage();
        ResourceBundle messages = loadMessages(lang);

        AuthmeService authmeService = container.getBean(AuthmeService.class);
        CaptchaService captchaService = container.getBean(CaptchaService.class);
        QuestionnaireService questionnaireService = container.getBean(QuestionnaireService.class);
        DiscordService discordService = container.getBean(DiscordService.class);
        VerifyCodeService codeService = container.getBean(VerifyCodeService.class);
        team.kitemc.verifymc.mail.MailService mailService = container.getBean(team.kitemc.verifymc.mail.MailService.class);

        webServer = new WebServer(port, staticDir, plugin, codeService, mailService, userDao, auditDao,
                authmeService, captchaService, questionnaireService, discordService, wsServer, messages);
        container.registerInstance(WebServer.class.getSimpleName(), webServer);
    }

    private void initializeLifecycleComponents() {
        if (webServer instanceof Lifecycle) {
            lifecycleManager.register((Lifecycle) webServer);
        }
    }

    public void startServices() {
        LOGGER.info("Starting services...");

        try {
            wsServer.start();
            LOGGER.info("WebSocket server started on port: " + wsServer.getPort());
        } catch (Exception e) {
            LOGGER.warning("Failed to start WebSocket server: " + e.getMessage());
        }

        try {
            webServer.start();
            LOGGER.info("Web server started on port: " + webServer.getPort());
        } catch (Exception e) {
            LOGGER.warning("Failed to start web server: " + e.getMessage());
        }

        lifecycleManager.startAll();
        LOGGER.info("All services started.");
    }

    public void performPostStartupTasks() {
        performWhitelistSync();
        performAuthmeSync();
        detectServerType();
        startVersionCheck();
    }

    private void performWhitelistSync() {
        boolean autoSync = configService.getBoolean("auto_sync_whitelist", true);
        boolean autoCleanup = configService.getBoolean("auto_cleanup_whitelist", true);

        WhitelistService whitelistService = container.getBean(WhitelistService.class);
        if (whitelistService != null) {
            if (autoSync) {
                whitelistService.syncToServer();
            }
            if (autoCleanup) {
                whitelistService.cleanupWhitelist();
            }
        }
    }

    private void performAuthmeSync() {
        if (isFoliaServer()) {
            LOGGER.info("§e[VerifyMC] AuthMe sync task disabled on Folia (use manual /vmc reload instead)");
            return;
        }

        AuthmeService authmeService = container.getBean(AuthmeService.class);
        if (authmeService != null && authmeService.isAuthmeEnabled()) {
            authmeService.syncApprovedUsers();
            long syncTicks = Math.max(20L, configService.getLong("authme.database.sync_interval_seconds", 30L) * 20L);
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                authmeService.syncApprovedUsers();
                WhitelistService whitelistService = container.getBean(WhitelistService.class);
                if (whitelistService != null) {
                    whitelistService.syncToServer();
                }
            }, syncTicks, syncTicks);
        }
    }

    private void detectServerType() {
        String serverName = plugin.getServer().getName().toLowerCase();
        String lang = configService.getLanguage();
        ResourceBundle messages = loadMessages(lang);

        logServerMessage(messages, "server.cores_supported");

        if (isFoliaServer()) {
            logServerMessage(messages, "server.detected.folia");
            LOGGER.info("§e[VerifyMC] Folia compatibility mode enabled:");
            LOGGER.info("§e  - Player kick uses delayed scheduling");
            LOGGER.info("§e  - Whitelist.json auto-sync disabled (use /vmc reload to manually sync)");
            LOGGER.info("§e  - Version update reminders disabled");
        } else if (serverName.contains("purpur")) {
            logServerMessage(messages, "server.detected.purpur");
        } else if (serverName.contains("paper")) {
            logServerMessage(messages, "server.detected.paper");
        } else if (serverName.contains("spigot")) {
            logServerMessage(messages, "server.detected.spigot");
        } else if (serverName.contains("bukkit")) {
            logServerMessage(messages, "server.detected.bukkit");
        } else if (serverName.contains("velocity")) {
            logServerMessage(messages, "server.detected.velocity");
        } else if (serverName.contains("waterfall")) {
            logServerMessage(messages, "server.detected.waterfall");
        } else if (serverName.contains("canvas")) {
            logServerMessage(messages, "server.detected.canvas");
        } else {
            logServerMessage(messages, "server.detected.unknown");
        }
    }

    private void logServerMessage(ResourceBundle messages, String key) {
        if (messages.containsKey(key)) {
            LOGGER.info(messages.getString(key));
        } else {
            LOGGER.info(key);
        }
    }

    private void startVersionCheck() {
        if (isFoliaServer()) {
            return;
        }

        VersionCheckService versionCheckService = container.getBean(VersionCheckService.class);
        if (versionCheckService != null) {
            String lang = configService.getLanguage();
            ResourceBundle messages = loadMessages(lang);
            versionCheckService.checkForUpdatesAsync().thenAccept(result -> {
                if (result.isSuccess() && result.isUpdateAvailable()) {
                    LOGGER.info("§e[VerifyMC] " + getMessageFromBundle(messages, "version.update_available"));
                    LOGGER.info("§e[VerifyMC] " + getMessageFromBundle(messages, "version.current_version") + ": " + result.getCurrentVersion());
                    LOGGER.info("§e[VerifyMC] " + getMessageFromBundle(messages, "version.latest_version") + ": " + result.getLatestVersion());
                    LOGGER.info("§e[VerifyMC] " + getMessageFromBundle(messages, "version.download_url") + ": " + versionCheckService.getReleasesUrl());
                }
            });
        }
    }

    private String getMessageFromBundle(ResourceBundle messages, String key) {
        if (messages.containsKey(key)) {
            return messages.getString(key);
        }
        return key;
    }

    private boolean isFoliaServer() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void shutdown() {
        LOGGER.info("Shutting down services...");

        lifecycleManager.stopAll();

        if (webServer != null) {
            try {
                webServer.stop();
                LOGGER.info("Web server stopped.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error stopping web server", e);
            }
        }

        if (wsServer != null) {
            try {
                wsServer.stop();
                LOGGER.info("WebSocket server stopped.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error stopping WebSocket server", e);
            }
        }

        if (userDao != null) {
            userDao.save();
            LOGGER.info("User data saved.");
        }

        if (auditDao != null) {
            auditDao.save();
            LOGGER.info("Audit data saved.");
        }

        String whitelistMode = configService.getString("whitelist_mode", "bukkit");
        boolean whitelistJsonSync = configService.getBoolean("whitelist_json_sync", true);
        if ("bukkit".equalsIgnoreCase(whitelistMode) && whitelistJsonSync) {
            WhitelistService whitelistService = container.getBean(WhitelistService.class);
            if (whitelistService != null) {
                whitelistService.syncToJson();
            }
        }

        container.clear();
        LOGGER.info("All services shut down.");
    }

    public DIContainer getContainer() {
        return container;
    }

    public ConfigurationService getConfigService() {
        return configService;
    }

    public WebServer getWebServer() {
        return webServer;
    }

    public ReviewWebSocketServer getWebSocketServer() {
        return wsServer;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public AuditDao getAuditDao() {
        return auditDao;
    }

    private ResourceBundle loadMessages(String lang) {
        try {
            return ResourceBundle.getBundle("i18n/messages_" + lang);
        } catch (MissingResourceException e) {
            LOGGER.warning("No messages_" + lang + ".properties found, fallback to English.");
            return ResourceBundle.getBundle("i18n/messages_en");
        }
    }

    private String getMessage(String key) {
        String lang = configService.getLanguage();
        try {
            ResourceBundle messages = loadMessages(lang);
            if (messages.containsKey(key)) {
                return messages.getString(key);
            }
        } catch (Exception e) {
            // Ignore
        }
        return key;
    }
}
