package team.kitemc.verifymc.domain.service;

import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.domain.model.AuditRecord;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.domain.repository.AuditRepository;
import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import java.util.ArrayList;
import java.util.List;

public class ReviewService {
    private final Plugin plugin;
    private final UserService userService;
    private final AuditRepository auditRepository;
    private final ConfigurationService configService;
    private MailServiceAdapter mailService;

    public ReviewService(Plugin plugin, UserService userService, AuditRepository auditRepository, ConfigurationService configService) {
        this.plugin = plugin;
        this.userService = userService;
        this.auditRepository = auditRepository;
        this.configService = configService;
    }

    public void setMailService(MailServiceAdapter mailService) {
        this.mailService = mailService;
    }

    public List<User> getPendingUsers() {
        return userService.getPendingUsers();
    }

    public boolean approveUser(String uuid, String reviewer, String reason) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }

        User user = userService.getUserByUuid(uuid).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getStatus() == UserStatus.APPROVED) {
            return true;
        }

        boolean updated = userService.updateUserStatus(uuid, UserStatus.APPROVED);
        if (!updated) {
            return false;
        }

        createAuditRecord(user, "approve", reviewer, reason);

        if (mailService != null && configService.getBoolean("user_notification.enabled", true)) {
            if (configService.getBoolean("user_notification.on_approve", true)) {
                mailService.sendReviewResultNotification(user.getEmail(), user.getUsername(), true, reason, "en");
            }
        }

        userService.flush();

        return true;
    }

    public boolean rejectUser(String uuid, String reviewer, String reason) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }

        User user = userService.getUserByUuid(uuid).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getStatus() == UserStatus.REJECTED) {
            return true;
        }

        boolean updated = userService.updateUserStatus(uuid, UserStatus.REJECTED);
        if (!updated) {
            return false;
        }

        createAuditRecord(user, "reject", reviewer, reason);

        if (mailService != null && configService.getBoolean("user_notification.enabled", true)) {
            if (configService.getBoolean("user_notification.on_reject", true)) {
                mailService.sendReviewResultNotification(user.getEmail(), user.getUsername(), false, reason, "en");
            }
        }

        userService.flush();

        return true;
    }

    public BatchResult batchApprove(List<String> uuids, String reviewer) {
        if (uuids == null || uuids.isEmpty()) {
            return BatchResult.success(0);
        }

        BatchResult.Builder builder = BatchResult.builder().total(uuids.size());
        int successCount = 0;

        for (String uuid : uuids) {
            boolean result = approveUser(uuid, reviewer, "Batch approved");
            if (result) {
                successCount++;
            } else {
                builder.addFailedUuid(uuid).addErrorMessage("Failed to approve user: " + uuid);
            }
        }

        return builder.success(successCount).failed(uuids.size() - successCount).build();
    }

    public BatchResult batchReject(List<String> uuids, String reviewer, String reason) {
        if (uuids == null || uuids.isEmpty()) {
            return BatchResult.success(0);
        }

        BatchResult.Builder builder = BatchResult.builder().total(uuids.size());
        int successCount = 0;

        for (String uuid : uuids) {
            boolean result = rejectUser(uuid, reviewer, reason);
            if (result) {
                successCount++;
            } else {
                builder.addFailedUuid(uuid).addErrorMessage("Failed to reject user: " + uuid);
            }
        }

        return builder.success(successCount).failed(uuids.size() - successCount).build();
    }

    public List<AuditRecord> getReviewHistory(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return new ArrayList<>();
        }
        return auditRepository.findByUuid(uuid);
    }

    private void createAuditRecord(User user, String action, String reviewer, String details) {
        AuditRecord record = new AuditRecord.Builder()
                .action(action)
                .uuid(user.getUuid())
                .username(user.getUsername())
                .email(user.getEmail())
                .timestamp(System.currentTimeMillis())
                .details(buildDetails(reviewer, details))
                .build();

        auditRepository.save(record);
    }

    private String buildDetails(String reviewer, String details) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reviewer: ").append(reviewer != null ? reviewer : "System");
        if (details != null && !details.isEmpty()) {
            sb.append(" | Reason: ").append(details);
        }
        return sb.toString();
    }

    public void flush() {
        userService.flush();
        auditRepository.flush();
    }

    public interface MailServiceAdapter {
        boolean sendReviewResultNotification(String email, String username, boolean approved, String reason, String language);
    }
}
