package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntiteAndEntiteIdOrderByDateActionDesc(String entite, Long entiteId);
}
