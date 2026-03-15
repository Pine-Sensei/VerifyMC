package team.kitemc.verifymc.web;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import team.kitemc.verifymc.core.PluginContext;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ReviewWebSocketServer extends WebSocketServer {
    private static final int MAX_CLIENTS = 50;
    private static final int CLOSE_CODE_POLICY_VIOLATION = 1008;

    private final Set<WebSocket> clients = Collections.synchronizedSet(new HashSet<>());
    private final boolean debug;
    private final org.bukkit.plugin.Plugin plugin;
    private final WebAuthHelper authHelper;

    public ReviewWebSocketServer(int port, PluginContext context) {
        super(new InetSocketAddress(port));
        this.plugin = context.getPlugin();
        this.debug = context.isDebug();
        this.authHelper = context.getWebAuthHelper();
    }

    /**
     * Compatible with old constructor (no authentication)
     * @deprecated Use ReviewWebSocketServer(int, PluginContext) instead
     */
    @Deprecated
    public ReviewWebSocketServer(int port, org.bukkit.plugin.Plugin plugin) {
        super(new InetSocketAddress(port));
        this.plugin = plugin;
        this.debug = plugin != null && plugin.getConfig().getBoolean("debug", false);
        this.authHelper = null;
    }

    /**
     * Compatible with old constructor (no authentication)
     * @deprecated Use ReviewWebSocketServer(int, PluginContext) instead
     */
    @Deprecated
    public ReviewWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        this.plugin = null;
        this.debug = false;
        this.authHelper = null;
    }

    private void debugLog(String msg) {
        if (debug && plugin != null) {
            plugin.getLogger().info("[DEBUG] ReviewWebSocketServer: " + msg);
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        debugLog("WebSocket connection opened: " + conn.getRemoteSocketAddress());

        // Check max clients limit
        if (clients.size() >= MAX_CLIENTS) {
            debugLog("Connection rejected: max clients limit reached (" + MAX_CLIENTS + ")");
            conn.close(CLOSE_CODE_POLICY_VIOLATION, "Max clients limit reached");
            return;
        }

        // Authentication check
        if (authHelper != null) {
            String token = extractToken(handshake);
            if (token == null) {
                debugLog("Connection rejected: no token provided");
                conn.close(CLOSE_CODE_POLICY_VIOLATION, "Authentication required");
                return;
            }

            if (!authHelper.validateToken(token)) {
                debugLog("Connection rejected: invalid or expired token");
                conn.close(CLOSE_CODE_POLICY_VIOLATION, "Invalid or expired token");
                return;
            }

            debugLog("Connection authenticated successfully");
        }

        clients.add(conn);
        debugLog("Total clients connected: " + clients.size());
    }

    /**
     * Extract token from handshake request.
     * First tries Authorization header, then query parameter.
     */
    private String extractToken(ClientHandshake handshake) {
        // Try Authorization header first
        String authHeader = handshake.getFieldValue("Authorization");
        if (authHeader != null && !authHeader.isEmpty()) {
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            // If it's not a Bearer token, treat the whole header as token
            return authHeader;
        }

        // Try query parameter
        String resourceDescriptor = handshake.getResourceDescriptor();
        if (resourceDescriptor != null && resourceDescriptor.contains("?")) {
            try {
                String query = resourceDescriptor.substring(resourceDescriptor.indexOf("?") + 1);
                String[] params = query.split("&");
                for (String param : params) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && "token".equals(pair[0])) {
                        return URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name());
                    }
                }
            } catch (Exception e) {
                debugLog("Error parsing query parameters: " + e.getMessage());
            }
        }

        return null;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        debugLog("WebSocket connection closed: " + conn.getRemoteSocketAddress() + ", code: " + code + ", reason: " + reason);
        clients.remove(conn);
        debugLog("Remaining clients: " + clients.size());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        debugLog("Received message from " + conn.getRemoteSocketAddress() + ": " + message);
        // Can handle messages from frontend as needed
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        debugLog("WebSocket error: " + ex.getMessage());
        if (conn != null) {
            debugLog("Error on connection: " + conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onStart() {
        debugLog("WebSocket server started on port: " + getPort());
    }

    public void enableSsl(SSLContext sslContext) {
        setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
        debugLog("WebSocket SSL enabled");
    }

    public void broadcastMessage(String message) {
        debugLog("Broadcasting message to " + clients.size() + " clients: " + message);
        synchronized (clients) {
            int sentCount = 0;
            for (WebSocket ws : clients) {
                if (ws.isOpen()) {
                    ws.send(message);
                    sentCount++;
                }
            }
            debugLog("Message sent to " + sentCount + " clients");
        }
    }
} 
