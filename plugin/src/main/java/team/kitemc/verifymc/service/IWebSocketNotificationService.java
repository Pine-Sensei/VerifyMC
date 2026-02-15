package team.kitemc.verifymc.service;

public interface IWebSocketNotificationService {
    void notifyReviewComplete(String uuid, String username, String action, String reason);
    void notifyNewRegistration(String uuid, String username);
    void notifyUserBanned(String uuid, String username, String reason);
}
