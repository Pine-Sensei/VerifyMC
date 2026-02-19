package team.kitemc.verifymc.db;

import java.sql.*;
import java.util.*;
import org.bukkit.plugin.Plugin;

public class MysqlUserDao implements UserDao {
    private final Connection conn;
    private final ResourceBundle messages;
    private final boolean debug;
    private final Plugin plugin;

    public MysqlUserDao(Properties mysqlConfig, ResourceBundle messages, Plugin plugin) throws SQLException {
        this.messages = messages;
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
        String url = "jdbc:mysql://" + mysqlConfig.getProperty("host") + ":" +
                mysqlConfig.getProperty("port") + "/" +
                mysqlConfig.getProperty("database") + "?useSSL=false&characterEncoding=utf8";
        conn = DriverManager.getConnection(url, mysqlConfig.getProperty("user"), mysqlConfig.getProperty("password"));
        initDatabase();
    }

    public MysqlUserDao(Properties mysqlConfig) throws SQLException {
        this.messages = null;
        this.plugin = null;
        this.debug = false;
        String url = "jdbc:mysql://" + mysqlConfig.getProperty("host") + ":" +
                mysqlConfig.getProperty("port") + "/" +
                mysqlConfig.getProperty("database") + "?useSSL=false&characterEncoding=utf8";
        conn = DriverManager.getConnection(url, mysqlConfig.getProperty("user"), mysqlConfig.getProperty("password"));
        initDatabase();
    }

    private void initDatabase() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(32) PRIMARY KEY," +
                    "email VARCHAR(64)," +
                    "status VARCHAR(16)," +
                    "password VARCHAR(255)," +
                    "regTime BIGINT," +
                    "discord_id VARCHAR(64)," +
                    "questionnaire_score INT NULL," +
                    "questionnaire_passed BOOLEAN NULL," +
                    "questionnaire_review_summary TEXT NULL," +
                    "questionnaire_scored_at BIGINT NULL)");

            try {
                stmt.executeQuery("SELECT password FROM users LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN password VARCHAR(255)");
            }

            try {
                stmt.executeQuery("SELECT regTime FROM users LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN regTime BIGINT");
                stmt.executeUpdate("UPDATE users SET regTime = " + System.currentTimeMillis() + " WHERE regTime IS NULL");
            }

            try {
                stmt.executeQuery("SELECT discord_id FROM users LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN discord_id VARCHAR(64)");
            }

            try {
                stmt.executeQuery("SELECT questionnaire_score FROM users LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN questionnaire_score INT NULL");
            }
            try {
                stmt.executeQuery("SELECT questionnaire_passed FROM users LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN questionnaire_passed BOOLEAN NULL");
            }
            try {
                stmt.executeQuery("SELECT questionnaire_review_summary FROM users LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN questionnaire_review_summary TEXT NULL");
            }
            try {
                stmt.executeQuery("SELECT questionnaire_scored_at FROM users LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN questionnaire_scored_at BIGINT NULL");
            }

            ensureIndex(stmt, "idx_email", "CREATE INDEX idx_email ON users(email)");
        }
    }

    private void ensureIndex(Statement stmt, String indexName, String createIndexSql) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SHOW INDEX FROM users WHERE Key_name = '" + indexName + "'")) {
            if (!rs.next()) {
                stmt.executeUpdate(createIndexSql);
            }
        }
    }

    private void debugLog(String msg) {
        if (debug && plugin != null)
            plugin.getLogger().info("[DEBUG] MysqlUserDao: " + msg);
    }

    @Override
    public boolean registerUser(String username, String email, String status) {
        return registerUser(username, email, status, null, null, null, null);
    }

    @Override
    public boolean registerUser(String username, String email, String status,
            Integer questionnaireScore, Boolean questionnairePassed,
            String questionnaireReviewSummary, Long questionnaireScoredAt) {
        String checkSql = "SELECT username FROM users WHERE username = ?";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, username);
            ResultSet rs = checkPs.executeQuery();
            if (rs.next()) {
                debugLog("User already exists with username: " + username + ", skipping registration");
                return false;
            }
        } catch (SQLException e) {
            debugLog("Error checking existing user: " + e.getMessage());
            return false;
        }

        String sql = "INSERT INTO users (username, email, status, regTime, questionnaire_score, questionnaire_passed, questionnaire_review_summary, questionnaire_scored_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, status);
            ps.setLong(4, System.currentTimeMillis());
            if (questionnaireScore != null)
                ps.setInt(5, questionnaireScore);
            else
                ps.setNull(5, Types.INTEGER);
            if (questionnairePassed != null)
                ps.setBoolean(6, questionnairePassed);
            else
                ps.setNull(6, Types.BOOLEAN);
            ps.setString(7, questionnaireReviewSummary);
            if (questionnaireScoredAt != null)
                ps.setLong(8, questionnaireScoredAt);
            else
                ps.setNull(8, Types.BIGINT);
            ps.executeUpdate();
            debugLog("User registered: " + username);
            return true;
        } catch (SQLException e) {
            debugLog("Error registering user: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean registerUser(String username, String email, String status, String password) {
        return registerUser(username, email, status, password, null, null, null, null);
    }

    @Override
    public boolean registerUser(String username, String email, String status, String password,
            Integer questionnaireScore, Boolean questionnairePassed,
            String questionnaireReviewSummary, Long questionnaireScoredAt) {
        String checkSql = "SELECT username FROM users WHERE username = ?";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, username);
            ResultSet rs = checkPs.executeQuery();
            if (rs.next()) {
                debugLog("User already exists with username: " + username + ", skipping registration");
                return false;
            }
        } catch (SQLException e) {
            debugLog("Error checking existing user: " + e.getMessage());
            return false;
        }

        String sql = "INSERT INTO users (username, email, status, password, regTime, questionnaire_score, questionnaire_passed, questionnaire_review_summary, questionnaire_scored_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, status);
            ps.setString(4, hashPassword(password));
            ps.setLong(5, System.currentTimeMillis());
            if (questionnaireScore != null)
                ps.setInt(6, questionnaireScore);
            else
                ps.setNull(6, Types.INTEGER);
            if (questionnairePassed != null)
                ps.setBoolean(7, questionnairePassed);
            else
                ps.setNull(7, Types.BOOLEAN);
            ps.setString(8, questionnaireReviewSummary);
            if (questionnaireScoredAt != null)
                ps.setLong(9, questionnaireScoredAt);
            else
                ps.setNull(9, Types.BIGINT);
            ps.executeUpdate();
            debugLog("User registered with password: " + username);
            return true;
        } catch (SQLException e) {
            debugLog("Error registering user: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateUserStatus(String username, String status) {
        String sql = "UPDATE users SET status=? WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            debugLog("User status updated: " + username + " to " + status);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error updating user status: " + e.getMessage());
            return false;
        }
    }

    private String hashPassword(String password) {
        if (password == null || password.isEmpty()) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            debugLog("SHA-256 not available, storing password hash failed");
            return null;
        }
    }

    @Override
    public boolean updateUserPassword(String username, String password) {
        String sql = "UPDATE users SET password=? WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(password));
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            debugLog("User password updated: " + username);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error updating user password: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateUserEmail(String username, String email) {
        String sql = "UPDATE users SET email=? WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            debugLog("User email updated: " + username);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error updating user email: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getAllUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            debugLog("Error getting all users: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPendingUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE status='pending'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            debugLog("Error getting pending users: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> mapUserFromResultSet(ResultSet rs) throws SQLException {
        Map<String, Object> user = new HashMap<>();
        user.put("username", rs.getString("username"));
        user.put("email", rs.getString("email"));
        user.put("status", rs.getString("status"));
        user.put("password", rs.getString("password"));
        user.put("regTime", rs.getLong("regTime"));
        user.put("discord_id", rs.getString("discord_id"));
        user.put("questionnaire_score", rs.getObject("questionnaire_score"));
        user.put("questionnaire_passed", rs.getObject("questionnaire_passed"));
        user.put("questionnaire_review_summary", rs.getString("questionnaire_review_summary"));
        user.put("questionnaire_scored_at", rs.getObject("questionnaire_scored_at"));
        return user;
    }

    @Override
    public Map<String, Object> getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username)=LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting user by username: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, Object> getUserByUsernameExact(String username) {
        String sql = "SELECT * FROM users WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting user by username (exact): " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            int rows = ps.executeUpdate();
            debugLog("User deleted: " + username);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error deleting user: " + e.getMessage());
            return false;
        }
    }

    @Override
    public int countUsersByEmail(String email) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email)=LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            debugLog("Error counting users by email: " + e.getMessage());
        }
        return count;
    }

    @Override
    public void save() {
        debugLog("MySQL storage: save() called (no-op)");
    }

    @Override
    public List<Map<String, Object>> getUsersWithPagination(int page, int pageSize) {
        debugLog("Getting users with pagination: page=" + page + ", pageSize=" + pageSize);
        List<Map<String, Object>> result = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        String sql = "SELECT * FROM users ORDER BY regTime DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapUserFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting users with pagination: " + e.getMessage());
        }

        debugLog("Returning " + result.size() + " users for page " + page);
        return result;
    }

    @Override
    public int getTotalUserCount() {
        debugLog("Getting total user count");
        int count = 0;
        String sql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            debugLog("Error getting total user count: " + e.getMessage());
        }

        debugLog("Total user count: " + count);
        return count;
    }

    @Override
    public List<Map<String, Object>> getUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery) {
        debugLog("Getting users with pagination and search: page=" + page + ", pageSize=" + pageSize + ", query=" + searchQuery);
        List<Map<String, Object>> result = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        String sql;
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            sql = "SELECT * FROM users ORDER BY regTime DESC LIMIT ? OFFSET ?";
        } else {
            sql = "SELECT * FROM users WHERE LOWER(username) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) ORDER BY regTime DESC LIMIT ? OFFSET ?";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                ps.setInt(1, pageSize);
                ps.setInt(2, offset);
            } else {
                String searchPattern = "%" + searchQuery.trim() + "%";
                ps.setString(1, searchPattern);
                ps.setString(2, searchPattern);
                ps.setInt(3, pageSize);
                ps.setInt(4, offset);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapUserFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting users with pagination and search: " + e.getMessage());
        }

        debugLog("Returning " + result.size() + " users for page " + page + " with search query: " + searchQuery);
        return result;
    }

    @Override
    public int getTotalUserCountWithSearch(String searchQuery) {
        debugLog("Getting total user count with search: query=" + searchQuery);
        int count = 0;

        String sql;
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            sql = "SELECT COUNT(*) FROM users";
        } else {
            sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?)";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String searchPattern = "%" + searchQuery.trim() + "%";
                ps.setString(1, searchPattern);
                ps.setString(2, searchPattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting total user count with search: " + e.getMessage());
        }

        debugLog("Total user count with search '" + searchQuery + "': " + count);
        return count;
    }

    @Override
    public int getApprovedUserCount() {
        debugLog("Getting approved user count (excluding pending)");
        int count = 0;
        String sql = "SELECT COUNT(*) FROM users WHERE status != 'pending'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            debugLog("Error getting approved user count: " + e.getMessage());
        }

        debugLog("Approved user count: " + count);
        return count;
    }

    @Override
    public int getApprovedUserCountWithSearch(String searchQuery) {
        debugLog("Getting approved user count with search: query=" + searchQuery);
        int count = 0;

        String sql;
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            sql = "SELECT COUNT(*) FROM users WHERE status != 'pending'";
        } else {
            sql = "SELECT COUNT(*) FROM users WHERE status != 'pending' AND (LOWER(username) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?))";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String searchPattern = "%" + searchQuery.trim() + "%";
                ps.setString(1, searchPattern);
                ps.setString(2, searchPattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting approved user count with search: " + e.getMessage());
        }

        debugLog("Approved user count with search '" + searchQuery + "': " + count);
        return count;
    }

    @Override
    public List<Map<String, Object>> getApprovedUsersWithPagination(int page, int pageSize) {
        debugLog("Getting approved users with pagination: page=" + page + ", pageSize=" + pageSize);
        List<Map<String, Object>> result = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        String sql = "SELECT * FROM users WHERE status != 'pending' ORDER BY regTime DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapUserFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting approved users with pagination: " + e.getMessage());
        }

        debugLog("Returning " + result.size() + " approved users for page " + page);
        return result;
    }

    @Override
    public List<Map<String, Object>> getApprovedUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery) {
        debugLog("Getting approved users with pagination and search: page=" + page + ", pageSize=" + pageSize + ", query=" + searchQuery);
        List<Map<String, Object>> result = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        String sql;
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            sql = "SELECT * FROM users WHERE status != 'pending' ORDER BY regTime DESC LIMIT ? OFFSET ?";
        } else {
            sql = "SELECT * FROM users WHERE status != 'pending' AND (LOWER(username) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?)) ORDER BY regTime DESC LIMIT ? OFFSET ?";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                ps.setInt(1, pageSize);
                ps.setInt(2, offset);
            } else {
                String searchPattern = "%" + searchQuery.trim() + "%";
                ps.setString(1, searchPattern);
                ps.setString(2, searchPattern);
                ps.setInt(3, pageSize);
                ps.setInt(4, offset);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapUserFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting approved users with pagination and search: " + e.getMessage());
        }

        debugLog("Returning " + result.size() + " approved users for page " + page + " with search query: " + searchQuery);
        return result;
    }

    @Override
    public boolean updateUserDiscordId(String username, String discordId) {
        debugLog("updateUserDiscordId called: username=" + username + ", discordId=" + discordId);
        String sql = "UPDATE users SET discord_id=? WHERE LOWER(username)=LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, discordId);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            debugLog("User Discord ID updated: " + username + " -> " + discordId + ", rows affected: " + rows);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error updating Discord ID: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getUserByDiscordId(String discordId) {
        debugLog("Getting user by Discord ID: " + discordId);
        String sql = "SELECT * FROM users WHERE discord_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    debugLog("User found with Discord ID: " + rs.getString("username"));
                    return mapUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting user by Discord ID: " + e.getMessage());
        }
        debugLog("User not found with Discord ID: " + discordId);
        return null;
    }

    @Override
    public boolean isDiscordIdLinked(String discordId) {
        debugLog("Checking if Discord ID is linked: " + discordId);
        return getUserByDiscordId(discordId) != null;
    }
}
