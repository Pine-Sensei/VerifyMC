package team.kitemc.verifymc.db;

public record AuditRecord(Long id, String uuid, String username, String action, String operator, String reason, long timestamp) {
    public AuditRecord {
        uuid = uuid == null ? "" : uuid;
        username = username == null ? "" : username;
        action = action == null ? "" : action;
        operator = operator == null ? "" : operator;
        reason = reason == null ? "" : reason;
    }

    public AuditRecord(String uuid, String username, String action, String operator, String reason, long timestamp) {
        this(null, uuid, username, action, operator, reason, timestamp);
    }
}
