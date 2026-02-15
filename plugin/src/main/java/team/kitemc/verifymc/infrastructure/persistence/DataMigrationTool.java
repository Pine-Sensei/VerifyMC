package team.kitemc.verifymc.infrastructure.persistence;

import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.domain.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataMigrationTool {
    private final Plugin plugin;
    private final boolean debug;

    public DataMigrationTool(Plugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug", false);
    }

    public MigrationReport migrateFileToMysql(FileUserRepository source, MysqlUserRepository target) {
        debugLog("Starting migration from File to MySQL");
        MigrationReport report = new MigrationReport();
        
        List<User> users = source.findAll();
        report.setTotalCount(users.size());
        debugLog("Found " + users.size() + " users to migrate");

        for (User user : users) {
            try {
                boolean success = migrateUserToMysql(target, user);
                if (success) {
                    report.incrementSuccessCount();
                    debugLog("Migrated user: " + user.getUsername());
                } else {
                    report.incrementSkippedCount();
                    report.addDetail("Skipped (already exists): " + user.getUsername() + " (" + user.getUuid() + ")");
                    debugLog("Skipped user (already exists): " + user.getUsername());
                }
            } catch (Exception e) {
                report.incrementErrorCount();
                report.addDetail("Error: " + user.getUsername() + " (" + user.getUuid() + ") - " + e.getMessage());
                plugin.getLogger().warning("Error migrating user " + user.getUsername() + ": " + e.getMessage());
            }
        }

        debugLog("Migration completed: " + report.getSuccessCount() + " success, " + 
                report.getErrorCount() + " errors, " + report.getSkippedCount() + " skipped");
        return report;
    }

    private boolean migrateUserToMysql(MysqlUserRepository target, User user) {
        return target.save(user);
    }

    public MigrationReport migrateMysqlToFile(MysqlUserRepository source, FileUserRepository target) {
        debugLog("Starting migration from MySQL to File");
        MigrationReport report = new MigrationReport();
        
        List<User> users = source.findAll();
        report.setTotalCount(users.size());
        debugLog("Found " + users.size() + " users to migrate");

        for (User user : users) {
            try {
                boolean success = migrateUserToFile(target, user);
                if (success) {
                    report.incrementSuccessCount();
                    debugLog("Migrated user: " + user.getUsername());
                } else {
                    report.incrementSkippedCount();
                    report.addDetail("Skipped (already exists): " + user.getUsername() + " (" + user.getUuid() + ")");
                    debugLog("Skipped user (already exists): " + user.getUsername());
                }
            } catch (Exception e) {
                report.incrementErrorCount();
                report.addDetail("Error: " + user.getUsername() + " (" + user.getUuid() + ") - " + e.getMessage());
                plugin.getLogger().warning("Error migrating user " + user.getUsername() + ": " + e.getMessage());
            }
        }

        target.flush();
        debugLog("Migration completed: " + report.getSuccessCount() + " success, " + 
                report.getErrorCount() + " errors, " + report.getSkippedCount() + " skipped");
        return report;
    }

    private boolean migrateUserToFile(FileUserRepository target, User user) {
        return target.save(user);
    }

    public MigrationReport migrateWithTransaction(FileUserRepository source, MysqlUserRepository target) {
        debugLog("Starting transactional migration from File to MySQL");
        MigrationReport report = new MigrationReport();
        
        List<User> users = source.findAll();
        report.setTotalCount(users.size());
        debugLog("Found " + users.size() + " users to migrate");

        Connection conn = null;
        try {
            conn = target.getDataSource().getConnection();
            conn.setAutoCommit(false);
            debugLog("Transaction started");

            for (User user : users) {
                try {
                    boolean success = saveUserWithConnection(target, conn, user);
                    if (success) {
                        report.incrementSuccessCount();
                        debugLog("Migrated user: " + user.getUsername());
                    } else {
                        report.incrementSkippedCount();
                        report.addDetail("Skipped (already exists): " + user.getUsername() + " (" + user.getUuid() + ")");
                    }
                } catch (Exception e) {
                    report.incrementErrorCount();
                    report.addDetail("Error: " + user.getUsername() + " (" + user.getUuid() + ") - " + e.getMessage());
                    plugin.getLogger().warning("Error migrating user " + user.getUsername() + ": " + e.getMessage());
                }
            }

            conn.commit();
            debugLog("Transaction committed successfully");
            
        } catch (SQLException e) {
            report.incrementErrorCount();
            report.addDetail("Transaction error: " + e.getMessage());
            plugin.getLogger().severe("Transaction failed, rolling back: " + e.getMessage());
            
            if (conn != null) {
                try {
                    conn.rollback();
                    debugLog("Transaction rolled back");
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().severe("Rollback failed: " + rollbackEx.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    plugin.getLogger().warning("Error closing connection: " + e.getMessage());
                }
            }
        }

        debugLog("Migration completed: " + report.getSuccessCount() + " success, " + 
                report.getErrorCount() + " errors, " + report.getSkippedCount() + " skipped");
        return report;
    }

    private boolean saveUserWithConnection(MysqlUserRepository target, Connection conn, User user) throws SQLException {
        return target.saveWithConnection(conn, user);
    }

    private void debugLog(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] DataMigrationTool: " + msg);
        }
    }

    public static class MigrationReport {
        private int totalCount;
        private int successCount;
        private int errorCount;
        private int skippedCount;
        private final List<String> details = new ArrayList<>();
        private long durationMs;

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void incrementSuccessCount() {
            this.successCount++;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void incrementErrorCount() {
            this.errorCount++;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public void incrementSkippedCount() {
            this.skippedCount++;
        }

        public List<String> getDetails() {
            return new ArrayList<>(details);
        }

        public void addDetail(String detail) {
            details.add(detail);
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public boolean isSuccess() {
            return errorCount == 0;
        }

        public String getSummary() {
            return String.format("Migration completed: %d total, %d success, %d errors, %d skipped, took %d ms",
                    totalCount, successCount, errorCount, skippedCount, durationMs);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("MigrationReport{\n");
            sb.append("  totalCount=").append(totalCount).append(",\n");
            sb.append("  successCount=").append(successCount).append(",\n");
            sb.append("  errorCount=").append(errorCount).append(",\n");
            sb.append("  skippedCount=").append(skippedCount).append(",\n");
            sb.append("  durationMs=").append(durationMs).append(",\n");
            if (!details.isEmpty()) {
                sb.append("  details=[\n");
                for (String detail : details) {
                    sb.append("    ").append(detail).append(",\n");
                }
                sb.append("  ]\n");
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
