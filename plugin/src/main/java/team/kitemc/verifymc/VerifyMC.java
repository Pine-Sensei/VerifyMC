package team.kitemc.verifymc;

import org.bukkit.plugin.java.JavaPlugin;
import team.kitemc.verifymc.command.CommandHandler;
import team.kitemc.verifymc.infrastructure.DIContainer;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;
import team.kitemc.verifymc.infrastructure.lifecycle.Lifecycle;
import team.kitemc.verifymc.infrastructure.lifecycle.LifecycleState;
import team.kitemc.verifymc.listener.PlayerLoginListener;
import team.kitemc.verifymc.service.VersionCheckService;

public class VerifyMC extends JavaPlugin implements Lifecycle {
    private static VerifyMC instance;

    private PluginContext context;
    private ServiceInitializer serviceInitializer;
    private PlayerLoginListener playerLoginListener;
    private LifecycleState state = LifecycleState.NEW;

    public static VerifyMC getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        try {
            initialize();
            start();
            getLogger().info("Plugin enabled successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize plugin: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            stop();
            getLogger().info("Plugin disabled.");
        } catch (Exception e) {
            getLogger().warning("Error during shutdown: " + e.getMessage());
        }
        instance = null;
    }

    @Override
    public void initialize() throws Exception {
        if (state != LifecycleState.NEW) {
            return;
        }

        saveDefaultConfig();

        serviceInitializer = new ServiceInitializer(this);
        serviceInitializer.initialize();

        DIContainer container = serviceInitializer.getContainer();
        ConfigurationService configService = container.getBean(ConfigurationService.class);
        PluginContext.initialize(this, container, configService);
        context = PluginContext.getInstance();

        playerLoginListener = new PlayerLoginListener(context);

        state = LifecycleState.INITIALIZED;
    }

    @Override
    public void start() throws Exception {
        if (state != LifecycleState.INITIALIZED && state != LifecycleState.NEW) {
            return;
        }

        if (state == LifecycleState.NEW) {
            initialize();
        }

        serviceInitializer.startServices();
        registerCommands();
        registerListeners();
        serviceInitializer.performPostStartupTasks();
        startMetrics();

        state = LifecycleState.STARTED;
    }

    @Override
    public void stop() throws Exception {
        if (state != LifecycleState.STARTED) {
            return;
        }

        if (serviceInitializer != null) {
            serviceInitializer.shutdown();
        }

        if (context != null) {
            PluginContext.shutdown();
        }

        state = LifecycleState.STOPPED;
    }

    @Override
    public LifecycleState getState() {
        return state;
    }

    @Override
    public void setState(LifecycleState state) {
        this.state = state;
    }

    private void registerCommands() {
        CommandHandler handler = new CommandHandler(context);
        getCommand("vmc").setExecutor(handler);
        getCommand("vmc").setTabCompleter(handler);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(playerLoginListener, this);
    }

    private void startMetrics() {
        try {
            new Metrics(this, 26637);
        } catch (Exception e) {
            getLogger().warning("Failed to start metrics: " + e.getMessage());
        }
    }

    public PluginContext getContext() {
        return context;
    }

    public ServiceInitializer getServiceInitializer() {
        return serviceInitializer;
    }

    public VersionCheckService getVersionCheckService() {
        if (serviceInitializer != null) {
            return serviceInitializer.getContainer().getBean(VersionCheckService.class);
        }
        return null;
    }
}
