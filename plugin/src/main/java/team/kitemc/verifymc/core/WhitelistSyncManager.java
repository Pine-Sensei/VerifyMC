package team.kitemc.verifymc.core;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import team.kitemc.verifymc.db.UserDao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class WhitelistSyncManager {
    private static final Logger LOGGER = Logger.getLogger(WhitelistSyncManager.class.getName());
    
    private final UserDao userDao;
    private final boolean autoSync;
    private final String whitelistMode;
    private final boolean whitelistJsonSync;
    private final Path whitelistJsonPath;
    private long lastWhitelistJsonModified = 0;
    
    public WhitelistSyncManager(UserDao userDao, 
                               boolean autoSync,
                               String whitelistMode,
                               boolean whitelistJsonSync,
                               Path whitelistJsonPath) {
        this.userDao = userDao;
        this.autoSync = autoSync;
        this.whitelistMode = whitelistMode;
        this.whitelistJsonSync = whitelistJsonSync;
        this.whitelistJsonPath = whitelistJsonPath;
    }
    
    public void syncToWhitelist() {
        if (!autoSync) return;
        syncWhitelistToServer();
        LOGGER.info("Whitelist sync completed");
    }
    
    public void syncWhitelistToServer() {
        for (Map<String, Object> user : userDao.getAllUsers()) {
            String name = (String) user.get("username");
            String status = (String) user.get("status");
            if ("approved".equals(status)) {
                Bukkit.getOfflinePlayer(name).setWhitelisted(true);
            } else if ("banned".equals(status)) {
                Bukkit.getOfflinePlayer(name).setWhitelisted(false);
            }
        }
    }
    
    public void cleanupServerWhitelist() {
        for (OfflinePlayer p : Bukkit.getWhitelistedPlayers()) {
            Map<String, Object> user = userDao.getUserByUsername(p.getName());
            if (user == null || !"approved".equals(user.get("status"))) {
                p.setWhitelisted(false);
            } else {
                p.setWhitelisted(true);
            }
        }
    }
    
    public void addToWhitelist(String username) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(username);
        player.setWhitelisted(true);
        LOGGER.info("Added to whitelist: " + username);
    }
    
    public void removeFromWhitelist(String username) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(username);
        player.setWhitelisted(false);
        LOGGER.info("Removed from whitelist: " + username);
    }
    
    public void syncWhitelistJsonToPlugin() {
        if (whitelistJsonPath == null || !Files.exists(whitelistJsonPath)) return;
        
        try {
            List<String> lines = Files.readAllLines(whitelistJsonPath);
            String json = String.join("\n", lines);
            List<Map<String, Object>> list = new Gson().fromJson(json, List.class);
            for (Map<String, Object> entry : list) {
                String uuid = (String) entry.get("uuid");
                if (uuid != null) {
                    Map<String, Object> user = userDao.getAllUsers().stream()
                        .filter(u -> uuid.equals(u.get("uuid")))
                        .findFirst().orElse(null);
                    if (user != null) {
                        Object whitelisted = entry.get("whitelisted");
                        String currentStatus = (String) user.get("status");
                        if ("pending".equals(currentStatus) && Boolean.TRUE.equals(whitelisted)) {
                            user.put("status", "approved");
                        } else if (!"approved".equals(currentStatus) && !"banned".equals(currentStatus) && !Boolean.TRUE.equals(whitelisted)) {
                            user.put("status", "pending");
                        }
                    }
                }
            }
            userDao.save();
        } catch (Exception e) {
            LOGGER.warning("Failed to sync whitelist.json to plugin: " + e.getMessage());
        }
    }
    
    public void syncPluginToWhitelistJson() {
        if (!"bukkit".equalsIgnoreCase(whitelistMode)) return;
        
        try {
            List<Map<String, Object>> users = userDao.getAllUsers();
            List<Map<String, Object>> wl = new ArrayList<>();
            for (Map<String, Object> user : users) {
                if ("approved".equals(user.get("status"))) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("uuid", user.get("uuid"));
                    entry.put("name", user.get("username"));
                    entry.put("whitelisted", true);
                    wl.add(entry);
                }
            }
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(wl);
            Files.write(whitelistJsonPath, json.getBytes(StandardCharsets.UTF_8), 
                       StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            LOGGER.warning("Failed to sync plugin to whitelist.json: " + e.getMessage());
        }
    }
    
    public boolean checkWhitelistJsonModified() {
        if (whitelistJsonPath == null || !Files.exists(whitelistJsonPath)) return false;
        
        try {
            long modified = Files.getLastModifiedTime(whitelistJsonPath).toMillis();
            if (modified != lastWhitelistJsonModified) {
                lastWhitelistJsonModified = modified;
                return true;
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to check whitelist.json modification: " + e.getMessage());
        }
        return false;
    }
}
