package team.kitemc.verifymc;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import team.kitemc.verifymc.infrastructure.DIContainer;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import java.io.File;
import java.util.logging.Logger;

public class PluginContext {
    private static PluginContext instance;

    private final JavaPlugin plugin;
    private final DIContainer container;
    private final ConfigurationService configService;

    private PluginContext(JavaPlugin plugin, DIContainer container, ConfigurationService configService) {
        this.plugin = plugin;
        this.container = container;
        this.configService = configService;
    }

    public static synchronized PluginContext getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PluginContext not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static synchronized void initialize(JavaPlugin plugin, DIContainer container, ConfigurationService configService) {
        if (instance != null) {
            throw new IllegalStateException("PluginContext already initialized.");
        }
        instance = new PluginContext(plugin, container, configService);
    }

    public static synchronized void shutdown() {
        instance = null;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public DIContainer getContainer() {
        return container;
    }

    public ConfigurationService getConfig() {
        return configService;
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> type) {
        return container.getBean(type);
    }

    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    public Logger getLogger() {
        return plugin.getLogger();
    }

    public Server getServer() {
        return plugin.getServer();
    }

    public boolean isDebug() {
        return configService.isDebug();
    }

    public String getLanguage() {
        return configService.getLanguage();
    }

    public void debugLog(String msg) {
        if (isDebug()) {
            getLogger().info("[DEBUG] " + msg);
        }
    }
}
