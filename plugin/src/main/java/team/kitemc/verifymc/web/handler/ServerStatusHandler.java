package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Server;
import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Collection;
import org.bukkit.entity.Player;

public class ServerStatusHandler implements HttpHandler {
    private final PluginContext ctx;

    public ServerStatusHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        JSONObject resp = new JSONObject();
        
        try {
            Server server = ctx.getPlugin().getServer();
            
            resp.put("success", true);
            
            JSONObject data = new JSONObject();
            data.put("online", true);
            
            JSONObject players = new JSONObject();
            players.put("online", server.getOnlinePlayers().size());
            players.put("max", server.getMaxPlayers());
            
            String authenticatedUser = AdminAuthUtil.getAuthenticatedUserQuietly(exchange, ctx);
            if (authenticatedUser != null) {
                Collection<? extends Player> onlinePlayers = server.getOnlinePlayers();
                if (!onlinePlayers.isEmpty()) {
                    JSONArray playerList = new JSONArray();
                    for (Player player : onlinePlayers) {
                        JSONObject playerInfo = new JSONObject();
                        playerInfo.put("name", player.getName());
                        playerInfo.put("uuid", player.getUniqueId().toString());
                        playerList.put(playerInfo);
                    }
                    players.put("list", playerList);
                }
            }
            data.put("players", players);
            
            data.put("version", server.getBukkitVersion());
            
            double tps = getTPS(server);
            data.put("tps", tps);
            
            JSONObject memory = new JSONObject();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedBytes = memoryBean.getHeapMemoryUsage().getUsed();
            long maxBytes = memoryBean.getHeapMemoryUsage().getMax();
            memory.put("used", bytesToMB(usedBytes));
            memory.put("max", bytesToMB(maxBytes));
            data.put("memory", memory);
            
            String motd = server.getMotd();
            if (motd != null) {
                motd = motd.replace("\n", " ").trim();
            }
            data.put("motd", motd != null ? motd : "");
            
            resp.put("data", data);
        } catch (Exception e) {
            ctx.debugLog("Error getting server status: " + e.getMessage());
            resp.put("success", false);
            resp.put("message", "Failed to get server status");
        }
        
        WebResponseHelper.sendJson(exchange, resp);
    }
    
    private double getTPS(Server server) {
        try {
            java.lang.reflect.Method getTPSMethod = server.getClass().getMethod("getTPS");
            if (getTPSMethod != null) {
                double[] tpsArray = (double[]) getTPSMethod.invoke(server);
                if (tpsArray != null && tpsArray.length > 0) {
                    return Math.round(tpsArray[0] * 100.0) / 100.0;
                }
            }
        } catch (Exception e) {
            ctx.debugLog("Could not retrieve TPS: " + e.getMessage());
        }
        return -1.0;
    }
    
    private long bytesToMB(long bytes) {
        return bytes / (1024 * 1024);
    }
}
