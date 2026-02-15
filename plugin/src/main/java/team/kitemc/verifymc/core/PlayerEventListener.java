package team.kitemc.verifymc.core;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import team.kitemc.verifymc.db.UserDao;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class PlayerEventListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger(PlayerEventListener.class.getName());
    
    private final UserDao userDao;
    private final String webRegisterUrl;
    private final Supplier<List<String>> bypassIpsSupplier;
    private final Supplier<Boolean> debugSupplier;
    
    public PlayerEventListener(UserDao userDao, 
                              String webRegisterUrl,
                              Supplier<List<String>> bypassIpsSupplier,
                              Supplier<Boolean> debugSupplier) {
        this.userDao = userDao;
        this.webRegisterUrl = webRegisterUrl;
        this.bypassIpsSupplier = bypassIpsSupplier;
        this.debugSupplier = debugSupplier;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        String playerName = event.getPlayer().getName();
        String ip = event.getAddress() != null ? event.getAddress().getHostAddress() : "";
        
        List<String> bypassIps = bypassIpsSupplier.get();
        if (bypassIps != null && bypassIps.contains(ip)) {
            debugLog("Bypassed whitelist check for IP: " + ip);
            return;
        }
        
        Map<String, Object> user = userDao != null ? userDao.getAllUsers().stream()
            .filter(u -> playerName.equalsIgnoreCase((String) u.get("username")) && "approved".equals(u.get("status")))
            .findFirst().orElse(null) : null;
        
        if (user == null) {
            String msg = "§c[ VerifyMC ]\n§7Please visit §a" + webRegisterUrl + " §7to register";
            event.disallow(Result.KICK_WHITELIST, msg);
            debugLog("Blocked unregistered player: " + playerName + " from IP: " + ip);
        } else {
            event.setResult(Result.ALLOWED);
            debugLog("Allowed registered player: " + playerName + " (Status: approved)");
        }
    }
    
    private void debugLog(String msg) {
        if (debugSupplier != null && debugSupplier.get()) {
            LOGGER.info("[DEBUG] " + msg);
        }
    }
}
