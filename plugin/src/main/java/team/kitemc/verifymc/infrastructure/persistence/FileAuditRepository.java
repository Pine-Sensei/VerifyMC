package team.kitemc.verifymc.infrastructure.persistence;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.domain.model.AuditRecord;
import team.kitemc.verifymc.domain.repository.AuditRepository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class FileAuditRepository implements AuditRepository {
    private final File file;
    private final List<AuditRecord> records = new CopyOnWriteArrayList<>();
    private final Gson gson = new Gson();
    private final boolean debug;
    private final Plugin plugin;

    public FileAuditRepository(File dataFile, Plugin plugin) {
        this.file = dataFile;
        this.plugin = plugin;
        this.debug = plugin != null && plugin.getConfig().getBoolean("debug", false);
        load();
    }

    public FileAuditRepository(File dataFile) {
        this.file = dataFile;
        this.plugin = null;
        this.debug = false;
        load();
    }

    private void debugLog(String msg) {
        if (debug && plugin != null) {
            plugin.getLogger().info("[DEBUG] FileAuditRepository: " + msg);
        }
    }

    public synchronized void load() {
        debugLog("Loading audit records from: " + file.getAbsolutePath());
        records.clear();
        if (!file.exists()) {
            debugLog("File does not exist, creating new audit database");
            return;
        }

        List<AuditRecord> loaded = tryLoadAuditRecords();
        if (loaded != null) {
            records.addAll(loaded);
            debugLog("Loaded " + records.size() + " audit records");
            return;
        }

        List<Map<String, Object>> legacy = tryLoadLegacyMaps();
        if (legacy == null) {
            debugLog("No audit records found");
            return;
        }

        for (Map<String, Object> item : legacy) {
            AuditRecord record = AuditRecord.fromMap(item);
            if (record != null) {
                records.add(record);
            }
        }
        debugLog("Migrated " + records.size() + " audit records from legacy format");
        flush();
    }

    private List<AuditRecord> tryLoadAuditRecords() {
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, new TypeToken<List<AuditRecord>>() {}.getType());
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> tryLoadLegacyMaps() {
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, new TypeToken<List<Map<String, Object>>>() {}.getType());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized void flush() {
        debugLog("Saving " + records.size() + " audit records to: " + file.getAbsolutePath());
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(records, writer);
            debugLog("Save successful");
        } catch (Exception e) {
            debugLog("Error saving audit records: " + e.getMessage());
        }
    }

    @Override
    public boolean save(AuditRecord record) {
        debugLog("save called: action=" + record.getAction() + ", uuid=" + record.getUuid());
        try {
            records.add(record);
            flush();
            debugLog("Audit record saved successfully");
            return true;
        } catch (Exception e) {
            debugLog("Exception in save: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<AuditRecord> findByUuid(String uuid) {
        debugLog("Finding audit records by UUID: " + uuid);
        return records.stream()
                .filter(r -> uuid != null && uuid.equals(r.getUuid()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditRecord> findByUsername(String username) {
        debugLog("Finding audit records by username: " + username);
        return records.stream()
                .filter(r -> username != null && username.equalsIgnoreCase(r.getUsername()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditRecord> findAll() {
        debugLog("Getting all audit records, total: " + records.size());
        return new ArrayList<>(records);
    }
}
