package team.kitemc.verifymc.db;

import java.util.List;
import java.util.Map;

public interface UserDao {
    boolean registerUser(String uuid, String username, String email, String status);

    boolean registerUser(String uuid, String username, String email, String status,
                         Integer questionnaireScore, Boolean questionnairePassed,
                         String questionnaireReviewSummary, Long questionnaireScoredAt);
    
    boolean registerUser(String uuid, String username, String email, String status, String password);

    boolean registerUser(String uuid, String username, String email, String status, String password,
                         Integer questionnaireScore, Boolean questionnairePassed,
                         String questionnaireReviewSummary, Long questionnaireScoredAt);
    
    boolean updateUserStatus(String uuidOrName, String status);
    
    boolean updateUserPassword(String uuidOrName, String password);

    boolean updateUserEmail(String uuidOrName, String email);
    
    List<Map<String, Object>> getAllUsers();
    
    List<Map<String, Object>> getUsersWithPagination(int page, int pageSize);
    
    int getTotalUserCount();
    
    List<Map<String, Object>> getUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery);
    
    int getTotalUserCountWithSearch(String searchQuery);
    
    int getApprovedUserCount();
    
    int getApprovedUserCountWithSearch(String searchQuery);
    
    List<Map<String, Object>> getApprovedUsersWithPagination(int page, int pageSize);
    
    List<Map<String, Object>> getApprovedUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery);
    
    Map<String, Object> getUserByUuid(String uuid);
    
    Map<String, Object> getUserByUsername(String username);
    
    Map<String, Object> getUserByUsernameExact(String username);
    
    boolean deleteUser(String uuidOrName);
    
    void save();
    
    int countUsersByEmail(String email);
    
    List<Map<String, Object>> getPendingUsers();
    
    boolean updateUserDiscordId(String uuidOrName, String discordId);
    
    Map<String, Object> getUserByDiscordId(String discordId);
    
    boolean isDiscordIdLinked(String discordId);

    default boolean updateUserStatus(String uuidOrName, String status, String operator) {
        return updateUserStatus(uuidOrName, status);
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

    default boolean banUser(String uuidOrName) {
        return updateUserStatus(uuidOrName, "banned");
    }

    default boolean unbanUser(String uuidOrName) {
        return updateUserStatus(uuidOrName, "approved");
    }

    default List<Map<String, Object>> getUsersByStatus(String status) {
        List<Map<String, Object>> allUsers = getAllUsers();
        return allUsers.stream()
            .filter(u -> status.equalsIgnoreCase((String) u.get("status")))
            .collect(java.util.stream.Collectors.toList());
    }

    default boolean updatePassword(String uuidOrName, String password) {
        return updateUserPassword(uuidOrName, password);
    }
} 
