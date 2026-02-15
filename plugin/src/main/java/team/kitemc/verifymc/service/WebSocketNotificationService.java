package team.kitemc.verifymc.service;

import team.kitemc.verifymc.web.ReviewWebSocketServer;
import org.json.JSONObject;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

public class WebSocketNotificationService implements IWebSocketNotificationService {
    private static final Logger LOGGER = Logger.getLogger(WebSocketNotificationService.class.getName());
    
    private final ReviewWebSocketServer webSocketServer;
    
    public WebSocketNotificationService(ReviewWebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
    }
    
    public void notifyReviewComplete(String uuid, String username, String action, String reason) {
        if (webSocketServer == null) return;
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "review_complete");
        message.put("uuid", uuid);
        message.put("username", username);
        message.put("action", action);
        message.put("reason", reason);
        message.put("timestamp", System.currentTimeMillis());
        
        JSONObject jsonMessage = new JSONObject(message);
        webSocketServer.broadcastMessage(jsonMessage.toString());
        LOGGER.info("WebSocket notification sent for user: " + username);
    }
    
    public void notifyNewRegistration(String uuid, String username) {
        if (webSocketServer == null) return;
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "new_registration");
        message.put("uuid", uuid);
        message.put("username", username);
        message.put("timestamp", System.currentTimeMillis());
        
        JSONObject jsonMessage = new JSONObject(message);
        webSocketServer.broadcastMessage(jsonMessage.toString());
        LOGGER.info("WebSocket notification sent for new registration: " + username);
    }
    
    public void notifyUserBanned(String uuid, String username, String reason) {
        if (webSocketServer == null) return;
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "user_banned");
        message.put("uuid", uuid);
        message.put("username", username);
        message.put("reason", reason);
        message.put("timestamp", System.currentTimeMillis());
        
        JSONObject jsonMessage = new JSONObject(message);
        webSocketServer.broadcastMessage(jsonMessage.toString());
    }
}
