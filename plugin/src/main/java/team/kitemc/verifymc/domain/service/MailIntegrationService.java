package team.kitemc.verifymc.domain.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

public class MailIntegrationService {
    private final ConfigurationService configService;
    private final boolean debug;

    private Session session;
    private String from;

    public MailIntegrationService(ConfigurationService configService) {
        this.configService = configService;
        this.debug = configService.isDebug();
        init();
    }

    private void init() {
        debugLog("Initializing MailIntegrationService");
        String host = configService.getString("smtp.host", "smtp.qq.com");
        int port = configService.getInt("smtp.port", 587);
        String username = configService.getString("smtp.username", "");
        String password = configService.getString("smtp.password", "");
        from = configService.getString("smtp.from", username);
        boolean enableSsl = configService.getBoolean("smtp.enable_ssl", true);

        debugLog("SMTP Configuration: host=" + host + ", port=" + port + ", username=" + username + ", enableSsl=" + enableSsl);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        if (enableSsl) {
            if (port == 465) {
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }
        }
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                debugLog("Authenticating with SMTP server");
                return new PasswordAuthentication(username, password);
            }
        });
        debugLog("MailIntegrationService initialized successfully");
    }

    public boolean isConfigured() {
        String username = configService.getString("smtp.username", "");
        String password = configService.getString("smtp.password", "");
        return username != null && !username.isEmpty() && password != null && !password.isEmpty();
    }

    public boolean sendCode(String email, String code, String language) {
        debugLog("sendCode called: to=" + email + ", code=" + code + ", language=" + language);
        try {
            String lang = (language != null && !language.isEmpty()) ? language : configService.getLanguage();
            debugLog("Using language: " + lang);

            File emailDir = new File(configService.getDataFolder(), "email");
            File templateFile = new File(emailDir, "verify_code_" + lang + ".html");
            String content;
            if (templateFile.exists()) {
                debugLog("Using custom template: " + templateFile.getAbsolutePath());
                content = new String(Files.readAllBytes(templateFile.toPath()), StandardCharsets.UTF_8);
                content = content.replace("{code}", code);
            } else {
                debugLog("Using default template");
                content = getDefaultVerifyCodeTemplate(lang, code);
            }

            String subject = "zh".equals(lang) ? "[VerifyMC] 验证码" : "[VerifyMC] Verification Code";

            debugLog("Creating email message");
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            debugLog("Sending email");
            Transport.send(message);
            debugLog("Email sent successfully");
            return true;
        } catch (Exception e) {
            debugLog("Failed to send email: " + e.getMessage());
            configService.getLogger().warning("Failed to send verification email: " + e.getMessage());
            return false;
        }
    }

    public boolean sendReviewResultNotification(String email, String username, boolean approved, String reason, String language) {
        if (!isUserNotificationEnabled()) {
            debugLog("User notification is disabled");
            return false;
        }

        if (approved && !isNotifyOnApprove()) {
            debugLog("Notification on approval is disabled");
            return false;
        }

        if (!approved && !isNotifyOnReject()) {
            debugLog("Notification on rejection is disabled");
            return false;
        }

        if (email == null || email.trim().isEmpty()) {
            debugLog("User email is empty");
            return false;
        }

        debugLog("sendReviewResultNotification called: email=" + email + ", username=" + username + ", approved=" + approved + ", language=" + language);

        try {
            String lang = (language != null && !language.isEmpty()) ? language : configService.getLanguage();
            String templateName = approved ? "review_approved_" + lang + ".html" : "review_rejected_" + lang + ".html";
            String subject = approved ?
                ("zh".equals(lang) ? "[VerifyMC] 您的白名单申请已通过" : "[VerifyMC] Your whitelist application has been approved") :
                ("zh".equals(lang) ? "[VerifyMC] 您的白名单申请被拒绝" : "[VerifyMC] Your whitelist application has been rejected");

            File emailDir = new File(configService.getDataFolder(), "email");
            File templateFile = new File(emailDir, templateName);
            String content;

            if (templateFile.exists()) {
                debugLog("Using custom template: " + templateFile.getAbsolutePath());
                content = new String(Files.readAllBytes(templateFile.toPath()), StandardCharsets.UTF_8);
            } else {
                debugLog("Using default template");
                content = getDefaultReviewResultTemplate(lang, approved);
            }

            String serverName = configService.getWebServerPrefix();
            content = content.replace("{username}", username)
                             .replace("{server_name}", serverName)
                             .replace("{reason}", reason != null ? reason : "");

            debugLog("Creating review result notification email");
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            debugLog("Sending review result notification to: " + email);
            Transport.send(message);
            debugLog("Review result notification sent successfully");
            return true;
        } catch (Exception e) {
            debugLog("Failed to send review result notification: " + e.getMessage());
            configService.getLogger().warning("Failed to send review result notification: " + e.getMessage());
            return false;
        }
    }

    public boolean sendCustomEmail(String to, String subject, String body) {
        if (!isConfigured()) {
            debugLog("Mail service not configured");
            return false;
        }

        debugLog("sendCustomEmail called: to=" + to + ", subject=" + subject);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");

            Transport.send(message);
            debugLog("Custom email sent successfully");
            return true;
        } catch (Exception e) {
            debugLog("Failed to send custom email: " + e.getMessage());
            configService.getLogger().warning("Failed to send custom email: " + e.getMessage());
            return false;
        }
    }

    private boolean isUserNotificationEnabled() {
        return configService.getBoolean("user_notification.enabled", true);
    }

    private boolean isNotifyOnApprove() {
        return configService.getBoolean("user_notification.on_approve", true);
    }

    private boolean isNotifyOnReject() {
        return configService.getBoolean("user_notification.on_reject", true);
    }

    private String getDefaultVerifyCodeTemplate(String lang, String code) {
        if ("zh".equals(lang)) {
            return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
                   "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">" +
                   "<h2 style=\"color: #333;\">验证码</h2>" +
                   "<p style=\"color: #666;\">您的验证码是：</p>" +
                   "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center;\">" +
                   "<span style=\"font-size: 32px; font-weight: bold; color: #4CAF50; letter-spacing: 5px;\">" + code + "</span>" +
                   "</div>" +
                   "<p style=\"color: #999; font-size: 12px;\">此邮件由 VerifyMC 自动发送</p>" +
                   "</div></body></html>";
        } else {
            return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
                   "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">" +
                   "<h2 style=\"color: #333;\">Verification Code</h2>" +
                   "<p style=\"color: #666;\">Your verification code is:</p>" +
                   "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center;\">" +
                   "<span style=\"font-size: 32px; font-weight: bold; color: #4CAF50; letter-spacing: 5px;\">" + code + "</span>" +
                   "</div>" +
                   "<p style=\"color: #999; font-size: 12px;\">This email was automatically sent by VerifyMC</p>" +
                   "</div></body></html>";
        }
    }

    private String getDefaultReviewResultTemplate(String lang, boolean approved) {
        if ("zh".equals(lang)) {
            if (approved) {
                return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
                       "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">" +
                       "<h2 style=\"color: #4CAF50;\">🎉 白名单申请已通过</h2>" +
                       "<p style=\"color: #666;\">恭喜！您在 {server_name} 的白名单申请已通过审核。</p>" +
                       "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0;\">" +
                       "<p><strong>用户名:</strong> {username}</p>" +
                       "<p><strong>状态:</strong> <span style=\"color: #4CAF50;\">已通过</span></p>" +
                       "</div>" +
                       "<p style=\"color: #666;\">您现在可以使用该用户名登录服务器了。</p>" +
                       "<p style=\"color: #999; font-size: 12px;\">此邮件由 VerifyMC 自动发送</p>" +
                       "</div></body></html>";
            } else {
                return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
                       "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">" +
                       "<h2 style=\"color: #f44336;\">白名单申请被拒绝</h2>" +
                       "<p style=\"color: #666;\">很抱歉，您在 {server_name} 的白名单申请未能通过审核。</p>" +
                       "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0;\">" +
                       "<p><strong>用户名:</strong> {username}</p>" +
                       "<p><strong>状态:</strong> <span style=\"color: #f44336;\">已拒绝</span></p>" +
                       "<p><strong>原因:</strong> {reason}</p>" +
                       "</div>" +
                       "<p style=\"color: #666;\">如有疑问，请联系服务器管理员。</p>" +
                       "<p style=\"color: #999; font-size: 12px;\">此邮件由 VerifyMC 自动发送</p>" +
                       "</div></body></html>";
            }
        } else {
            if (approved) {
                return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
                       "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">" +
                       "<h2 style=\"color: #4CAF50;\">🎉 Whitelist Application Approved</h2>" +
                       "<p style=\"color: #666;\">Congratulations! Your whitelist application for {server_name} has been approved.</p>" +
                       "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0;\">" +
                       "<p><strong>Username:</strong> {username}</p>" +
                       "<p><strong>Status:</strong> <span style=\"color: #4CAF50;\">Approved</span></p>" +
                       "</div>" +
                       "<p style=\"color: #666;\">You can now join the server using this username.</p>" +
                       "<p style=\"color: #999; font-size: 12px;\">This email was automatically sent by VerifyMC</p>" +
                       "</div></body></html>";
            } else {
                return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
                       "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">" +
                       "<h2 style=\"color: #f44336;\">Whitelist Application Rejected</h2>" +
                       "<p style=\"color: #666;\">We're sorry, but your whitelist application for {server_name} has been rejected.</p>" +
                       "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0;\">" +
                       "<p><strong>Username:</strong> {username}</p>" +
                       "<p><strong>Status:</strong> <span style=\"color: #f44336;\">Rejected</span></p>" +
                       "<p><strong>Reason:</strong> {reason}</p>" +
                       "</div>" +
                       "<p style=\"color: #666;\">If you have any questions, please contact the server administrator.</p>" +
                       "<p style=\"color: #999; font-size: 12px;\">This email was automatically sent by VerifyMC</p>" +
                       "</div></body></html>";
            }
        }
    }

    private void debugLog(String msg) {
        if (debug) {
            configService.getLogger().info("[DEBUG] MailIntegrationService: " + msg);
        }
    }
}
