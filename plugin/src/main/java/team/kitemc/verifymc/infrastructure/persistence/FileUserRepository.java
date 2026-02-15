package team.kitemc.verifymc.infrastructure.persistence;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.domain.repository.UserRepository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileUserRepository implements UserRepository {
    private final File file;
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final boolean debug;
    private final Plugin plugin;

    public FileUserRepository(File dataFile, Plugin plugin) {
        this.file = dataFile;
        this.plugin = plugin;
        this.debug = plugin != null && plugin.getConfig().getBoolean("debug", false);
        load();
    }

    public FileUserRepository(File dataFile) {
        this.file = dataFile;
        this.plugin = null;
        this.debug = false;
        load();
    }

    private void debugLog(String msg) {
        if (debug && plugin != null) {
            plugin.getLogger().info("[DEBUG] FileUserRepository: " + msg);
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void load() {
        debugLog("Loading users from: " + file.getAbsolutePath());
        if (!file.exists()) {
            debugLog("File does not exist, creating new user database");
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Map<String, Map<String, Object>> loaded = gson.fromJson(reader,
                    new TypeToken<Map<String, Map<String, Object>>>() {}.getType());
            if (loaded != null) {
                boolean hasUpgraded = false;
                for (Map.Entry<String, Map<String, Object>> entry : loaded.entrySet()) {
                    Map<String, Object> userData = entry.getValue();
                    if (userData != null) {
                        ensureRequiredFields(userData, entry.getKey());
                        User user = User.fromMap(userData);
                        if (user != null) {
                            users.put(user.getUuid(), user);
                        }
                    }
                }
                debugLog("Loaded " + users.size() + " users from database");
                if (hasUpgraded) {
                    debugLog("Data format upgraded, saving updated data");
                    flush();
                }
            } else {
                debugLog("No users found in database");
            }
        } catch (Exception e) {
            debugLog("Error loading users: " + e.getMessage());
        }
    }

    private void ensureRequiredFields(Map<String, Object> userData, String uuid) {
        if (!userData.containsKey("uuid")) {
            userData.put("uuid", uuid);
        }
        if (!userData.containsKey("password")) {
            userData.put("password", null);
        }
        if (!userData.containsKey("regTime")) {
            userData.put("regTime", System.currentTimeMillis());
        }
        if (!userData.containsKey("discord_id")) {
            userData.put("discord_id", null);
        }
        if (!userData.containsKey("questionnaire_score")) {
            userData.put("questionnaire_score", null);
        }
        if (!userData.containsKey("questionnaire_passed")) {
            userData.put("questionnaire_passed", null);
        }
        if (!userData.containsKey("questionnaire_review_summary")) {
            userData.put("questionnaire_review_summary", null);
        }
        if (!userData.containsKey("questionnaire_scored_at")) {
            userData.put("questionnaire_scored_at", null);
        }
    }

    @Override
    public synchronized void flush() {
        debugLog("Saving " + users.size() + " users to: " + file.getAbsolutePath());
        try (Writer writer = new FileWriter(file)) {
            Map<String, Map<String, Object>> dataToSave = new HashMap<>();
            for (User user : users.values()) {
                dataToSave.put(user.getUuid(), user.toMap());
            }
            gson.toJson(dataToSave, writer);
            debugLog("Save successful");
        } catch (Exception e) {
            debugLog("Error saving users: " + e.getMessage());
        }
    }

    @Override
    public boolean save(User user) {
        debugLog("save called: uuid=" + user.getUuid() + ", username=" + user.getUsername());
        try {
            if (users.containsKey(user.getUuid())) {
                debugLog("User already exists with UUID: " + user.getUuid() + ", updating");
            }
            users.put(user.getUuid(), user);
            flush();
            debugLog("User saved successfully");
            return true;
        } catch (Exception e) {
            debugLog("Exception in save: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<User> findByUuid(String uuid) {
        debugLog("Finding user by UUID: " + uuid);
        return Optional.ofNullable(users.get(uuid));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        debugLog("Finding user by username: " + username);
        return users.values().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public List<User> findByEmail(String email) {
        debugLog("Finding users by email: " + email);
        return users.values().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findByDiscordId(String discordId) {
        debugLog("Finding user by Discord ID: " + discordId);
        return users.values().stream()
                .filter(u -> discordId != null && discordId.equals(u.getDiscordId()))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        debugLog("Getting all users, total: " + users.size());
        return new ArrayList<>(users.values());
    }

    @Override
    public List<User> findPending() {
        debugLog("Getting pending users");
        List<User> result = users.values().stream()
                .filter(User::isPending)
                .collect(Collectors.toList());
        debugLog("Found " + result.size() + " pending users");
        return result;
    }

    @Override
    public List<User> findWithPagination(int page, int pageSize) {
        debugLog("Getting users with pagination: page=" + page + ", pageSize=" + pageSize);
        List<User> allUsers = new ArrayList<>(users.values());
        allUsers.sort(Comparator.comparingLong(User::getRegTime).reversed());
        return paginate(allUsers, page, pageSize);
    }

    @Override
    public List<User> findApprovedWithPagination(int page, int pageSize) {
        debugLog("Getting approved users with pagination: page=" + page + ", pageSize=" + pageSize);
        List<User> approvedUsers = users.values().stream()
                .filter(u -> !u.isPending())
                .sorted(Comparator.comparingLong(User::getRegTime).reversed())
                .collect(Collectors.toList());
        return paginate(approvedUsers, page, pageSize);
    }

    @Override
    public List<User> search(String query, int page, int pageSize) {
        debugLog("Searching users: query=" + query + ", page=" + page + ", pageSize=" + pageSize);
        String lowerQuery = query != null ? query.toLowerCase().trim() : "";
        List<User> filteredUsers = users.values().stream()
                .filter(u -> {
                    if (lowerQuery.isEmpty()) return true;
                    String username = u.getUsername() != null ? u.getUsername().toLowerCase() : "";
                    String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                    return username.contains(lowerQuery) || email.contains(lowerQuery);
                })
                .sorted(Comparator.comparingLong(User::getRegTime).reversed())
                .collect(Collectors.toList());
        return paginate(filteredUsers, page, pageSize);
    }

    private List<User> paginate(List<User> list, int page, int pageSize) {
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, list.size());
        if (startIndex >= list.size()) {
            return new ArrayList<>();
        }
        return list.subList(startIndex, endIndex);
    }

    @Override
    public int count() {
        int count = users.size();
        debugLog("Total user count: " + count);
        return count;
    }

    @Override
    public int countApproved() {
        debugLog("Getting approved user count (excluding pending)");
        int count = (int) users.values().stream()
                .filter(u -> !u.isPending())
                .count();
        debugLog("Approved user count: " + count);
        return count;
    }

    @Override
    public int countByEmail(String email) {
        debugLog("Counting users by email: " + email);
        int count = (int) users.values().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                .count();
        debugLog("Found " + count + " users with email: " + email);
        return count;
    }

    @Override
    public boolean updateStatus(String uuid, UserStatus status) {
        debugLog("updateStatus called: uuid=" + uuid + ", status=" + status);
        User user = users.get(uuid);
        if (user == null) {
            debugLog("User not found: " + uuid);
            return false;
        }
        User updatedUser = user.toBuilder().status(status).build();
        users.put(uuid, updatedUser);
        flush();
        debugLog("User status updated: " + uuid + " to " + status);
        return true;
    }

    @Override
    public boolean updatePassword(String uuid, String password) {
        debugLog("updatePassword called: uuid=" + uuid);
        User user = users.get(uuid);
        if (user == null) {
            debugLog("User not found: " + uuid);
            return false;
        }
        User updatedUser = user.toBuilder().password(password).build();
        users.put(uuid, updatedUser);
        flush();
        debugLog("User password updated: " + user.getUsername());
        return true;
    }

    @Override
    public boolean updateEmail(String uuid, String email) {
        debugLog("updateEmail called: uuid=" + uuid);
        User user = users.get(uuid);
        if (user == null) {
            debugLog("User not found: " + uuid);
            return false;
        }
        User updatedUser = user.toBuilder().email(email).build();
        users.put(uuid, updatedUser);
        flush();
        debugLog("User email updated: " + user.getUsername());
        return true;
    }

    @Override
    public boolean updateDiscordId(String uuid, String discordId) {
        debugLog("updateDiscordId called: uuid=" + uuid + ", discordId=" + discordId);
        User user = users.get(uuid);
        if (user == null) {
            debugLog("User not found: " + uuid);
            return false;
        }
        User updatedUser = user.toBuilder().discordId(discordId).build();
        users.put(uuid, updatedUser);
        flush();
        debugLog("User Discord ID updated: " + user.getUsername() + " -> " + discordId);
        return true;
    }

    @Override
    public boolean delete(String uuid) {
        debugLog("delete called: uuid=" + uuid);
        try {
            User removed = users.remove(uuid);
            if (removed != null) {
                debugLog("User deleted: " + removed.getUsername());
                flush();
                return true;
            } else {
                debugLog("User not found for deletion");
                return false;
            }
        } catch (Exception e) {
            debugLog("Exception in delete: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public boolean existsByDiscordId(String discordId) {
        return findByDiscordId(discordId).isPresent();
    }
}
