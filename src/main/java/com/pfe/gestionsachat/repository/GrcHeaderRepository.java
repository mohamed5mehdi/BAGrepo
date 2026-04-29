package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.GrcHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrcHeaderRepository extends JpaRepository<GrcHeader, Long> {
    java.util.Optional<GrcHeader> findByGrnHeader(com.pfe.gestionsachat.model.GrnHeader grnHeader);
}
