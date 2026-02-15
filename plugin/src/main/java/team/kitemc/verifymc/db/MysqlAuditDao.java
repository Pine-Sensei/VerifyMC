package team.kitemc.verifymc.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MysqlAuditDao extends BaseAuditDao {
    private final Connection conn;

    public MysqlAuditDao(Properties mysqlConfig) throws SQLException {
        String url = "jdbc:mysql://" + mysqlConfig.getProperty("host") + ":" +
                mysqlConfig.getProperty("port") + "/" +
                mysqlConfig.getProperty("database") + "?useSSL=false&characterEncoding=utf8";
        conn = DriverManager.getConnection(url, mysqlConfig.getProperty("user"), mysqlConfig.getProperty("password"));
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS audits (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36)," +
                    "username VARCHAR(32)," +
                    "action VARCHAR(32)," +
                    "operator VARCHAR(32)," +
                    "reason TEXT," +
                    "timestamp BIGINT)");
        }
    }

    @Override
    public void addAudit(AuditRecord audit) {
        validateRecord(audit);
        String sql = "INSERT INTO audits (uuid, username, action, operator, reason, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, audit.uuid());
            ps.setString(2, audit.username());
            ps.setString(3, audit.action());
            ps.setString(4, audit.operator());
            ps.setString(5, audit.reason());
            ps.setLong(6, audit.timestamp());
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    @Override
    public List<AuditRecord> getAllAudits() {
        List<AuditRecord> result = new ArrayList<>();
        String sql = "SELECT * FROM audits";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new AuditRecord(
                        rs.getLong("id"),
                        rs.getString("uuid"),
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getString("operator"),
                        rs.getString("reason"),
                        rs.getLong("timestamp")
                ));
            }
        } catch (SQLException ignored) {}
        return result;
    }

    @Override
    public void save() {
    }
}
