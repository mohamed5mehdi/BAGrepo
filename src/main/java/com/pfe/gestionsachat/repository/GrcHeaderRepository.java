package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.GrcHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrcHeaderRepository extends JpaRepository<GrcHeader, Long> {
    java.util.Optional<GrcHeader> findByGrnHeader(com.pfe.gestionsachat.model.GrnHeader grnHeader);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT g FROM GrcHeader g WHERE g.id = :id")
    java.util.Optional<GrcHeader> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);
}
