package team.kitemc.verifymc.db;

import java.util.List;
import java.util.Map;

public interface UserDao {
    boolean registerUser(String username, String email, String status);

    boolean registerUser(String username, String email, String status,
                         Integer questionnaireScore, Boolean questionnairePassed,
                         String questionnaireReviewSummary, Long questionnaireScoredAt);
    
    boolean registerUser(String username, String email, String status, String password);

    boolean registerUser(String username, String email, String status, String password,
                         Integer questionnaireScore, Boolean questionnairePassed,
                         String questionnaireReviewSummary, Long questionnaireScoredAt);
    
    boolean updateUserStatus(String username, String status);
    
    /**
     * Updates a user's password using a plaintext password input.
     * Implementations are responsible for hashing before persistence.
     */
    boolean updateUserPassword(String username, String plainPassword);

    boolean updateUserEmail(String username, String email);
    
    List<Map<String, Object>> getAllUsers();
    
    List<Map<String, Object>> getUsersWithPagination(int page, int pageSize);
    
    int getTotalUserCount();
    
    List<Map<String, Object>> getUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery);
    
    int getTotalUserCountWithSearch(String searchQuery);
    
    int getApprovedUserCount();
    
    int getApprovedUserCountWithSearch(String searchQuery);
    
    List<Map<String, Object>> getApprovedUsersWithPagination(int page, int pageSize);
    
    List<Map<String, Object>> getApprovedUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery);
    
    Map<String, Object> getUserByUsername(String username);

    Map<String, Object> getUserByUsernameExact(String username);

    Map<String, Object> getUserByEmail(String email);
    
    boolean deleteUser(String username);
    
    void save();
    
    int countUsersByEmail(String email);
    
    List<Map<String, Object>> getPendingUsers();
    
    boolean updateUserDiscordId(String username, String discordId);
    
    Map<String, Object> getUserByDiscordId(String discordId);
    
    boolean isDiscordIdLinked(String discordId);

    default boolean updateUserStatus(String username, String status, String operator) {
        return updateUserStatus(username, status);
    }

    default List<Map<String, Object>> getUsers(int page, int size, String search, String status) {
        if (status == null || status.isEmpty() || "all".equalsIgnoreCase(status)) {
            return getUsersWithPaginationAndSearch(page, size, search);
        }
        if ("approved".equalsIgnoreCase(status)) {
            return getApprovedUsersWithPaginationAndSearch(page, size, search);
        }
        if ("pending".equalsIgnoreCase(status)) {
            return getPendingUsers();
        }
        return getUsersByStatus(status);
    }

    default int getTotalUsers(String search, String status) {
        if (status == null || status.isEmpty() || "all".equalsIgnoreCase(status)) {
            return getTotalUserCountWithSearch(search);
        }
        if ("approved".equalsIgnoreCase(status)) {
            return getApprovedUserCountWithSearch(search);
        }
        return getTotalUserCountWithSearch(search);
    }

    default boolean banUser(String username) {
        return updateUserStatus(username, "banned");
    }

    default boolean unbanUser(String username) {
        return updateUserStatus(username, "approved");
    }

    default List<Map<String, Object>> getUsersByStatus(String status) {
        List<Map<String, Object>> allUsers = getAllUsers();
        return allUsers.stream()
            .filter(u -> status.equalsIgnoreCase((String) u.get("status")))
            .collect(java.util.stream.Collectors.toList());
    }

    default boolean updatePassword(String username, String plainPassword) {
        return updateUserPassword(username, plainPassword);
    }

    /**
     * Closes any resources held by this DAO (e.g., database connections).
     * Default implementation does nothing.
     */
    default void close() {
        // Default: no-op
    }
} 
