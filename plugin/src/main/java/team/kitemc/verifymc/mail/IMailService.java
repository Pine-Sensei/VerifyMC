package team.kitemc.verifymc.mail;

public interface IMailService {
    boolean sendCode(String to, String subject, String code, String language);
    boolean sendCode(String to, String subject, String code);
    boolean isUserNotificationEnabled();
    boolean isNotifyOnApprove();
    boolean isNotifyOnReject();
    boolean sendReviewResultNotification(String email, String username, boolean approved, String reason, String language);
    void sendMail(String to, String subject, String body);
}
