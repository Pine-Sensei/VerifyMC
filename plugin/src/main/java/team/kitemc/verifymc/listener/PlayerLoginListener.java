package team.kitemc.verifymc.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import team.kitemc.verifymc.PluginContext;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.domain.service.UserService;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import java.util.List;
import java.util.Map;

public class PlayerLoginListener implements Listener {
    private final PluginContext context;
    private final ConfigurationService configService;

    public PlayerLoginListener(PluginContext context) {
        this.context = context;
        this.configService = context.getConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String ip = event.getAddress() != null ? event.getAddress().getHostAddress() : "";

        List<String> bypassIps = configService.getStringList("whitelist_bypass_ips");
        if (bypassIps.contains(ip)) {
            context.debugLog("Bypassed whitelist check for IP: " + ip);
            return;
        }

        boolean isApproved = checkPlayerApproval(player.getName());

        if (!isApproved) {
            String url = configService.getString("web_register_url", "https://yourdomain.com/");
            String msg = "§c[ VerifyMC ]\n§7Please visit §a" + url + " §7to register";

            event.disallow(Result.KICK_WHITELIST, msg);
            context.debugLog("Blocked unregistered player: " + player.getName() + " from IP: " + ip);
        } else {
            event.setResult(Result.ALLOWED);
            context.debugLog("Allowed registered player: " + player.getName() + " (Status: approved)");
        }
    }

    private boolean checkPlayerApproval(String playerName) {
        try {
            UserService userService = context.getService(UserService.class);
            if (userService != null) {
                return userService.getUserByUsername(playerName)
                        .map(user -> user.getStatus() == UserStatus.APPROVED)
                        .orElse(false);
            }

            UserDao userDao = context.getService(UserDao.class);
            if (userDao != null) {
                List<Map<String, Object>> users = userDao.getAllUsers();
                for (Map<String, Object> user : users) {
                    String username = (String) user.get("username");
                    String status = (String) user.get("status");
                    if (playerName.equalsIgnoreCase(username) && "approved".equals(status)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            context.getLogger().warning("Error checking player approval: " + e.getMessage());
            return false;
        }
    }
}
