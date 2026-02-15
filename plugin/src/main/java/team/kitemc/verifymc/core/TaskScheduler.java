package team.kitemc.verifymc.core;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import team.kitemc.verifymc.service.IAuthmeService;
import java.util.logging.Logger;

public class TaskScheduler {
    private static final Logger LOGGER = Logger.getLogger(TaskScheduler.class.getName());
    
    private final Plugin plugin;
    private BukkitRunnable authmeSyncTask;
    private BukkitRunnable whitelistSyncTask;
    private BukkitRunnable whitelistJsonWatcherTask;
    private BukkitRunnable versionCheckReminderTask;
    
    public TaskScheduler(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public void startAuthmeSyncTask(IAuthmeService authmeService, 
                                   WhitelistSyncManager whitelistSyncManager,
                                   long intervalSeconds) {
        if (authmeService == null || !authmeService.isAuthmeEnabled()) return;
        
        long syncTicks = Math.max(20L, intervalSeconds * 20L);
        authmeSyncTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    authmeService.syncApprovedUsers();
                    whitelistSyncManager.syncWhitelistToServer();
                } catch (Exception e) {
                    LOGGER.warning("AuthMe sync task failed: " + e.getMessage());
                }
            }
        };
        authmeSyncTask.runTaskTimerAsynchronously(plugin, syncTicks, syncTicks);
        LOGGER.info("Started AuthMe sync task with interval: " + intervalSeconds + "s");
    }
    
    public void startWhitelistJsonWatcher(WhitelistSyncManager whitelistSyncManager) {
        if (whitelistJsonWatcherTask != null) return;
        
        whitelistJsonWatcherTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (whitelistSyncManager.checkWhitelistJsonModified()) {
                        whitelistSyncManager.syncWhitelistJsonToPlugin();
                    }
                } catch (Exception e) {
                    LOGGER.warning("Whitelist.json watcher failed: " + e.getMessage());
                }
            }
        };
        whitelistJsonWatcherTask.runTaskTimerAsynchronously(plugin, 40L, 100L);
        LOGGER.info("Started whitelist.json watcher");
    }
    
    public void startVersionCheckReminder(Runnable reminderCallback, long intervalTicks, int maxReminders) {
        if (versionCheckReminderTask != null) return;
        
        versionCheckReminderTask = new BukkitRunnable() {
            private int reminderCount = 0;
            
            @Override
            public void run() {
                if (reminderCount >= maxReminders) {
                    this.cancel();
                    return;
                }
                
                reminderCount++;
                reminderCallback.run();
            }
        };
        versionCheckReminderTask.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
        LOGGER.info("Started version check reminder task");
    }
    
    public void stopAll() {
        if (authmeSyncTask != null) {
            authmeSyncTask.cancel();
            authmeSyncTask = null;
        }
        if (whitelistSyncTask != null) {
            whitelistSyncTask.cancel();
            whitelistSyncTask = null;
        }
        if (whitelistJsonWatcherTask != null) {
            whitelistJsonWatcherTask.cancel();
            whitelistJsonWatcherTask = null;
        }
        if (versionCheckReminderTask != null) {
            versionCheckReminderTask.cancel();
            versionCheckReminderTask = null;
        }
        LOGGER.info("All scheduled tasks stopped");
    }
    
    public boolean isFoliaServer() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
