package team.kitemc.verifymc.db;

public record AuditRecord(Long id, String action, String operator, String target, String detail, long timestamp) {
    public AuditRecord {
        action = action == null ? "" : action;
        operator = operator == null ? "" : operator;
        target = target == null ? "" : target;
        detail = detail == null ? "" : detail;
    }

    public AuditRecord(String action, String operator, String target, String detail, long timestamp) {
        this(null, action, operator, target, detail, timestamp);
    }
}
