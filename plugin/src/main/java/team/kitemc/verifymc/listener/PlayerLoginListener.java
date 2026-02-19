package team.kitemc.verifymc.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import team.kitemc.verifymc.core.PluginContext;

import java.util.Map;

public class PlayerLoginListener implements Listener {
    private final PluginContext ctx;

    public PlayerLoginListener(PluginContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String username = player.getName();

        ctx.debugLog("PlayerLogin: username=" + username);

        Map<String, Object> user = ctx.getUserDao().getUserByUsername(username);

        if (user == null) {
            return;
        }

        String status = (String) user.getOrDefault("status", "");

        switch (status) {
            case "approved" -> {
                ctx.debugLog("User " + username + " is approved, allowing login.");
            }
            case "pending" -> {
                String msg = ctx.getMessage("login.pending", ctx.getConfigManager().getLanguage());
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, msg);
                ctx.debugLog("User " + username + " is pending, kicking.");
            }
            case "rejected" -> {
                String msg = ctx.getMessage("login.rejected", ctx.getConfigManager().getLanguage());
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, msg);
                ctx.debugLog("User " + username + " is rejected, kicking.");
            }
            case "banned" -> {
                String msg = ctx.getMessage("login.banned", ctx.getConfigManager().getLanguage());
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, msg);
                ctx.debugLog("User " + username + " is banned, kicking.");
            }
            default -> {
                ctx.debugLog("User " + username + " has unknown status: " + status);
            }
        }
    }
}
