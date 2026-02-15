package team.kitemc.verifymc.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import team.kitemc.verifymc.PluginContext;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.domain.service.WhitelistService;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.web.ReviewWebSocketServer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler implements TabExecutor {
    private final PluginContext context;
    private final ConfigurationService configService;
    private ResourceBundle messages;

    public CommandHandler(PluginContext context) {
        this.context = context;
        this.configService = context.getConfig();
        loadMessages();
    }

    private void loadMessages() {
        String lang = configService.getLanguage();
        try {
            messages = ResourceBundle.getBundle("i18n/messages_" + lang);
        } catch (MissingResourceException e) {
            try {
                messages = ResourceBundle.getBundle("i18n/messages", new Locale(lang));
            } catch (MissingResourceException e2) {
                messages = ResourceBundle.getBundle("i18n/messages_en");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("vmc")) {
            return false;
        }

        String language = configService.getLanguage();
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        if (args.length == 0) {
            showHelp(sender, language);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "help":
                showHelp(sender, language);
                break;
            case "reload":
                if (isPlayer && !player.hasPermission("verifymc.admin")) {
                    player.sendMessage("§c" + getMessage("command.no_permission", language));
                    return true;
                }
                reloadPlugin(sender, language);
                break;
            case "add":
                if (args.length < 3) {
                    sender.sendMessage("§e" + getMessage("command.add_usage", language));
                    return true;
                }
                if (isPlayer && !player.hasPermission("verifymc.admin")) {
                    player.sendMessage("§c" + getMessage("command.no_permission", language));
                    return true;
                }
                addWhitelist(sender, args[1], args[2], language);
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("§e" + getMessage("command.remove_usage", language));
                    return true;
                }
                if (isPlayer && !player.hasPermission("verifymc.admin")) {
                    player.sendMessage("§c" + getMessage("command.no_permission", language));
                    return true;
                }
                removeWhitelist(sender, args[1], language);
                break;
            case "port":
                showPort(sender, language);
                break;
            default:
                showHelp(sender, language);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("vmc")) {
            return null;
        }

        List<String> subCommands = Arrays.asList("help", "reload", "add", "remove", "port");
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String cmd : subCommands) {
                if (cmd.startsWith(prefix)) {
                    result.add(cmd);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private void showHelp(CommandSender sender, String language) {
        sender.sendMessage("§6=== VerifyMC " + getMessage("command.help.title", language) + " ===\n");
        sender.sendMessage("§e/vmc help §7- " + getMessage("command.help.help", language) + "\n");
        sender.sendMessage("§e/vmc port §7- " + getMessage("command.help.port", language) + "\n");
        sender.sendMessage("§e/vmc reload §7- " + getMessage("command.help.reload", language) + "\n");
        sender.sendMessage("§e/vmc add <" + getMessage("command.help.player", language) + "> §7- " + getMessage("command.help.add", language) + "\n");
        sender.sendMessage("§e/vmc remove <" + getMessage("command.help.player", language) + "> §7- " + getMessage("command.help.remove", language) + "\n");
    }

    private void reloadPlugin(CommandSender sender, String language) {
        try {
            String oldTheme = configService.getString("frontend.theme", "default");
            java.io.File configFile = new java.io.File(context.getDataFolder(), "config.yml");
            String configContent = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
            String newTheme = oldTheme;
            Matcher m = Pattern.compile("frontend.theme\\s*:\\s*(\\w+)").matcher(configContent);
            if (m.find()) {
                newTheme = m.group(1);
            }
            boolean themeChanged = !oldTheme.equals(newTheme);

            sender.sendMessage("§aRestarting plugin...");
            Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                try {
                    Bukkit.getPluginManager().disablePlugin(context.getPlugin());
                    Bukkit.getPluginManager().enablePlugin(context.getPlugin());
                    sender.sendMessage("§aPlugin restart successful");
                    sender.sendMessage("§7Note: /vmc reload can only reload partial plugin configurations. For a complete reload, please restart the server.");
                    if (themeChanged) {
                        sender.sendMessage("§ePlease restart server to switch frontend theme");
                    }
                } catch (Exception e) {
                    sender.sendMessage("§cPlugin restart failed: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            sender.sendMessage("§cPlugin restart failed: " + e.getMessage());
        }
    }

    private void addWhitelist(CommandSender sender, String targetName, String email, String language) {
        try {
            UserDao userDao = context.getService(UserDao.class);
            AuthmeService authmeService = context.getService(AuthmeService.class);
            ReviewWebSocketServer wsServer = context.getService(ReviewWebSocketServer.class);
            WhitelistService whitelistService = context.getService(WhitelistService.class);

            try {
                Bukkit.getOfflinePlayer(targetName).setWhitelisted(true);
            } catch (Exception whitelistError) {
                context.debugLog("Whitelist operation failed for " + targetName + ": " + whitelistError.getMessage());
            }

            String uuid = Bukkit.getOfflinePlayer(targetName).getUniqueId().toString();
            Map<String, Object> user = userDao.getUserByUuid(uuid);
            boolean ok;

            if (user != null) {
                ok = userDao.updateUserStatus(uuid, "approved");
            } else {
                ok = userDao.registerUser(uuid, targetName, email, "approved");
            }

            userDao.save();

            String whitelistMode = configService.getString("whitelist_mode", "bukkit");
            boolean whitelistJsonSync = configService.getBoolean("whitelist_json_sync", true);
            if ("bukkit".equalsIgnoreCase(whitelistMode) && whitelistJsonSync && whitelistService != null) {
                whitelistService.syncToJson();
            }

            if (whitelistService != null) {
                whitelistService.syncToServer();
            }

            if (wsServer != null) {
                wsServer.broadcastMessage("{\"type\":\"user_update\"}");
            }

            if (ok) {
                sender.sendMessage("§a" + getMessage("command.add_success", language).replace("{player}", targetName));
            } else {
                sender.sendMessage("§c" + getMessage("command.add_failed", language));
            }
        } catch (Exception e) {
            sender.sendMessage("§c" + getMessage("command.add_failed", language) + ": " + e.getMessage());
        }
    }

    private void removeWhitelist(CommandSender sender, String targetName, String language) {
        try {
            UserDao userDao = context.getService(UserDao.class);
            AuthmeService authmeService = context.getService(AuthmeService.class);
            ReviewWebSocketServer wsServer = context.getService(ReviewWebSocketServer.class);

            Bukkit.getOfflinePlayer(targetName).setWhitelisted(false);

            Map<String, Object> user = userDao.getUserByUsername(targetName);
            String uuid = null;
            if (user != null && user.get("uuid") != null) {
                uuid = user.get("uuid").toString();
            } else {
                uuid = Bukkit.getOfflinePlayer(targetName).getUniqueId().toString();
            }

            if (authmeService != null && authmeService.isAuthmeEnabled()) {
                authmeService.unregisterFromAuthme(targetName);
            }

            boolean ok = userDao.deleteUser(uuid);
            userDao.save();

            if (ok) {
                sender.sendMessage("§a" + getMessage("command.remove_success", language).replace("{player}", targetName));
                if (wsServer != null) {
                    wsServer.broadcastMessage("{\"type\":\"user_update\"}");
                }
            } else {
                sender.sendMessage("§c" + getMessage("command.remove_failed", language));
            }
        } catch (Exception e) {
            sender.sendMessage("§c" + getMessage("command.remove_failed", language) + ": " + e.getMessage());
        }
    }

    private void showPort(CommandSender sender, String language) {
        int port = configService.getInt("web_port", 8080);
        sender.sendMessage("§a" + getMessage("command.port_info", language).replace("{port}", String.valueOf(port)));
    }

    private String getMessage(String key, String language) {
        try {
            if (messages != null && messages.containsKey(key)) {
                return messages.getString(key);
            }
        } catch (Exception e) {
            // Ignore
        }
        return key;
    }
}
