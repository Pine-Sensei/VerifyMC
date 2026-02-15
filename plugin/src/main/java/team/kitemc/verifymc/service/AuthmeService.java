package team.kitemc.verifymc.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.db.UserDao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * AuthMe integration service class
 * Uses direct database operations (mysql / sqlite).
 */
public class AuthmeService {
    private final Plugin plugin;
    private final boolean debug;
    private UserDao userDao;

    public AuthmeService(Plugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public boolean isAuthmeEnabled() {
        return plugin.getConfig().getBoolean("authme.enabled", false);
    }

    public boolean isPasswordRequired() {
        return plugin.getConfig().getBoolean("authme.require_password", false);
    }

    public boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        String regex = plugin.getConfig().getString("authme.password_regex", "^[a-zA-Z0-9_]{8,26}$");
        return Pattern.matches(regex, password);
    }

    public boolean registerToAuthme(String username, String password) {
        if (!isAuthmeEnabled()) {
            debugLog("AuthMe not enabled, skipping registration");
            return false;
        }

        return upsertAuthmeUser(username, password);
    }

    public boolean unregisterFromAuthme(String username) {
        if (!isAuthmeEnabled()) {
            debugLog("AuthMe not enabled, skipping unregistration");
            return false;
        }

        return deleteAuthmeUser(username);
    }

    public boolean changePasswordInAuthme(String username, String newPassword) {
        if (!isAuthmeEnabled()) {
            debugLog("AuthMe not enabled, skipping password change");
            return false;
        }

        return updateAuthmePassword(username, newPassword);
    }

    /**
     * Encode password in AuthMe-compatible format for VerifyMC local storage.
     * Accepts plain text and returns encoded value; already-encoded AuthMe values are returned as-is.
     */
    public String encodePasswordForStorage(String plainOrEncodedPassword) {
        return buildStoredPassword(plainOrEncodedPassword);
    }

    /**
     * Synchronize approved users between VerifyMC and AuthMe.
     * - Local approved users are synced to AuthMe by filling missing shared fields.
     * - AuthMe users are synced to local storage and marked as approved.
     */
    public void syncApprovedUsers() {
        if (!isAuthmeEnabled() || userDao == null) {
            return;
        }
        try {
            List<Map<String, Object>> localUsers = userDao.getAllUsers();
            Map<String, Map<String, Object>> localByLowerName = new HashMap<>();
            for (Map<String, Object> u : localUsers) {
                String username = (String) u.get("username");
                if (username != null) {
                    localByLowerName.put(username.toLowerCase(), u);
                }
            }

            Map<String, AuthmeUserRow> authmeByLowerName = listAuthmeUsersByLowerName();

            for (Map.Entry<String, Map<String, Object>> entry : localByLowerName.entrySet()) {
                AuthmeUserRow authme = authmeByLowerName.get(entry.getKey());
                if (authme == null) {
                    continue;
                }

                Map<String, Object> local = entry.getValue();
                String identity = localIdentity(local, authme.username());
                String status = asTrimmedString(local.get("status"));
                if (!"approved".equalsIgnoreCase(status)) {
                    userDao.updateUserStatus(identity, "approved");
                }

                String localPassword = asTrimmedString(local.get("password"));
                String authPassword = asTrimmedString(authme.password());
                if (isBlank(localPassword) && !isBlank(authPassword)) {
                    userDao.updateUserPassword(identity, authPassword);
                } else if (!isBlank(localPassword) && isBlank(authPassword)) {
                    updateAuthmePassword(authme.username(), localPassword);
                }

                if (authme.hasEmailColumn()) {
                    String localEmail = asTrimmedString(local.get("email"));
                    String authEmail = asTrimmedString(authme.email());
                    if (isBlank(localEmail) && !isBlank(authEmail)) {
                        userDao.updateUserEmail(identity, authEmail);
                    } else if (!isBlank(localEmail) && isBlank(authEmail)) {
                        updateAuthmeEmail(authme.username(), localEmail);
                    }
                }
            }

            for (AuthmeUserRow authme : authmeByLowerName.values()) {
                Map<String, Object> local = localByLowerName.get(authme.username().toLowerCase());
                String authPassword = asTrimmedString(authme.password());
                String authEmail = asTrimmedString(authme.email());

                if (local == null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(authme.username());
                    UUID id = offlinePlayer.getUniqueId();
                    boolean created;
                    if (!isBlank(authPassword)) {
                        created = userDao.registerUser(id.toString(), authme.username(), isBlank(authEmail) ? "" : authEmail, "approved", authPassword);
                    } else {
                        created = userDao.registerUser(id.toString(), authme.username(), isBlank(authEmail) ? "" : authEmail, "approved");
                    }
                    if (created) {
                        continue;
                    }

                    Map<String, Object> fallbackLocal = userDao.getUserByUsername(authme.username());
                    if (fallbackLocal == null) {
                        continue;
                    }
                    local = fallbackLocal;
                }

                String identity = localIdentity(local, authme.username());
                String localStatus = asTrimmedString(local.get("status"));
                if (!"approved".equalsIgnoreCase(localStatus)) {
                    userDao.updateUserStatus(identity, "approved");
                }

                String localPassword = asTrimmedString(local.get("password"));
                if (isBlank(localPassword) && !isBlank(authPassword)) {
                    userDao.updateUserPassword(identity, authPassword);
                }

                if (authme.hasEmailColumn()) {
                    String localEmail = asTrimmedString(local.get("email"));
                    if (isBlank(localEmail) && !isBlank(authEmail)) {
                        userDao.updateUserEmail(identity, authEmail);
                    }
                }
            }
            userDao.save();
        } catch (Exception e) {
            debugLog("Failed syncApprovedUsers: " + e.getMessage());
        }
    }

    private Connection getAuthmeConnection() throws Exception {
        String type = plugin.getConfig().getString("authme.database.type", "sqlite").toLowerCase();
        if ("sqlite".equals(type)) {
            Class.forName("org.sqlite.JDBC");
            String path = plugin.getConfig().getString("authme.database.sqlite.path", "plugins/AuthMe/authme.db");
            return DriverManager.getConnection("jdbc:sqlite:" + path);
        }

        Class.forName("com.mysql.cj.jdbc.Driver");
        String host = plugin.getConfig().getString("authme.database.mysql.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("authme.database.mysql.port", 3306);
        String database = plugin.getConfig().getString("authme.database.mysql.database", "authme");
        String user = plugin.getConfig().getString("authme.database.mysql.user", "root");
        String password = plugin.getConfig().getString("authme.database.mysql.password", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&characterEncoding=utf8";
        return DriverManager.getConnection(url, user, password);
    }

    private String tableName() {
        return plugin.getConfig().getString("authme.database.table", "authme");
    }

    private String column(String key, String def) {
        return plugin.getConfig().getString("authme.database.columns." + key, def);
    }

    private String nameColumn() {
        return column("mySQLColumnName", "username");
    }

    private String passwordColumn() {
        return column("mySQLColumnPassword", "password");
    }

    private String saltColumn() {
        return column("mySQLColumnSalt", "");
    }

    private boolean hasSaltColumn() {
        String saltCol = saltColumn();
        return saltCol != null && !saltCol.trim().isEmpty();
    }

    private Map<String, AuthmeUserRow> listAuthmeUsersByLowerName() throws Exception {
        Map<String, AuthmeUserRow> result = new HashMap<>();
        Set<String> existingColumns = getAuthmeColumns();
        String nameCol = nameColumn();
        String passCol = passwordColumn();
        String emailCol = column("mySQLColumnEmail", "email");
        boolean hasPassword = existingColumns.contains(passCol.toLowerCase());
        boolean hasEmail = existingColumns.contains(emailCol.toLowerCase());

        StringBuilder sql = new StringBuilder("SELECT ").append(nameCol);
        if (hasPassword) {
            sql.append(", ").append(passCol);
        }
        if (hasEmail) {
            sql.append(", ").append(emailCol);
        }
        sql.append(" FROM ").append(tableName());

        try (Connection conn = getAuthmeConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                String username = rs.getString(1);
                if (username != null) {
                    int idx = 2;
                    String password = hasPassword ? rs.getString(idx++) : null;
                    String email = hasEmail ? rs.getString(idx) : null;
                    result.put(username.toLowerCase(), new AuthmeUserRow(username, password, email, hasEmail));
                }
            }
        }
        return result;
    }

    private Set<String> getAuthmeColumns() throws Exception {
        Set<String> columns = new HashSet<>();
        String sql = "SELECT * FROM " + tableName() + " LIMIT 1";
        try (Connection conn = getAuthmeConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int count = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= count; i++) {
                columns.add(rs.getMetaData().getColumnName(i).toLowerCase());
            }
        }
        return columns;
    }

    private boolean upsertAuthmeUser(String username, String password) {
        Set<String> existingColumns;
        try {
            existingColumns = getAuthmeColumns();
        } catch (Exception e) {
            debugLog("Failed to inspect AuthMe columns: " + e.getMessage());
            return false;
        }

        String nameCol = nameColumn();
        String passCol = passwordColumn();
        if (!existingColumns.contains(nameCol.toLowerCase()) || !existingColumns.contains(passCol.toLowerCase())) {
            debugLog("Skip AuthMe sync: missing username/password column in AuthMe table");
            return false;
        }

        String realNameCol = column("mySQLRealName", "realname");
        String regDateCol = column("mySQLColumnRegisterDate", "regdate");
        String lastLoginCol = column("mySQLColumnLastLogin", "lastlogin");
        String ipCol = column("mySQLColumnIp", "ip");
        String regIpCol = column("mySQLColumnRegisterIp", "regip");
        String loggedCol = column("mySQLColumnLogged", "isLogged");
        String hasSessionCol = column("mySQLColumnHasSession", "hasSession");
        String xCol = column("mySQLlastlocX", "x");
        String yCol = column("mySQLlastlocY", "y");
        String zCol = column("mySQLlastlocZ", "z");
        String worldCol = column("mySQLlastlocWorld", "world");
        String yawCol = column("mySQLlastlocYaw", "yaw");
        String pitchCol = column("mySQLlastlocPitch", "pitch");
        String emailCol = column("mySQLColumnEmail", "email");
        String saltCol = saltColumn();

        String selectSql = "SELECT " + nameCol + " FROM " + tableName() + " WHERE " + nameCol + " = ?";

        StringBuilder updateSql = new StringBuilder("UPDATE " + tableName() + " SET " + passCol + " = ?");
        if (existingColumns.contains(realNameCol.toLowerCase())) updateSql.append(", ").append(realNameCol).append(" = ?");
        if (existingColumns.contains(regDateCol.toLowerCase())) updateSql.append(", ").append(regDateCol).append(" = ?");
        if (existingColumns.contains(lastLoginCol.toLowerCase())) updateSql.append(", ").append(lastLoginCol).append(" = ?");
        if (existingColumns.contains(ipCol.toLowerCase())) updateSql.append(", ").append(ipCol).append(" = ?");
        if (existingColumns.contains(regIpCol.toLowerCase())) updateSql.append(", ").append(regIpCol).append(" = ?");
        if (existingColumns.contains(loggedCol.toLowerCase())) updateSql.append(", ").append(loggedCol).append(" = 0");
        if (existingColumns.contains(hasSessionCol.toLowerCase())) updateSql.append(", ").append(hasSessionCol).append(" = 0");
        if (hasSaltColumn() && existingColumns.contains(saltCol.toLowerCase())) {
            updateSql.append(", ").append(saltCol).append(" = ?");
        }
        updateSql.append(" WHERE ").append(nameCol).append(" = ?");

        long now = System.currentTimeMillis() / 1000;
        String loopback = "127.0.0.1";
        String storedPassword = buildStoredPassword(password);

        try (Connection conn = getAuthmeConnection();
             PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setString(1, username);
            boolean exists;
            try (ResultSet rs = select.executeQuery()) {
                exists = rs.next();
            }

            if (!exists) {
                debugLog("Skip AuthMe create for non-existing user: " + username);
                return false;
            }

            try (PreparedStatement update = conn.prepareStatement(updateSql.toString())) {
                int idx = 1;
                update.setString(idx++, storedPassword);
                if (existingColumns.contains(realNameCol.toLowerCase())) update.setString(idx++, username);
                if (existingColumns.contains(regDateCol.toLowerCase())) update.setLong(idx++, now);
                if (existingColumns.contains(lastLoginCol.toLowerCase())) update.setLong(idx++, now);
                if (existingColumns.contains(ipCol.toLowerCase())) update.setString(idx++, loopback);
                if (existingColumns.contains(regIpCol.toLowerCase())) update.setString(idx++, loopback);
                if (hasSaltColumn() && existingColumns.contains(saltCol.toLowerCase())) {
                    update.setString(idx++, "");
                }
                update.setString(idx, username);
                return update.executeUpdate() > 0;
            }
        } catch (Exception e) {
            debugLog("Failed to upsert AuthMe user " + username + ": " + e.getMessage());
            return false;
        }
    }

    private boolean deleteAuthmeUser(String username) {
        String sql = "DELETE FROM " + tableName() + " WHERE " + nameColumn() + " = ?";
        try (Connection conn = getAuthmeConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            debugLog("Failed to delete AuthMe user " + username + ": " + e.getMessage());
            return false;
        }
    }

    private boolean updateAuthmePassword(String username, String newPassword) {
        String passCol = passwordColumn();
        String nameCol = nameColumn();
        Set<String> existingColumns;
        try {
            existingColumns = getAuthmeColumns();
        } catch (Exception e) {
            debugLog("Failed to inspect AuthMe columns for password update: " + e.getMessage());
            return false;
        }
        if (!existingColumns.contains(nameCol.toLowerCase()) || !existingColumns.contains(passCol.toLowerCase())) {
            return false;
        }
        String sql;
        if (hasSaltColumn() && existingColumns.contains(saltColumn().toLowerCase())) {
            sql = "UPDATE " + tableName() + " SET " + passCol + " = ?, " + saltColumn() + " = ? WHERE " + nameCol + " = ?";
        } else {
            sql = "UPDATE " + tableName() + " SET " + passCol + " = ? WHERE " + nameCol + " = ?";
        }
        try (Connection conn = getAuthmeConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, buildStoredPassword(newPassword));
            if (hasSaltColumn()) {
                ps.setString(2, "");
                ps.setString(3, username);
            } else {
                ps.setString(2, username);
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            debugLog("Failed to update AuthMe password " + username + ": " + e.getMessage());
            return false;
        }
    }

    private boolean updateAuthmeEmail(String username, String email) {
        String nameCol = nameColumn();
        String emailCol = column("mySQLColumnEmail", "email");
        Set<String> existingColumns;
        try {
            existingColumns = getAuthmeColumns();
        } catch (Exception e) {
            debugLog("Failed to inspect AuthMe columns for email update: " + e.getMessage());
            return false;
        }
        if (!existingColumns.contains(nameCol.toLowerCase()) || !existingColumns.contains(emailCol.toLowerCase())) {
            return false;
        }

        String sql = "UPDATE " + tableName() + " SET " + emailCol + " = ? WHERE " + nameCol + " = ?";
        try (Connection conn = getAuthmeConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            debugLog("Failed to update AuthMe email " + username + ": " + e.getMessage());
            return false;
        }
    }

    private String localIdentity(Map<String, Object> local, String fallbackUsername) {
        String uuid = asTrimmedString(local.get("uuid"));
        return isBlank(uuid) ? fallbackUsername : uuid;
    }

    private String asTrimmedString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record AuthmeUserRow(String username, String password, String email, boolean hasEmailColumn) {}

    private String buildStoredPassword(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }
        if (plainPassword.startsWith("$SHA$")) {
            return plainPassword;
        }
        return authmeSha256(plainPassword);
    }

    private String authmeSha256(String plainPassword) {
        String salt = generateHexSalt(getSaltLength());
        return "$SHA$" + salt + "$" + sha256Hex(sha256Hex(plainPassword) + salt);
    }

    private int getSaltLength() {
        return plugin.getConfig().getInt("authme.database.salt_length", 16);
    }

    private String generateHexSalt(int length) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(random.nextInt(16)));
        }
        return sb.toString();
    }

    private String sha256Hex(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return data;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] AuthmeService: " + msg);
        }
    }
}
