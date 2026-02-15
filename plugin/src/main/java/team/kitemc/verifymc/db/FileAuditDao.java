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

public class FileAuditDao extends BaseAuditDao {
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
        Long id = getLong(map, "id");
        String uuid = getString(map, "uuid");
        String username = getString(map, "username");
        String action = getString(map, "action");
        String operator = getString(map, "operator");
        String reason = getString(map, "reason");
        Long timestampValue = getLong(map, "timestamp");
        long timestamp = timestampValue != null ? timestampValue : 0L;
        return new AuditRecord(id, uuid, username, action, operator, reason, timestamp);
    }

    @Override
    public synchronized void save() {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(audits, writer);
        } catch (Exception ignored) {}
    }

    @Override
    public synchronized void addAudit(AuditRecord audit) {
        validateRecord(audit);
        audits.add(audit);
        save();
    }

    @Override
    public synchronized List<AuditRecord> getAllAudits() {
        return new ArrayList<>(audits);
    }
}
