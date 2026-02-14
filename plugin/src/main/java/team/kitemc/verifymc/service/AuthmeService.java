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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * AuthMe integration service class
 * Supports two modes:
 * 1) command: execute AuthMe console commands
 * 2) database: operate directly on AuthMe storage (mysql / sqlite)
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

    public String getMode() {
        return plugin.getConfig().getString("authme.mode", "command").toLowerCase();
    }

    public boolean isDatabaseMode() {
        return "database".equals(getMode());
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

        if (isDatabaseMode()) {
            return upsertAuthmeUser(username, password);
        }

        debugLog("Registering user to AuthMe by command: " + username);
        if (Bukkit.isPrimaryThread()) {
            return executeAuthmeCommand("register " + username + " " + password);
        } else {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    executeAuthmeCommand("register " + username + " " + password)
                ).get();
            } catch (Exception e) {
                debugLog("Failed to register user to AuthMe: " + e.getMessage());
                return false;
            }
        }
    }

    public boolean unregisterFromAuthme(String username) {
        if (!isAuthmeEnabled()) {
            debugLog("AuthMe not enabled, skipping unregistration");
            return false;
        }

        if (isDatabaseMode()) {
            return deleteAuthmeUser(username);
        }

        debugLog("Unregistering user from AuthMe by command: " + username);
        if (Bukkit.isPrimaryThread()) {
            return executeAuthmeCommand("purgeplayer " + username + " force");
        } else {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    executeAuthmeCommand("purgeplayer " + username + " force")
                ).get();
            } catch (Exception e) {
                debugLog("Failed to unregister user from AuthMe: " + e.getMessage());
                return false;
            }
        }
    }

    public boolean changePasswordInAuthme(String username, String newPassword) {
        if (!isAuthmeEnabled()) {
            debugLog("AuthMe not enabled, skipping password change");
            return false;
        }

        if (isDatabaseMode()) {
            return updateAuthmePassword(username, newPassword);
        }

        debugLog("Changing password in AuthMe by command: " + username);
        if (Bukkit.isPrimaryThread()) {
            return executeAuthmeCommand("password " + username + " " + newPassword);
        } else {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    executeAuthmeCommand("password " + username + " " + newPassword)
                ).get();
            } catch (Exception e) {
                debugLog("Failed to change password in AuthMe: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Synchronize approved users between VerifyMC and AuthMe.
     * Rules:
     * - Only approved local users are allowed to sync from local to AuthMe.
     * - If AuthMe has user while local missing or pending, create/update local to approved.
     */
    public void syncApprovedUsers() {
        if (!isAuthmeEnabled() || !isDatabaseMode() || userDao == null) {
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

            List<String> authmeUsers = listAuthmeUsers();
            Map<String, String> authmeByLowerName = new HashMap<>();
            for (String name : authmeUsers) {
                authmeByLowerName.put(name.toLowerCase(), name);
            }

            // local approved -> authme
            for (Map<String, Object> local : localUsers) {
                String status = (String) local.get("status");
                String username = (String) local.get("username");
                String password = (String) local.get("password");
                if (username == null || !"approved".equals(status)) {
                    continue;
                }
                if (!authmeByLowerName.containsKey(username.toLowerCase()) && password != null && !password.trim().isEmpty()) {
                    upsertAuthmeUser(username, password);
                }
            }

            // authme -> local approved
            for (String authName : authmeUsers) {
                Map<String, Object> local = localByLowerName.get(authName.toLowerCase());
                if (local == null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(authName);
                    UUID id = offlinePlayer.getUniqueId();
                    userDao.registerUser(id.toString(), authName, "", "approved");
                    continue;
                }
                String status = (String) local.get("status");
                if (!"approved".equals(status)) {
                    String uuid = (String) local.get("uuid");
                    if (uuid != null) {
                        userDao.updateUserStatus(uuid, "approved");
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

    private List<String> listAuthmeUsers() throws Exception {
        List<String> names = new ArrayList<>();
        String sql = "SELECT " + nameColumn() + " FROM " + tableName();
        try (Connection conn = getAuthmeConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    private boolean upsertAuthmeUser(String username, String password) {
        String nameCol = nameColumn();
        String passCol = passwordColumn();
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

        StringBuilder updateSql = new StringBuilder("UPDATE " + tableName() + " SET " + passCol + " = ?, "
            + realNameCol + " = ?, " + regDateCol + " = ?, " + lastLoginCol + " = ?, "
            + ipCol + " = ?, " + regIpCol + " = ?, " + loggedCol + " = 0, " + hasSessionCol + " = 0");
        if (hasSaltColumn()) {
            updateSql.append(", ").append(saltCol).append(" = ?");
        }
        updateSql.append(" WHERE ").append(nameCol).append(" = ?");

        StringBuilder insertColumns = new StringBuilder(nameCol + ", " + realNameCol + ", " + passCol + ", "
            + regDateCol + ", " + lastLoginCol + ", " + ipCol + ", " + regIpCol + ", " + loggedCol
            + ", " + hasSessionCol + ", " + xCol + ", " + yCol + ", " + zCol + ", " + worldCol
            + ", " + yawCol + ", " + pitchCol + ", " + emailCol);
        StringBuilder insertValues = new StringBuilder("?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, 0, ?, 0, 0, ?");
        if (hasSaltColumn()) {
            insertColumns.append(", ").append(saltCol);
            insertValues.append(", ?");
        }
        String insertSql = "INSERT INTO " + tableName() + " (" + insertColumns + ") VALUES (" + insertValues + ")";

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

            if (exists) {
                try (PreparedStatement update = conn.prepareStatement(updateSql.toString())) {
                    int idx = 1;
                    update.setString(idx++, storedPassword);
                    update.setString(idx++, username);
                    update.setLong(idx++, now);
                    update.setLong(idx++, now);
                    update.setString(idx++, loopback);
                    update.setString(idx++, loopback);
                    if (hasSaltColumn()) {
                        update.setString(idx++, "");
                    }
                    update.setString(idx, username);
                    return update.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    int idx = 1;
                    insert.setString(idx++, username);
                    insert.setString(idx++, username);
                    insert.setString(idx++, storedPassword);
                    insert.setLong(idx++, now);
                    insert.setLong(idx++, now);
                    insert.setString(idx++, loopback);
                    insert.setString(idx++, loopback);
                    insert.setString(idx++, "world");
                    insert.setString(idx++, "");
                    if (hasSaltColumn()) {
                        insert.setString(idx++, "");
                    }
                    return insert.executeUpdate() > 0;
                }
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
        String sql;
        if (hasSaltColumn()) {
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

    private String buildStoredPassword(String plainPassword) {
        String format = plugin.getConfig().getString("authme.database.password_format", "sha256").toLowerCase();
        switch (format) {
            case "sha256":
                return authmeSha256(plainPassword);
            case "md5vb":
                return authmeMd5vb(plainPassword);
            case "plaintext":
            default:
                return plainPassword;
        }
    }

    private String authmeSha256(String plainPassword) {
        String salt = generateHexSalt(getSaltLength());
        return "$SHA$" + salt + "$" + sha256Hex(sha256Hex(plainPassword) + salt);
    }

    private String authmeMd5vb(String plainPassword) {
        String salt = generateHexSalt(getSaltLength());
        return "$MD5vb$" + salt + "$" + md5Hex(md5Hex(plainPassword) + salt);
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

    private String md5Hex(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
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

    private boolean executeAuthmeCommand(String command) {
        try {
            debugLog("Executing AuthMe command: " + command);
            boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "authme " + command);
            debugLog("AuthMe command result: " + result);
            return result;
        } catch (Exception e) {
            debugLog("Exception executing AuthMe command: " + e.getMessage());
            return false;
        }
    }

    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] AuthmeService: " + msg);
        }
    }
}
