package team.kitemc.verifymc.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.function.BiFunction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class MailService {
    private final Plugin plugin;
    private final BiFunction<String, String, String> getMessage;
    private final boolean debug;
    private Session session;
    private String from;

    public MailService(Plugin plugin, BiFunction<String, String, String> getMessage) {
        this.plugin = plugin;
        this.getMessage = getMessage;
        this.debug = plugin.getConfig().getBoolean("debug", false);
        init();
    }

    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] MailService: " + msg);
        }
    }

    private void init() {
        debugLog("Initializing MailService");
        FileConfiguration config = plugin.getConfig();
        String host = config.getString("smtp.host", "smtp.qq.com");
        int port = config.getInt("smtp.port", 587);
        String username = config.getString("smtp.username");
        String password = config.getString("smtp.password");
        from = config.getString("smtp.from", username);
        boolean enableSsl = config.getBoolean("smtp.enable_ssl", true);

        debugLog("SMTP Configuration: host=" + host + ", port=" + port + ", enableSsl=" + enableSsl);

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
        debugLog("MailService initialized successfully");
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Deprecated
    public boolean sendCode(String to, String subject, String code, String language) {
        debugLog("sendCode called: to=" + to + ", subject=" + subject + ", language=" + language);
        return sendVerificationCode(to, code, language);
    }

    @Deprecated
    public boolean sendCode(String to, String subject, String code) {
        return sendVerificationCode(to, code, null);
    }

    @Deprecated
    public boolean sendVerifyCode(String to, String subject, String code) {
        return sendVerificationCode(to, code, null);
    }

    @Deprecated
    public boolean sendVerifyCode(String to, String subject, String code, String language) {
        return sendVerificationCode(to, code, language);
    }

    public boolean sendVerificationCode(String to, String code, String language) {
        debugLog("sendVerificationCode called: to=" + to + ", language=" + language);
        try {
            String lang = resolveLanguage(language);
            String serverName = plugin.getConfig().getString("web_server_prefix", "[ Server ]");
            int expireSeconds = plugin.getConfig().getInt("captcha.expire_seconds", 300);
            long expireMinutes = Math.max(1, expireSeconds / 60);

            String content = loadEmailTemplate("verify_code_" + lang + ".html", getDefaultVerifyCodeTemplate(lang))
                    .replace("{code}", escapeHtml(code))
                    .replace("{server_name}", escapeHtml(serverName))
                    .replace("{expire_minutes}", String.valueOf(expireMinutes));

            String subject = plugin.getConfig().getString("email_subject", "VerifyMC Verification Code");
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            Transport.send(message);
            debugLog("Email sent successfully");
            return true;
        } catch (Exception e) {
            String lang = resolveLanguage(language);
            debugLog("Failed to send email: " + e.getMessage());
            plugin.getLogger().warning(getMessage.apply("email.failed", lang) + ": " + e.getMessage());
            return false;
        }
    }

    private String getDefaultVerifyCodeTemplate(String lang) {
        if ("zh".equals(lang)) {
            return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">"
                    + "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">"
                    + "<h2 style=\"color: #333;\">邮箱验证码</h2>"
                    + "<p style=\"color: #666;\">您在 {server_name} 的验证码是：</p>"
                    + "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center;\">"
                    + "<span style=\"font-size: 32px; font-weight: bold; color: #4CAF50; letter-spacing: 5px;\">{code}</span>"
                    + "</div>"
                    + "<p style=\"color: #999; font-size: 12px;\">此验证码将在 {expire_minutes} 分钟后过期。该邮件由 VerifyMC 自动发送，请勿回复。</p>"
                    + "</div></body></html>";
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">"
                + "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">"
                + "<h2 style=\"color: #333;\">Verification Code</h2>"
                + "<p style=\"color: #666;\">Your verification code for {server_name} is:</p>"
                + "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center;\">"
                + "<span style=\"font-size: 32px; font-weight: bold; color: #4CAF50; letter-spacing: 5px;\">{code}</span>"
                + "</div>"
                + "<p style=\"color: #999; font-size: 12px;\">This code will expire in {expire_minutes} minutes. This email was automatically sent by VerifyMC.</p>"
                + "</div></body></html>";
    }

    public boolean isUserNotificationEnabled() {
        return plugin.getConfig().getBoolean("user_notification.enabled", true);
    }

    public boolean isNotifyOnApprove() {
        return plugin.getConfig().getBoolean("user_notification.on_approve", true);
    }

    public boolean isNotifyOnReject() {
        return plugin.getConfig().getBoolean("user_notification.on_reject", true);
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

        try {
            String lang = resolveLanguage(language);
            String templateName = approved ? "review_approved_" + lang + ".html" : "review_rejected_" + lang + ".html";
            String subject = approved
                    ? ("zh".equals(lang) ? "[VerifyMC] 您的白名单申请已通过" : "[VerifyMC] Your whitelist application has been approved")
                    : ("zh".equals(lang) ? "[VerifyMC] 您的白名单申请被拒绝" : "[VerifyMC] Your whitelist application has been rejected");
            String content = loadEmailTemplate(templateName, getDefaultReviewResultTemplate(lang, approved));
            String serverName = plugin.getConfig().getString("web_server_prefix", "[ Server ]");

            content = content.replace("{username}", escapeHtml(username))
                    .replace("{server_name}", escapeHtml(serverName))
                    .replace("{reason}", escapeHtml(reason));

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            Transport.send(message);
            debugLog("Review result notification sent successfully");
            return true;
        } catch (Exception e) {
            debugLog("Failed to send review result notification: " + e.getMessage());
            plugin.getLogger().warning("Failed to send review result notification: " + e.getMessage());
            return false;
        }
    }

    @Deprecated
    public boolean sendReviewResult(String email, String username, boolean approved, String reason) {
        return sendReviewResultNotification(email, username, approved, reason, null);
    }

    public boolean sendReviewResult(String email, String username, boolean approved, String reason, String language) {
        return sendReviewResultNotification(email, username, approved, reason, language);
    }

    private String getDefaultReviewResultTemplate(String lang, boolean approved) {
        if ("zh".equals(lang)) {
            if (approved) {
                return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">"
                        + "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">"
                        + "<h2 style=\"color: #4CAF50;\">白名单申请已通过</h2>"
                        + "<p style=\"color: #666;\">恭喜，您在 {server_name} 的白名单申请已通过审核。</p>"
                        + "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0;\">"
                        + "<p><strong>用户名：</strong> {username}</p>"
                        + "<p><strong>状态：</strong> <span style=\"color: #4CAF50;\">已通过</span></p>"
                        + "</div>"
                        + "<p style=\"color: #666;\">您现在可以使用该用户名进入服务器了。</p>"
                        + "<p style=\"color: #999; font-size: 12px;\">该邮件由 VerifyMC 自动发送，请勿回复。</p>"
                        + "</div></body></html>";
            }

            return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">"
                    + "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">"
                    + "<h2 style=\"color: #f44336;\">白名单申请被拒绝</h2>"
                    + "<p style=\"color: #666;\">很抱歉，您在 {server_name} 的白名单申请未通过审核。</p>"
                    + "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0;\">"
                    + "<p><strong>用户名：</strong> {username}</p>"
                    + "<p><strong>状态：</strong> <span style=\"color: #f44336;\">已拒绝</span></p>"
                    + "<p><strong>原因：</strong> {reason}</p>"
                    + "</div>"
                    + "<p style=\"color: #666;\">如有疑问，请联系服务器管理员。</p>"
                    + "<p style=\"color: #999; font-size: 12px;\">该邮件由 VerifyMC 自动发送，请勿回复。</p>"
                    + "</div></body></html>";
        }

        if (approved) {
            return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">"
                    + "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">"
                    + "<h2 style=\"color: #4CAF50;\">Whitelist Application Approved</h2>"
                    + "<p style=\"color: #666;\">Congratulations! Your whitelist application for {server_name} has been approved.</p>"
                    + "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0;\">"
                    + "<p><strong>Username:</strong> {username}</p>"
                    + "<p><strong>Status:</strong> <span style=\"color: #4CAF50;\">Approved</span></p>"
                    + "</div>"
                    + "<p style=\"color: #666;\">You can now join the server using this username.</p>"
                    + "<p style=\"color: #999; font-size: 12px;\">This email was automatically sent by VerifyMC.</p>"
                    + "</div></body></html>";
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px;\">"
                + "<div style=\"max-width: 600px; margin: 0 auto; background: #f9f9f9; padding: 30px; border-radius: 10px;\">"
                + "<h2 style=\"color: #f44336;\">Whitelist Application Rejected</h2>"
                + "<p style=\"color: #666;\">We're sorry, but your whitelist application for {server_name} has been rejected.</p>"
                + "<div style=\"background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0;\">"
                + "<p><strong>Username:</strong> {username}</p>"
                + "<p><strong>Status:</strong> <span style=\"color: #f44336;\">Rejected</span></p>"
                + "<p><strong>Reason:</strong> {reason}</p>"
                + "</div>"
                + "<p style=\"color: #666;\">If you have any questions, please contact the server administrator.</p>"
                + "<p style=\"color: #999; font-size: 12px;\">This email was automatically sent by VerifyMC.</p>"
                + "</div></body></html>";
    }

    private String resolveLanguage(String language) {
        if (language != null && !language.isEmpty()) {
            return language;
        }
        return plugin.getConfig().getString("language", "en");
    }

    private String loadEmailTemplate(String templateName, String fallbackContent) {
        File templateFile = new File(new File(plugin.getDataFolder(), "email"), templateName);
        if (templateFile.exists()) {
            try {
                debugLog("Using custom template: " + templateFile.getAbsolutePath());
                return Files.readString(templateFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                debugLog("Failed to read custom template: " + e.getMessage() + ", falling back");
            }
        }

        try (InputStream resourceStream = plugin.getResource("email/" + templateName)) {
            if (resourceStream != null) {
                debugLog("Using bundled template: email/" + templateName);
                return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            debugLog("Failed to read bundled template: " + e.getMessage() + ", falling back");
        }

        if (!templateName.endsWith("_en.html")) {
            String enTemplateName = templateName.replaceAll("_[a-z]+\\.html$", "_en.html");
            try (InputStream resourceStream = plugin.getResource("email/" + enTemplateName)) {
                if (resourceStream != null) {
                    debugLog("Using English fallback template: email/" + enTemplateName);
                    return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                debugLog("Failed to read English fallback template: " + e.getMessage());
            }
        }

        debugLog("Using hardcoded fallback template: " + templateName);
        return fallbackContent;
    }
}
