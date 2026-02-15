package team.kitemc.verifymc.infrastructure.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.domain.model.User;
import team.kitemc.verifymc.domain.model.UserStatus;
import team.kitemc.verifymc.domain.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class MysqlUserRepository implements UserRepository {
    private final HikariDataSource dataSource;
    private final boolean debug;
    private final Plugin plugin;

    public MysqlUserRepository(Properties mysqlConfig, Plugin plugin) {
        this.plugin = plugin;
        this.debug = plugin != null && plugin.getConfig().getBoolean("debug", false);
        this.dataSource = createDataSource(mysqlConfig);
        initializeDatabase();
    }

    public MysqlUserRepository(Properties mysqlConfig) {
        this.plugin = null;
        this.debug = false;
        this.dataSource = createDataSource(mysqlConfig);
        initializeDatabase();
    }

    private HikariDataSource createDataSource(Properties mysqlConfig) {
        HikariConfig config = new HikariConfig();
        String url = "jdbc:mysql://" + mysqlConfig.getProperty("host") + ":" +
                mysqlConfig.getProperty("port") + "/" +
                mysqlConfig.getProperty("database") +
                "?useSSL=false&characterEncoding=utf8&serverTimezone=UTC";
        config.setJdbcUrl(url);
        config.setUsername(mysqlConfig.getProperty("user"));
        config.setPassword(mysqlConfig.getProperty("password"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setPoolName("VerifyMC-Pool");
        return new HikariDataSource(config);
    }

    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "username VARCHAR(32) NOT NULL," +
                    "email VARCHAR(64)," +
                    "status VARCHAR(16)," +
                    "password VARCHAR(255)," +
                    "regTime BIGINT," +
                    "discord_id VARCHAR(64)," +
                    "questionnaire_score INT NULL," +
                    "questionnaire_passed BOOLEAN NULL," +
                    "questionnaire_review_summary TEXT NULL," +
                    "questionnaire_scored_at BIGINT NULL)");

            ensureColumnExists(stmt, "password", "VARCHAR(255)");
            ensureColumnExists(stmt, "regTime", "BIGINT");
            ensureColumnExists(stmt, "discord_id", "VARCHAR(64)");
            ensureColumnExists(stmt, "questionnaire_score", "INT NULL");
            ensureColumnExists(stmt, "questionnaire_passed", "BOOLEAN NULL");
            ensureColumnExists(stmt, "questionnaire_review_summary", "TEXT NULL");
            ensureColumnExists(stmt, "questionnaire_scored_at", "BIGINT NULL");

            ensureIndex(stmt, "idx_username", "CREATE INDEX idx_username ON users(username)");
            ensureIndex(stmt, "idx_email", "CREATE INDEX idx_email ON users(email)");
            ensureIndex(stmt, "idx_discord_id", "CREATE INDEX idx_discord_id ON users(discord_id)");

            debugLog("Database initialized successfully");
        } catch (SQLException e) {
            debugLog("Error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void ensureColumnExists(Statement stmt, String columnName, String columnType) throws SQLException {
        try {
            stmt.executeQuery("SELECT " + columnName + " FROM users LIMIT 1");
            debugLog(columnName + " column already exists");
        } catch (SQLException e) {
            stmt.executeUpdate("ALTER TABLE users ADD COLUMN " + columnName + " " + columnType);
            debugLog("Added " + columnName + " column to users table");
        }
    }

    private void ensureIndex(Statement stmt, String indexName, String createIndexSql) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SHOW INDEX FROM users WHERE Key_name = '" + indexName + "'")) {
            if (!rs.next()) {
                stmt.executeUpdate(createIndexSql);
                debugLog("Added " + indexName + " index to users table");
            }
        }
    }

    private void debugLog(String msg) {
        if (debug && plugin != null) {
            plugin.getLogger().info("[DEBUG] MysqlUserRepository: " + msg);
        }
    }

    @Override
    public boolean save(User user) {
        debugLog("save called: uuid=" + user.getUuid() + ", username=" + user.getUsername());
        String checkSql = "SELECT uuid FROM users WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, user.getUuid());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    return updateUser(conn, user);
                } else {
                    return insertUser(conn, user);
                }
            }
        } catch (SQLException e) {
            debugLog("Error saving user: " + e.getMessage());
            return false;
        }
    }

    private boolean insertUser(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (uuid, username, email, status, password, regTime, discord_id, " +
                "questionnaire_score, questionnaire_passed, questionnaire_review_summary, questionnaire_scored_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUuid());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getStatus().getValue());
            ps.setString(5, user.getPassword());
            ps.setLong(6, user.getRegTime());
            ps.setString(7, user.getDiscordId());
            setNullableInt(ps, 8, user.getQuestionnaireScore());
            setNullableBoolean(ps, 9, user.getQuestionnairePassed());
            ps.setString(10, user.getQuestionnaireReviewSummary());
            setNullableLong(ps, 11, user.getQuestionnaireScoredAt());
            ps.executeUpdate();
            debugLog("User inserted: " + user.getUsername());
            return true;
        }
    }

    private boolean updateUser(Connection conn, User user) throws SQLException {
        String sql = "UPDATE users SET username=?, email=?, status=?, password=?, discord_id=?, " +
                "questionnaire_score=?, questionnaire_passed=?, questionnaire_review_summary=?, questionnaire_scored_at=? " +
                "WHERE uuid=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getStatus().getValue());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getDiscordId());
            setNullableInt(ps, 6, user.getQuestionnaireScore());
            setNullableBoolean(ps, 7, user.getQuestionnairePassed());
            ps.setString(8, user.getQuestionnaireReviewSummary());
            setNullableLong(ps, 9, user.getQuestionnaireScoredAt());
            ps.setString(10, user.getUuid());
            int rows = ps.executeUpdate();
            debugLog("User updated: " + user.getUsername() + ", rows affected: " + rows);
            return rows > 0;
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    private void setNullableBoolean(PreparedStatement ps, int index, Boolean value) throws SQLException {
        if (value != null) {
            ps.setBoolean(index, value);
        } else {
            ps.setNull(index, Types.BOOLEAN);
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.BIGINT);
        }
    }

    @Override
    public Optional<User> findByUuid(String uuid) {
        debugLog("Finding user by UUID: " + uuid);
        String sql = "SELECT * FROM users WHERE uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error finding user by UUID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        debugLog("Finding user by username: " + username);
        String sql = "SELECT * FROM users WHERE LOWER(username)=LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error finding user by username: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<User> findByEmail(String email) {
        debugLog("Finding users by email: " + email);
        List<User> result = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE LOWER(email)=LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error finding users by email: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Optional<User> findByDiscordId(String discordId) {
        debugLog("Finding user by Discord ID: " + discordId);
        String sql = "SELECT * FROM users WHERE discord_id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error finding user by Discord ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        debugLog("Getting all users");
        List<User> result = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            debugLog("Error getting all users: " + e.getMessage());
        }
        debugLog("Total users: " + result.size());
        return result;
    }

    @Override
    public List<User> findPending() {
        debugLog("Getting pending users");
        List<User> result = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE status='pending'";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            debugLog("Error getting pending users: " + e.getMessage());
        }
        debugLog("Found " + result.size() + " pending users");
        return result;
    }

    @Override
    public List<User> findWithPagination(int page, int pageSize) {
        debugLog("Getting users with pagination: page=" + page + ", pageSize=" + pageSize);
        List<User> result = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        String sql = "SELECT * FROM users ORDER BY regTime DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting users with pagination: " + e.getMessage());
        }
        debugLog("Returning " + result.size() + " users for page " + page);
        return result;
    }

    @Override
    public List<User> findApprovedWithPagination(int page, int pageSize) {
        debugLog("Getting approved users with pagination: page=" + page + ", pageSize=" + pageSize);
        List<User> result = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        String sql = "SELECT * FROM users WHERE status != 'pending' ORDER BY regTime DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error getting approved users with pagination: " + e.getMessage());
        }
        debugLog("Returning " + result.size() + " approved users for page " + page);
        return result;
    }

    @Override
    public List<User> search(String query, int page, int pageSize) {
        debugLog("Searching users: query=" + query + ", page=" + page + ", pageSize=" + pageSize);
        List<User> result = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        String sql;
        if (query == null || query.trim().isEmpty()) {
            sql = "SELECT * FROM users ORDER BY regTime DESC LIMIT ? OFFSET ?";
        } else {
            sql = "SELECT * FROM users WHERE LOWER(username) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) ORDER BY regTime DESC LIMIT ? OFFSET ?";
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (query == null || query.trim().isEmpty()) {
                ps.setInt(1, pageSize);
                ps.setInt(2, offset);
            } else {
                String searchPattern = "%" + query.trim() + "%";
                ps.setString(1, searchPattern);
                ps.setString(2, searchPattern);
                ps.setInt(3, pageSize);
                ps.setInt(4, offset);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            debugLog("Error searching users: " + e.getMessage());
        }
        debugLog("Returning " + result.size() + " users for page " + page + " with search query: " + query);
        return result;
    }

    @Override
    public int count() {
        debugLog("Getting total user count");
        int count = 0;
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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
    public int countApproved() {
        debugLog("Getting approved user count (excluding pending)");
        int count = 0;
        String sql = "SELECT COUNT(*) FROM users WHERE status != 'pending'";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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
    public int countByEmail(String email) {
        debugLog("Counting users by email: " + email);
        int count = 0;
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email)=LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            debugLog("Error counting users by email: " + e.getMessage());
        }
        debugLog("Found " + count + " users with email: " + email);
        return count;
    }

    @Override
    public boolean updateStatus(String uuid, UserStatus status) {
        debugLog("updateStatus called: uuid=" + uuid + ", status=" + status);
        String sql = "UPDATE users SET status=? WHERE uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.getValue());
            ps.setString(2, uuid);
            int rows = ps.executeUpdate();
            debugLog("User status updated: " + uuid + " to " + status);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error updating user status: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updatePassword(String uuid, String password) {
        debugLog("updatePassword called: uuid=" + uuid);
        String sql = "UPDATE users SET password=? WHERE uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, password);
            ps.setString(2, uuid);
            int rows = ps.executeUpdate();
            debugLog("User password updated: " + uuid);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error updating user password: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateEmail(String uuid, String email) {
        debugLog("updateEmail called: uuid=" + uuid);
        String sql = "UPDATE users SET email=? WHERE uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, uuid);
            int rows = ps.executeUpdate();
            debugLog("User email updated: " + uuid);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error updating user email: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateDiscordId(String uuid, String discordId) {
        debugLog("updateDiscordId called: uuid=" + uuid + ", discordId=" + discordId);
        String sql = "UPDATE users SET discord_id=? WHERE uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, discordId);
            ps.setString(2, uuid);
            int rows = ps.executeUpdate();
            debugLog("User Discord ID updated: " + uuid + " -> " + discordId);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error updating Discord ID: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(String uuid) {
        debugLog("delete called: uuid=" + uuid);
        String sql = "DELETE FROM users WHERE uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            int rows = ps.executeUpdate();
            debugLog("User deleted: " + uuid);
            return rows > 0;
        } catch (SQLException e) {
            debugLog("Error deleting user: " + e.getMessage());
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

    @Override
    public void flush() {
        debugLog("MySQL storage: flush() called (no-op for MySQL)");
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            debugLog("Connection pool closed");
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public boolean saveWithConnection(Connection conn, User user) throws SQLException {
        debugLog("saveWithConnection called: uuid=" + user.getUuid() + ", username=" + user.getUsername());
        String checkSql = "SELECT uuid FROM users WHERE uuid = ?";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, user.getUuid());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    return updateUser(conn, user);
                } else {
                    return insertUser(conn, user);
                }
            }
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User.Builder()
                .uuid(rs.getString("uuid"))
                .username(rs.getString("username"))
                .email(rs.getString("email"))
                .password(rs.getString("password"))
                .status(UserStatus.fromString(rs.getString("status")))
                .regTime(rs.getLong("regTime"))
                .discordId(rs.getString("discord_id"))
                .questionnaireScore((Integer) rs.getObject("questionnaire_score"))
                .questionnairePassed((Boolean) rs.getObject("questionnaire_passed"))
                .questionnaireReviewSummary(rs.getString("questionnaire_review_summary"))
                .questionnaireScoredAt((Long) rs.getObject("questionnaire_scored_at"))
                .build();
    }
}
