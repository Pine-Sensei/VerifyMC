package team.kitemc.verifymc.domain.service;

import team.kitemc.verifymc.infrastructure.config.ConfigurationService;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.domain.repository.UserRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class AuthmeIntegrationService {
    private final ConfigurationService configService;
    private final UserRepository userRepository;
    private final boolean debug;

    public AuthmeIntegrationService(ConfigurationService configService, UserRepository userRepository) {
        this.configService = configService;
        this.userRepository = userRepository;
        this.debug = configService.isDebug();
    }

    public boolean isEnabled() {
        return configService.getBoolean("authme.enabled", false);
    }

    public boolean isPasswordRequired() {
        return configService.getBoolean("authme.require_password", false);
    }

    public boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        String regex = configService.getString("authme.password_regex", "^[a-zA-Z0-9_]{8,26}$");
        return Pattern.matches(regex, password);
    }

    public boolean registerToAuthme(String username, String password) {
        if (!isEnabled()) {
            debugLog("AuthMe not enabled, skipping registration");
            return false;
        }
        return upsertAuthmeUser(username, password);
    }

    public boolean unregisterFromAuthme(String username) {
        if (!isEnabled()) {
            debugLog("AuthMe not enabled, skipping unregistration");
            return false;
        }
        return deleteAuthmeUser(username);
    }

    public boolean changePasswordInAuthme(String username, String newPassword) {
        if (!isEnabled()) {
            debugLog("AuthMe not enabled, skipping password change");
            return false;
        }
        return updateAuthmePassword(username, newPassword);
    }

    public void syncApprovedUsers() {
        if (!isEnabled()) {
            return;
        }
        try {
            List<User> localUsers = userRepository.findAll();
            Map<String, User> localByLowerName = new HashMap<>();
            for (User u : localUsers) {
                if (u.getUsername() != null) {
                    localByLowerName.put(u.getUsername().toLowerCase(), u);
                }
            }

            Map<String, AuthmeProfile> authmeProfilesByName = listAuthmeProfiles();
            Map<String, String> authmeByLowerName = new HashMap<>();
            for (String name : authmeProfilesByName.keySet()) {
                authmeByLowerName.put(name.toLowerCase(), name);
            }

            for (User local : localUsers) {
                String username = local.getUsername();
                String password = local.getPassword();
                String email = local.getEmail();
                if (username == null || !local.isApproved()) {
                    continue;
                }
                String authName = authmeByLowerName.get(username.toLowerCase());
                if (authName == null && password != null && !password.trim().isEmpty()) {
                    upsertAuthmeUser(username, password);
                    continue;
                }

                if (authName != null) {
                    AuthmeProfile profile = authmeProfilesByName.get(authName);
                    String authPassword = profile != null ? profile.password : null;
                    String authEmail = profile != null ? profile.email : null;

                    if (password != null && !password.trim().isEmpty() && (authPassword == null || authPassword.trim().isEmpty())) {
                        updateAuthmePassword(authName, password);
                    }

                    if (email != null && !email.trim().isEmpty() && (authEmail == null || authEmail.trim().isEmpty() || !email.equalsIgnoreCase(authEmail))) {
                        updateAuthmeEmail(authName, email);
                    }
                }
            }

            for (Map.Entry<String, AuthmeProfile> entry : authmeProfilesByName.entrySet()) {
                String authName = entry.getKey();
                AuthmeProfile profile = entry.getValue();
                String authPassword = profile != null ? profile.password : null;
                String authEmail = profile != null ? profile.email : null;
                User local = localByLowerName.get(authName.toLowerCase());
                if (local == null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(authName);
                    UUID id = offlinePlayer.getUniqueId();
                    String localEmail = authEmail != null ? authEmail : "";
                    User newUser;
                    if (authPassword != null && !authPassword.trim().isEmpty()) {
                        newUser = new User.Builder()
                                .uuid(id.toString())
                                .username(authName)
                                .email(localEmail)
                                .status(UserStatus.APPROVED)
                                .password(authPassword)
                                .build();
                    } else {
                        newUser = new User.Builder()
                                .uuid(id.toString())
                                .username(authName)
                                .email(localEmail)
                                .status(UserStatus.APPROVED)
                                .build();
                    }
                    userRepository.save(newUser);
                    continue;
                }
                if (!local.isApproved()) {
                    userRepository.updateStatus(local.getUuid(), UserStatus.APPROVED);
                }

                if (authPassword != null && !authPassword.trim().isEmpty()) {
                    String localPassword = local.getPassword();
                    if (localPassword == null || localPassword.trim().isEmpty() || !authPassword.equals(localPassword)) {
                        userRepository.updatePassword(local.getUuid(), authPassword);
                    }
                }

                if (authEmail != null && !authEmail.trim().isEmpty()) {
                    String localEmail = local.getEmail();
                    if (localEmail == null || localEmail.trim().isEmpty() || !authEmail.equalsIgnoreCase(localEmail)) {
                        userRepository.updateEmail(local.getUuid(), authEmail);
                    }
                }
            }
            userRepository.flush();
        } catch (Exception e) {
            debugLog("Failed syncApprovedUsers: " + e.getMessage());
        }
    }

    public String encodePasswordForStorage(String plainOrEncodedPassword) {
        return buildStoredPassword(plainOrEncodedPassword);
    }

    private Connection getAuthmeConnection() throws Exception {
        String type = configService.getString("authme.database.type", "sqlite").toLowerCase();
        if ("sqlite".equals(type)) {
            Class.forName("org.sqlite.JDBC");
            String path = configService.getString("authme.database.sqlite.path", "plugins/AuthMe/authme.db");
            return DriverManager.getConnection("jdbc:sqlite:" + path);
        }

        Class.forName("com.mysql.cj.jdbc.Driver");
        String host = configService.getString("authme.database.mysql.host", "127.0.0.1");
        int port = configService.getInt("authme.database.mysql.port", 3306);
        String database = configService.getString("authme.database.mysql.database", "authme");
        String user = configService.getString("authme.database.mysql.user", "root");
        String password = configService.getString("authme.database.mysql.password", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&characterEncoding=utf8";
        return DriverManager.getConnection(url, user, password);
    }

    private String tableName() {
        return configService.getString("authme.database.table", "authme");
    }

    private String column(String key, String def) {
        return configService.getString("authme.database.columns." + key, def);
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

    private static final class AuthmeProfile {
        private final String password;
        private final String email;

        private AuthmeProfile(String password, String email) {
            this.password = password;
            this.email = email;
        }
    }

    private Map<String, AuthmeProfile> listAuthmeProfiles() throws Exception {
        Map<String, AuthmeProfile> result = new HashMap<>();
        String sql = "SELECT " + nameColumn() + ", " + passwordColumn() + ", " + column("mySQLColumnEmail", "email") + " FROM " + tableName();
        try (Connection conn = getAuthmeConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String username = rs.getString(1);
                if (username != null) {
                    result.put(username, new AuthmeProfile(rs.getString(2), rs.getString(3)));
                }
            }
        }
        return result;
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

    private boolean updateAuthmeEmail(String username, String email) {
        String sql = "UPDATE " + tableName() + " SET " + column("mySQLColumnEmail", "email") + " = ? WHERE " + nameColumn() + " = ?";
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
        return configService.getInt("authme.database.salt_length", 16);
    }

    private String generateHexSalt(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(random.nextInt(16)));
        }
        return sb.toString();
    }

    private String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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
            configService.getLogger().info("[DEBUG] AuthmeIntegrationService: " + msg);
        }
    }
}
