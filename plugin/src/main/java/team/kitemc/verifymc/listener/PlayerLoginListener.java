package team.kitemc.verifymc.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import team.kitemc.verifymc.core.PluginContext;

import java.util.Map;

/**
 * Handles player login events — checks if the player is whitelisted
 * and their registration status. Replaces the inline Listener in the
 * original VerifyMC god class.
 */
public class PlayerLoginListener implements Listener {
    private final PluginContext ctx;

    public PlayerLoginListener(PluginContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String username = player.getName();

        ctx.debugLog("PlayerLogin: uuid=" + uuid + " username=" + username);

        // Check if user is registered
        Map<String, Object> user = ctx.getUserDao().getUserByUuid(uuid);
        if (user == null) {
            // Also check by username
            user = ctx.getUserDao().getUserByUsername(username);
        }

        if (user == null) {
            // Not registered — allow Bukkit whitelist to handle naturally
            return;
        }

        String status = (String) user.getOrDefault("status", "");

        switch (status) {
            case "approved" -> {
                // Ensure they're on the whitelist
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
