package team.kitemc.verifymc.service;

import java.util.Map;

/**
 * Review service interface for handling user application review operations.
 * Provides methods for approving, rejecting, banning, and notifying users.
 */
public interface IReviewService {
    
    /**
     * Approves a pending user application.
     * @param uuid The user's unique identifier
     * @param reviewerName The name of the reviewer performing the approval
     * @return true if the approval was successful
     */
    boolean approve(String uuid, String reviewerName);
    
    /**
     * Rejects a pending user application.
     * @param uuid The user's unique identifier
     * @param reason The reason for rejection
     * @param reviewerName The name of the reviewer performing the rejection
     * @return true if the rejection was successful
     */
    boolean reject(String uuid, String reason, String reviewerName);
    
    /**
     * Bans a user from the system.
     * @param uuidOrName The user's UUID or username
     * @param reason The reason for the ban
     * @param operatorName The name of the operator performing the ban
     * @return true if the ban was successful
     */
    boolean ban(String uuidOrName, String reason, String operatorName);
    
    /**
     * Removes a ban from a user.
     * @param uuidOrName The user's UUID or username
     * @param operatorName The name of the operator performing the unban
     * @return true if the unban was successful
     */
    boolean unban(String uuidOrName, String operatorName);
    
    /**
     * Sends a notification email to a user about their application status.
     * @param email The user's email address
     * @param username The user's username
     * @param approved Whether the application was approved
     * @param reason The reason (for rejections)
     */
    void sendNotification(String email, String username, boolean approved, String reason);
}
