package team.kitemc.verifymc.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileAuditDao implements AuditDao {
    private final File file;
    private final List<AuditRecord> audits = new ArrayList<>();
    private final Gson gson = new Gson();

    public FileAuditDao(File dataFile) {
        this.file = dataFile;
        load();
    }

    public synchronized void load() {
        audits.clear();
        if (!file.exists()) return;

        List<AuditRecord> loaded = tryLoadAuditRecords();
        if (loaded != null) {
            audits.addAll(loaded);
            return;
        }

        List<Map<String, Object>> legacy = tryLoadLegacyMaps();
        if (legacy == null) {
            return;
        }

        for (Map<String, Object> item : legacy) {
            audits.add(mapToRecord(item));
        }
        save();
    }

    private List<AuditRecord> tryLoadAuditRecords() {
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, new TypeToken<List<AuditRecord>>() {}.getType());
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<Map<String, Object>> tryLoadLegacyMaps() {
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, new TypeToken<List<Map<String, Object>>>() {}.getType());
        } catch (Exception ignored) {
            return null;
        }
    }

    private AuditRecord mapToRecord(Map<String, Object> map) {
        Long id = asLong(map.get("id"));
        String action = asString(map.get("action"));
        String operator = asString(map.get("operator"));
        String target = asString(map.get("target"));
        String detail = asString(map.get("detail"));
        Long timestampValue = asLong(map.get("timestamp"));
        long timestamp = timestampValue != null ? timestampValue : 0L;
        return new AuditRecord(id, action, operator, target, detail, timestamp);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public synchronized void save() {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(audits, writer);
        } catch (Exception ignored) {}
    }

    @Override
    public synchronized void addAudit(AuditRecord audit) {
        audits.add(audit);
        save();
    }

    @Override
    public synchronized List<AuditRecord> getAllAudits() {
        return new ArrayList<>(audits);
    }
}
