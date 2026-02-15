package team.kitemc.verifymc.service.impl;

import team.kitemc.verifymc.db.User;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.service.IUserService;
import team.kitemc.verifymc.web.BusinessException;
import team.kitemc.verifymc.web.ErrorCode;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class UserServiceImpl implements IUserService {
    private static final Logger LOGGER = Logger.getLogger(UserServiceImpl.class.getName());
    private final UserDao userDao;
    
    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }
    
    @Override
    public Map<String, Object> getUserByUuid(String uuid) {
        validateUuid(uuid);
        return userDao.getUserByUuid(uuid);
    }
    
    @Override
    public Map<String, Object> getUserByUsername(String username) {
        validateUsername(username);
        return userDao.getUserByUsername(username);
    }
    
    @Override
    public Map<String, Object> getUserByDiscordId(String discordId) {
        if (discordId == null || discordId.isEmpty()) {
            throw new BusinessException("Discord ID is required", ErrorCode.INVALID_REQUEST.getCode());
        }
        return userDao.getUserByDiscordId(discordId);
    }
    
    @Override
    public List<Map<String, Object>> getAllUsers() {
        return userDao.getAllUsers();
    }
    
    @Override
    public List<Map<String, Object>> getPendingUsers() {
        return userDao.getPendingUsers();
    }
    
    @Override
    public List<Map<String, Object>> getUsersWithPagination(int page, int pageSize) {
        validatePagination(page, pageSize);
        return userDao.getUsersWithPagination(page, pageSize);
    }
    
    @Override
    public List<Map<String, Object>> getUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery) {
        validatePagination(page, pageSize);
        return userDao.getUsersWithPaginationAndSearch(page, pageSize, searchQuery);
    }
    
    @Override
    public List<Map<String, Object>> getApprovedUsersWithPagination(int page, int pageSize) {
        validatePagination(page, pageSize);
        return userDao.getApprovedUsersWithPagination(page, pageSize);
    }
    
    @Override
    public List<Map<String, Object>> getApprovedUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery) {
        validatePagination(page, pageSize);
        return userDao.getApprovedUsersWithPaginationAndSearch(page, pageSize, searchQuery);
    }
    
    @Override
    public int getTotalUserCount() {
        return userDao.getTotalUserCount();
    }
    
    @Override
    public int getTotalUserCountWithSearch(String searchQuery) {
        return userDao.getTotalUserCountWithSearch(searchQuery);
    }
    
    @Override
    public int getApprovedUserCount() {
        return userDao.getApprovedUserCount();
    }
    
    @Override
    public int getApprovedUserCountWithSearch(String searchQuery) {
        return userDao.getApprovedUserCountWithSearch(searchQuery);
    }
    
    @Override
    public boolean updateUserStatus(String uuidOrName, String status) {
        validateUuidOrName(uuidOrName);
        validateStatus(status);
        boolean result = userDao.updateUserStatus(uuidOrName, status);
        if (result) {
            LOGGER.info("Updated user status: " + uuidOrName + " -> " + status);
        }
        return result;
    }
    
    @Override
    public boolean updateUserPassword(String uuidOrName, String password) {
        validateUuidOrName(uuidOrName);
        if (password == null || password.isEmpty()) {
            throw new BusinessException("Password is required", ErrorCode.INVALID_PASSWORD.getCode());
        }
        return userDao.updateUserPassword(uuidOrName, password);
    }
    
    @Override
    public boolean updateUserDiscordId(String uuidOrName, String discordId) {
        validateUuidOrName(uuidOrName);
        return userDao.updateUserDiscordId(uuidOrName, discordId);
    }
    
    @Override
    public boolean deleteUser(String uuid) {
        validateUuid(uuid);
        boolean result = userDao.deleteUser(uuid);
        if (result) {
            LOGGER.info("Deleted user: " + uuid);
        }
        return result;
    }
    
    @Override
    public boolean isDiscordIdLinked(String discordId) {
        return userDao.isDiscordIdLinked(discordId);
    }
    
    @Override
    public int countUsersByEmail(String email) {
        if (email == null || email.isEmpty()) return 0;
        return userDao.countUsersByEmail(email);
    }
    
    @Override
    public boolean isUsernameExists(String username) {
        return getUserByUsername(username) != null;
    }
    
    @Override
    public boolean isUsernameCaseConflict(String username) {
        Map<String, Object> user = getUserByUsername(username);
        if (user == null) return false;
        String existingUsername = (String) user.get("username");
        return !existingUsername.equals(username);
    }
    
    public boolean approveUser(String uuidOrName) {
        return updateUserStatus(uuidOrName, "approved");
    }
    
    public boolean rejectUser(String uuidOrName) {
        return updateUserStatus(uuidOrName, "rejected");
    }
    
    public boolean banUser(String uuidOrName) {
        return updateUserStatus(uuidOrName, "banned");
    }
    
    public boolean unbanUser(String uuidOrName) {
        return updateUserStatus(uuidOrName, "approved");
    }
    
    public boolean isApproved(String uuidOrName) {
        Map<String, Object> user = getUserByUuidOrName(uuidOrName);
        return user != null && "approved".equals(user.get("status"));
    }
    
    public boolean isBanned(String uuidOrName) {
        Map<String, Object> user = getUserByUuidOrName(uuidOrName);
        return user != null && "banned".equals(user.get("status"));
    }
    
    public boolean isPending(String uuidOrName) {
        Map<String, Object> user = getUserByUuidOrName(uuidOrName);
        return user != null && "pending".equals(user.get("status"));
    }
    
    private Map<String, Object> getUserByUuidOrName(String uuidOrName) {
        Map<String, Object> user = userDao.getUserByUuid(uuidOrName);
        if (user == null) {
            user = userDao.getUserByUsername(uuidOrName);
        }
        return user;
    }
    
    private User getUserEntityByUuidOrName(String uuidOrName) {
        User user = userDao.getUserEntityByUuid(uuidOrName);
        if (user == null) {
            user = userDao.getUserEntityByUsername(uuidOrName);
        }
        return user;
    }
    
    private void ensureUserExists(String uuidOrName) {
        Map<String, Object> user = getUserByUuidOrName(uuidOrName);
        if (user == null) {
            throw new BusinessException("User not found: " + uuidOrName, ErrorCode.USER_NOT_FOUND.getCode());
        }
    }
    
    private void validateUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            throw new BusinessException("UUID is required", ErrorCode.INVALID_UUID.getCode());
        }
    }
    
    private void validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new BusinessException("Username is required", ErrorCode.INVALID_USERNAME.getCode());
        }
    }
    
    private void validateUuidOrName(String uuidOrName) {
        if (uuidOrName == null || uuidOrName.isEmpty()) {
            throw new BusinessException("UUID or username is required", ErrorCode.INVALID_REQUEST.getCode());
        }
    }
    
    private void validateStatus(String status) {
        if (status == null || status.isEmpty()) {
            throw new BusinessException("Status is required", ErrorCode.INVALID_REQUEST.getCode());
        }
        if (!status.equals("pending") && !status.equals("approved") && 
            !status.equals("rejected") && !status.equals("banned")) {
            throw new BusinessException("Invalid status: " + status, ErrorCode.INVALID_REQUEST.getCode());
        }
    }
    
    private void validatePagination(int page, int pageSize) {
        if (page < 1) {
            throw new BusinessException("Page must be >= 1", ErrorCode.INVALID_REQUEST.getCode());
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException("Page size must be between 1 and 100", ErrorCode.INVALID_REQUEST.getCode());
        }
    }
}
