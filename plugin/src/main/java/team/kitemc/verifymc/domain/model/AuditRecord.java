package team.kitemc.verifymc.domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AuditRecord {
    private final String id;
    private final String action;
    private final String uuid;
    private final String username;
    private final String email;
    private final long timestamp;
    private final String details;

    private AuditRecord(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.action = builder.action != null ? builder.action : "";
        this.uuid = builder.uuid != null ? builder.uuid : "";
        this.username = builder.username != null ? builder.username : "";
        this.email = builder.email != null ? builder.email : "";
        this.timestamp = builder.timestamp > 0 ? builder.timestamp : System.currentTimeMillis();
        this.details = builder.details != null ? builder.details : "";
    }

    public String getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("action", action);
        map.put("uuid", uuid);
        map.put("username", username);
        map.put("email", email);
        map.put("timestamp", timestamp);
        map.put("details", details);
        return map;
    }

    public static AuditRecord fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Builder builder = new Builder();
        builder.id((String) map.get("id"));
        builder.action((String) map.get("action"));
        builder.uuid((String) map.get("uuid"));
        builder.username((String) map.get("username"));
        builder.email((String) map.get("email"));
        builder.timestamp(getLongValue(map.get("timestamp")));
        builder.details((String) map.get("details"));
        return builder.build();
    }

    private static long getLongValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static class Builder {
        private String id;
        private String action;
        private String uuid;
        private String username;
        private String email;
        private long timestamp;
        private String details;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public AuditRecord build() {
            return new AuditRecord(this);
        }
    }
}
