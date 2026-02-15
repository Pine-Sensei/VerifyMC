package team.kitemc.verifymc.service;

import java.util.List;
import java.util.Map;

/**
 * User service interface for managing user data operations.
 * Provides methods for user retrieval, pagination, status updates, and deletion.
 */
public interface IUserService {
    
    /**
     * Retrieves a user by their unique identifier.
     * @param uuid The user's unique identifier
     * @return A map containing user data, or null if not found
     */
    Map<String, Object> getUserByUuid(String uuid);
    
    /**
     * Retrieves a user by their username.
     * @param username The username to search for
     * @return A map containing user data, or null if not found
     */
    Map<String, Object> getUserByUsername(String username);
    
    /**
     * Retrieves a user by their Discord ID.
     * @param discordId The Discord ID to search for
     * @return A map containing user data, or null if not found
     */
    Map<String, Object> getUserByDiscordId(String discordId);
    
    /**
     * Retrieves all users in the system.
     * @return A list of all users, each represented as a map
     */
    List<Map<String, Object>> getAllUsers();
    
    /**
     * Retrieves all users with pending status.
     * @return A list of pending users
     */
    List<Map<String, Object>> getPendingUsers();
    
    /**
     * Retrieves users with pagination support.
     * @param page The page number (1-based)
     * @param pageSize The number of users per page
     * @return A list of users for the specified page
     */
    List<Map<String, Object>> getUsersWithPagination(int page, int pageSize);
    
    /**
     * Retrieves users with pagination and search filtering.
     * @param page The page number (1-based)
     * @param pageSize The number of users per page
     * @param searchQuery The search query to filter users
     * @return A list of matching users for the specified page
     */
    List<Map<String, Object>> getUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery);
    
    /**
     * Retrieves approved users with pagination support.
     * @param page The page number (1-based)
     * @param pageSize The number of users per page
     * @return A list of approved users for the specified page
     */
    List<Map<String, Object>> getApprovedUsersWithPagination(int page, int pageSize);
    
    /**
     * Retrieves approved users with pagination and search filtering.
     * @param page The page number (1-based)
     * @param pageSize The number of users per page
     * @param searchQuery The search query to filter users
     * @return A list of matching approved users for the specified page
     */
    List<Map<String, Object>> getApprovedUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery);
    
    /**
     * Gets the total count of all users.
     * @return The total number of users
     */
    int getTotalUserCount();
    
    /**
     * Gets the total count of users matching the search query.
     * @param searchQuery The search query to filter users
     * @return The number of matching users
     */
    int getTotalUserCountWithSearch(String searchQuery);
    
    /**
     * Gets the total count of approved users.
     * @return The number of approved users
     */
    int getApprovedUserCount();
    
    /**
     * Gets the total count of approved users matching the search query.
     * @param searchQuery The search query to filter users
     * @return The number of matching approved users
     */
    int getApprovedUserCountWithSearch(String searchQuery);
    
    /**
     * Updates a user's status.
     * @param uuidOrName The user's UUID or username
     * @param status The new status (pending, approved, rejected, banned)
     * @return true if the update was successful
     */
    boolean updateUserStatus(String uuidOrName, String status);
    
    /**
     * Updates a user's password.
     * @param uuidOrName The user's UUID or username
     * @param password The new password
     * @return true if the update was successful
     */
    boolean updateUserPassword(String uuidOrName, String password);
    
    /**
     * Updates a user's Discord ID.
     * @param uuidOrName The user's UUID or username
     * @param discordId The Discord ID to associate
     * @return true if the update was successful
     */
    boolean updateUserDiscordId(String uuidOrName, String discordId);
    
    /**
     * Deletes a user from the system.
     * @param uuid The user's unique identifier
     * @return true if the deletion was successful
     */
    boolean deleteUser(String uuid);
    
    /**
     * Checks if a Discord ID is already linked to a user.
     * @param discordId The Discord ID to check
     * @return true if the Discord ID is already linked
     */
    boolean isDiscordIdLinked(String discordId);
    
    /**
     * Counts the number of users registered with a specific email.
     * @param email The email address to check
     * @return The number of users with this email
     */
    int countUsersByEmail(String email);
    
    /**
     * Checks if a username already exists.
     * @param username The username to check
     * @return true if the username exists
     */
    boolean isUsernameExists(String username);
    
    /**
     * Checks if a username conflicts with an existing username (case-insensitive).
     * @param username The username to check
     * @return true if there is a case conflict
     */
    boolean isUsernameCaseConflict(String username);
}
