package team.kitemc.verifymc.domain.repository;

import team.kitemc.verifymc.domain.model.AuditRecord;

import java.util.List;

public interface AuditRepository {
    boolean save(AuditRecord record);

    List<AuditRecord> findByUuid(String uuid);

    List<AuditRecord> findByUsername(String username);

    List<AuditRecord> findAll();

    void flush();
}
