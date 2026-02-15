package team.kitemc.verifymc.domain.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhitelistService {
    private final Plugin plugin;
    private final UserService userService;
    private final ConfigurationService configService;
    private Path whitelistJsonPath;
    private String whitelistMode;
    private boolean whitelistJsonSync;

    public WhitelistService(Plugin plugin, UserService userService, ConfigurationService configService) {
        this.plugin = plugin;
        this.userService = userService;
        this.configService = configService;
        initializePaths();
    }

    private void initializePaths() {
        this.whitelistMode = configService.getString("whitelist_mode", "bukkit");
        this.whitelistJsonSync = configService.getBoolean("whitelist_json_sync", true);
        this.whitelistJsonPath = Paths.get(plugin.getServer().getWorldContainer().getAbsolutePath(), "whitelist.json");
    }

    public void syncToServer() {
        List<User> users = userService.getAllUsers();
        for (User user : users) {
            String name = user.getUsername();
            UserStatus status = user.getStatus();
            
            if (status == UserStatus.APPROVED) {
                Bukkit.getOfflinePlayer(name).setWhitelisted(true);
            } else if (status == UserStatus.BANNED) {
                Bukkit.getOfflinePlayer(name).setWhitelisted(false);
            }
        }
    }

    public void syncFromJson() {
        if (!"bukkit".equalsIgnoreCase(whitelistMode) || !whitelistJsonSync) {
            return;
        }

        if (whitelistJsonPath == null || !Files.exists(whitelistJsonPath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(whitelistJsonPath);
            String json = String.join("\n", lines);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> whitelistEntries = new Gson().fromJson(json, List.class);
            
            if (whitelistEntries == null) {
                return;
            }

            for (Map<String, Object> entry : whitelistEntries) {
                String uuid = (String) entry.get("uuid");
                if (uuid != null) {
                    User user = userService.getUserByUuid(uuid).orElse(null);
                    if (user != null) {
                        Object whitelisted = entry.get("whitelisted");
                        UserStatus currentStatus = user.getStatus();
                        
                        if (currentStatus == UserStatus.PENDING && Boolean.TRUE.equals(whitelisted)) {
                            userService.updateUserStatus(uuid, UserStatus.APPROVED);
                        } else if (currentStatus != UserStatus.APPROVED && currentStatus != UserStatus.BANNED && !Boolean.TRUE.equals(whitelisted)) {
                            userService.updateUserStatus(uuid, UserStatus.PENDING);
                        }
                    }
                }
            }
            
            userService.flush();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to sync from whitelist.json: " + e.getMessage());
        }
    }

    public void syncToJson() {
        if (!"bukkit".equalsIgnoreCase(whitelistMode) || !whitelistJsonSync) {
            return;
        }

        try {
            List<User> users = userService.getAllUsers();
            List<Map<String, Object>> whitelist = new ArrayList<>();
            
            for (User user : users) {
                if (user.getStatus() == UserStatus.APPROVED) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("uuid", user.getUuid());
                    entry.put("name", user.getUsername());
                    entry.put("whitelisted", true);
                    whitelist.add(entry);
                }
            }

            String json = new GsonBuilder().setPrettyPrinting().create().toJson(whitelist);
            Files.write(whitelistJsonPath, json.getBytes(StandardCharsets.UTF_8), 
                    java.nio.file.StandardOpenOption.CREATE, 
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to sync to whitelist.json: " + e.getMessage());
        }
    }

    public boolean addToWhitelist(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        User user = userService.getUserByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        boolean updated = userService.updateUserStatus(user.getUuid(), UserStatus.APPROVED);
        if (updated) {
            Bukkit.getOfflinePlayer(username).setWhitelisted(true);
            
            if ("bukkit".equalsIgnoreCase(whitelistMode) && whitelistJsonSync) {
                syncToJson();
            }
            
            userService.flush();
        }
        
        return updated;
    }

    public boolean removeFromWhitelist(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        User user = userService.getUserByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        boolean updated = userService.updateUserStatus(user.getUuid(), UserStatus.BANNED);
        if (updated) {
            Bukkit.getOfflinePlayer(username).setWhitelisted(false);
            
            if ("bukkit".equalsIgnoreCase(whitelistMode) && whitelistJsonSync) {
                syncToJson();
            }
            
            userService.flush();
        }
        
        return updated;
    }

    public void cleanupWhitelist() {
        for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
            String name = player.getName();
            if (name == null) {
                continue;
            }
            
            User user = userService.getUserByUsername(name).orElse(null);
            if (user == null || user.getStatus() != UserStatus.APPROVED) {
                player.setWhitelisted(false);
            } else {
                player.setWhitelisted(true);
            }
        }
    }

    public boolean isWhitelisted(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        User user = userService.getUserByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        return user.getStatus() == UserStatus.APPROVED;
    }

    public void reload() {
        initializePaths();
    }

    public int getApprovedCount() {
        return (int) userService.getAllUsers().stream()
                .filter(u -> u.getStatus() == UserStatus.APPROVED)
                .count();
    }

    public int getPendingCount() {
        return (int) userService.getAllUsers().stream()
                .filter(u -> u.getStatus() == UserStatus.PENDING)
                .count();
    }
}
