package team.kitemc.verifymc.db;

import java.util.List;

public interface AuditDao {
    void addAudit(AuditRecord audit);
    List<AuditRecord> getAllAudits();
    void save();
}
