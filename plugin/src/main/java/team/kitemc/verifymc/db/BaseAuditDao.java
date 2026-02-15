package team.kitemc.verifymc.db;

import java.util.Map;
import java.util.logging.Logger;

public abstract class BaseAuditDao implements AuditDao {
    protected final Logger logger = Logger.getLogger(getClass().getName());
    
    protected void validateRecord(AuditRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Audit record cannot be null");
        }
        if (record.uuid() == null || record.uuid().isEmpty()) {
            throw new IllegalArgumentException("UUID cannot be null or empty");
        }
        if (record.action() == null || record.action().isEmpty()) {
            throw new IllegalArgumentException("Action cannot be null or empty");
        }
    }
    
    protected String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    protected Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
