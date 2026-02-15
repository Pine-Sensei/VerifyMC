package team.kitemc.verifymc.service;

import java.util.List;
import java.util.Map;

/**
 * Admin service interface for administrative operations.
 * Provides methods for authentication, password management, and system statistics.
 */
public interface IAdminService {
    
    /**
     * Verifies an admin authentication token.
     * @param token The token to verify
     * @return true if the token is valid and not expired
     */
    boolean verifyToken(String token);
    
    /**
     * Generates a new admin authentication token.
     * @return The generated token
     */
    String generateToken();
    
    /**
     * Changes a user's password.
     * @param uuidOrName The user's UUID or username
     * @param newPassword The new password
     * @return true if the password change was successful
     */
    boolean changePassword(String uuidOrName, String newPassword);
    
    /**
     * Retrieves system statistics.
     * @return A map containing various system statistics
     */
    Map<String, Object> getSystemStats();
    
    /**
     * Reloads the plugin configuration.
     * @return true if the reload was successful
     */
    boolean reloadConfig();
}
