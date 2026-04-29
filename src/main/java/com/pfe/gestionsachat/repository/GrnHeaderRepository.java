package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.GrnHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrnHeaderRepository extends JpaRepository<GrnHeader, Long> {
}
