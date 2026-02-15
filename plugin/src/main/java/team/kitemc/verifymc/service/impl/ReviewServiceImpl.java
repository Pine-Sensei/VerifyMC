package team.kitemc.verifymc.service.impl;

import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.db.AuditDao;
import team.kitemc.verifymc.db.AuditRecord;
import team.kitemc.verifymc.service.IReviewService;
import team.kitemc.verifymc.service.AuthmeService;
import team.kitemc.verifymc.mail.MailService;
import team.kitemc.verifymc.web.BusinessException;
import team.kitemc.verifymc.web.ErrorCode;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ReviewServiceImpl implements IReviewService {
    private static final Logger LOGGER = Logger.getLogger(ReviewServiceImpl.class.getName());
    
    private final UserDao userDao;
    private final AuditDao auditDao;
    private final AuthmeService authmeService;
    private final MailService mailService;
    private final String webRegisterUrl;
    
    public ReviewServiceImpl(UserDao userDao, AuditDao auditDao, 
                             AuthmeService authmeService, MailService mailService,
                             String webRegisterUrl) {
        this.userDao = userDao;
        this.auditDao = auditDao;
        this.authmeService = authmeService;
        this.mailService = mailService;
        this.webRegisterUrl = webRegisterUrl;
    }
    
    @Override
    public boolean approve(String uuid, String reviewerName) {
        Map<String, Object> user = getUserOrThrow(uuid);
        String username = (String) user.get("username");
        String email = (String) user.get("email");
        
        boolean updated = userDao.updateUserStatus(uuid, "approved");
        if (!updated) {
            throw new BusinessException("Failed to update user status", ErrorCode.REVIEW_FAILED.getCode());
        }
        
        syncToWhitelist(username, true);
        
        if (authmeService != null && authmeService.isAuthmeEnabled()) {
            String password = (String) user.get("password");
            if (password != null && !password.isEmpty()) {
                authmeService.registerToAuthme(username, password);
            }
        }
        
        recordAudit(uuid, username, "approve", reviewerName, null);
        
        sendNotification(email, username, true, null);
        
        LOGGER.info("Approved user: " + username + " by " + reviewerName);
        return true;
    }
    
    @Override
    public boolean reject(String uuid, String reason, String reviewerName) {
        Map<String, Object> user = getUserOrThrow(uuid);
        String username = (String) user.get("username");
        String email = (String) user.get("email");
        
        boolean updated = userDao.updateUserStatus(uuid, "rejected");
        if (!updated) {
            throw new BusinessException("Failed to update user status", ErrorCode.REVIEW_FAILED.getCode());
        }
        
        syncToWhitelist(username, false);
        
        recordAudit(uuid, username, "reject", reviewerName, reason);
        
        sendNotification(email, username, false, reason);
        
        LOGGER.info("Rejected user: " + username + " by " + reviewerName);
        return true;
    }
    
    @Override
    public boolean ban(String uuidOrName, String reason, String operatorName) {
        Map<String, Object> user = getUserByUuidOrName(uuidOrName);
        if (user == null) {
            throw new BusinessException("User not found", ErrorCode.USER_NOT_FOUND.getCode());
        }
        
        String uuid = (String) user.get("uuid");
        String username = (String) user.get("username");
        
        boolean updated = userDao.updateUserStatus(uuidOrName, "banned");
        if (!updated) {
            throw new BusinessException("Failed to ban user", ErrorCode.REVIEW_FAILED.getCode());
        }
        
        syncToWhitelist(username, false);
        
        recordAudit(uuid, username, "ban", operatorName, reason);
        
        LOGGER.info("Banned user: " + username + " by " + operatorName);
        return true;
    }
    
    @Override
    public boolean unban(String uuidOrName, String operatorName) {
        Map<String, Object> user = getUserByUuidOrName(uuidOrName);
        if (user == null) {
            throw new BusinessException("User not found", ErrorCode.USER_NOT_FOUND.getCode());
        }
        
        String uuid = (String) user.get("uuid");
        String username = (String) user.get("username");
        
        boolean updated = userDao.updateUserStatus(uuidOrName, "approved");
        if (!updated) {
            throw new BusinessException("Failed to unban user", ErrorCode.REVIEW_FAILED.getCode());
        }
        
        syncToWhitelist(username, true);
        
        recordAudit(uuid, username, "unban", operatorName, null);
        
        LOGGER.info("Unbanned user: " + username + " by " + operatorName);
        return true;
    }
    
    @Override
    public void sendNotification(String email, String username, boolean approved, String reason) {
        if (mailService == null || email == null || email.isEmpty()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                String subject = approved ? "Your registration has been approved" : "Your registration has been rejected";
                String body = buildNotificationBody(username, approved, reason);
                mailService.sendMail(email, subject, body);
            } catch (Exception e) {
                LOGGER.warning("Failed to send notification email to " + email + ": " + e.getMessage());
            }
        });
    }
    
    private Map<String, Object> getUserOrThrow(String uuid) {
        Map<String, Object> user = userDao.getUserByUuid(uuid);
        if (user == null) {
            throw new BusinessException("User not found", ErrorCode.USER_NOT_FOUND.getCode());
        }
        return user;
    }
    
    private Map<String, Object> getUserByUuidOrName(String uuidOrName) {
        Map<String, Object> user = userDao.getUserByUuid(uuidOrName);
        if (user == null) {
            user = userDao.getUserByUsername(uuidOrName);
        }
        return user;
    }
    
    private void syncToWhitelist(String username, boolean add) {
        LOGGER.info((add ? "Adding to" : "Removing from") + " whitelist: " + username);
    }
    
    private void recordAudit(String uuid, String username, String action, String operator, String reason) {
        if (auditDao != null) {
            String detail = reason != null ? reason : "";
            String target = username != null ? username : uuid;
            AuditRecord record = new AuditRecord(action, operator, target, detail, System.currentTimeMillis());
            auditDao.addAudit(record);
        }
    }
    
    private String buildNotificationBody(String username, boolean approved, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello ").append(username).append(",\n\n");
        
        if (approved) {
            sb.append("Your registration has been approved!\n");
            sb.append("You can now join the server.\n");
        } else {
            sb.append("Your registration has been rejected.\n");
            if (reason != null && !reason.isEmpty()) {
                sb.append("Reason: ").append(reason).append("\n");
            }
        }
        
        sb.append("\nVisit ").append(webRegisterUrl).append(" for more information.\n");
        return sb.toString();
    }
    
    private void validateReviewAction(String action) {
        if (action == null || (!action.equals("approve") && !action.equals("reject"))) {
            throw new BusinessException("Invalid review action", ErrorCode.INVALID_REVIEW_ACTION.getCode());
        }
    }
    
    private void validateUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            throw new BusinessException("UUID is required", ErrorCode.INVALID_UUID.getCode());
        }
    }
    
    private void ensureUserPending(String uuid) {
        Map<String, Object> user = userDao.getUserByUuid(uuid);
        if (user == null) {
            throw new BusinessException("User not found", ErrorCode.USER_NOT_FOUND.getCode());
        }
        String status = (String) user.get("status");
        if (!"pending".equals(status)) {
            throw new BusinessException("User is not pending review", ErrorCode.INVALID_REQUEST.getCode());
        }
    }
}
