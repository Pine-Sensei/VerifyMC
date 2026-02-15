package team.kitemc.verifymc.core;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.mail.IMailService;
import team.kitemc.verifymc.service.IAuthmeService;
import team.kitemc.verifymc.web.ReviewWebSocketServer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.logging.Logger;

public class PluginCommandHandler implements CommandExecutor, TabCompleter {
    private static final Logger LOGGER = Logger.getLogger(PluginCommandHandler.class.getName());
    
    private final UserDao userDao;
    private final IMailService mailService;
    private final IAuthmeService authmeService;
    private final ReviewWebSocketServer wsServer;
    private final WhitelistSyncManager whitelistSyncManager;
    private final Function<String, String> getMessage;
    private final String webRegisterUrl;
    private final String whitelistMode;
    private final boolean whitelistJsonSync;
    
    public PluginCommandHandler(UserDao userDao, 
                               IMailService mailService,
                               IAuthmeService authmeService,
                               ReviewWebSocketServer wsServer,
                               WhitelistSyncManager whitelistSyncManager,
                               Function<String, String> getMessage,
                               String webRegisterUrl,
                               String whitelistMode,
                               boolean whitelistJsonSync) {
        this.userDao = userDao;
        this.mailService = mailService;
        this.authmeService = authmeService;
        this.wsServer = wsServer;
        this.whitelistSyncManager = whitelistSyncManager;
        this.getMessage = getMessage;
        this.webRegisterUrl = webRegisterUrl;
        this.whitelistMode = whitelistMode;
        this.whitelistJsonSync = whitelistJsonSync;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("vmc")) return false;
        
        String language = "en";
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;
        
        if (args.length == 0) {
            sendHelp(sender, language);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                sendHelp(sender, language);
                break;
            case "reload":
                if (isPlayer && !player.hasPermission("verifymc.admin")) {
                    player.sendMessage("§c" + getMessage.apply("command.no_permission"));
                    return true;
                }
                handleReload(sender, language);
                break;
            case "add":
                if (args.length < 3) {
                    sender.sendMessage("§e" + getMessage.apply("command.add_usage"));
                    return true;
                }
                if (isPlayer && !player.hasPermission("verifymc.admin")) {
                    player.sendMessage("§c" + getMessage.apply("command.no_permission"));
                    return true;
                }
                handleAdd(sender, args[1], args[2], language);
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("§e" + getMessage.apply("command.remove_usage"));
                    return true;
                }
                if (isPlayer && !player.hasPermission("verifymc.admin")) {
                    player.sendMessage("§c" + getMessage.apply("command.no_permission"));
                    return true;
                }
                handleRemove(sender, args[1], language);
                break;
            case "port":
                handlePort(sender, language);
                break;
            default:
                sendHelp(sender, language);
                break;
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("vmc")) return null;
        
        List<String> subCommands = Arrays.asList("help", "reload", "add", "remove", "port");
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> result = new java.util.ArrayList<>();
            for (String cmd : subCommands) {
                if (cmd.startsWith(prefix)) result.add(cmd);
            }
            return result;
        }
        return Collections.emptyList();
    }
    
    private void handleAdd(CommandSender sender, String targetName, String email, String language) {
        try {
            try {
                Bukkit.getOfflinePlayer(targetName).setWhitelisted(true);
            } catch (Exception whitelistError) {
                LOGGER.fine("Whitelist operation failed for " + targetName + ": " + whitelistError.getMessage());
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
            
            if ("bukkit".equalsIgnoreCase(whitelistMode) && whitelistJsonSync) {
                whitelistSyncManager.syncPluginToWhitelistJson();
            }
            
            whitelistSyncManager.syncWhitelistToServer();
            
            if (wsServer != null) {
                wsServer.broadcastMessage("{\"type\":\"user_update\"}");
            }
            
            if (ok) {
                sender.sendMessage("§a" + getMessage.apply("command.add_success").replace("{player}", targetName));
            } else {
                sender.sendMessage("§c" + getMessage.apply("command.add_failed"));
            }
        } catch (Exception e) {
            sender.sendMessage("§c" + getMessage.apply("command.add_failed") + ": " + e.getMessage());
        }
    }
    
    private void handleRemove(CommandSender sender, String targetName, String language) {
        try {
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
                sender.sendMessage("§a" + getMessage.apply("command.remove_success").replace("{player}", targetName));
                if (wsServer != null) wsServer.broadcastMessage("{\"type\":\"user_update\"}");
            } else {
                sender.sendMessage("§c" + getMessage.apply("command.remove_failed"));
            }
        } catch (Exception e) {
            sender.sendMessage("§c" + getMessage.apply("command.remove_failed") + ": " + e.getMessage());
        }
    }
    
    private void handleReload(CommandSender sender, String language) {
        sender.sendMessage("§aReloading plugin configuration...");
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("VerifyMC"), () -> {
            try {
                Bukkit.getPluginManager().disablePlugin(Bukkit.getPluginManager().getPlugin("VerifyMC"));
                Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin("VerifyMC"));
                sender.sendMessage("§aPlugin restart successful");
                sender.sendMessage("§7Note: /vmc reload can only reload partial plugin configurations. For a complete reload, please restart the server.");
            } catch (Exception e) {
                sender.sendMessage("§cPlugin restart failed: " + e.getMessage());
            }
        });
    }
    
    private void handlePort(CommandSender sender, String language) {
        int port = Bukkit.getPluginManager().getPlugin("VerifyMC").getConfig().getInt("web_port", 8080);
        sender.sendMessage("§a" + getMessage.apply("command.port_info").replace("{port}", String.valueOf(port)));
    }
    
    private void sendHelp(CommandSender sender, String language) {
        sender.sendMessage("§6=== VerifyMC " + getMessage.apply("command.help.title") + " ===\n");
        sender.sendMessage("§e/vmc help §7- " + getMessage.apply("command.help.help") + "\n");
        sender.sendMessage("§e/vmc port §7- " + getMessage.apply("command.help.port") + "\n");
        sender.sendMessage("§e/vmc reload §7- " + getMessage.apply("command.help.reload") + "\n");
        sender.sendMessage("§e/vmc add <" + getMessage.apply("command.help.player") + "> §7- " + getMessage.apply("command.help.add") + "\n");
        sender.sendMessage("§e/vmc remove <" + getMessage.apply("command.help.player") + "> §7- " + getMessage.apply("command.help.remove") + "\n");
    }
}
