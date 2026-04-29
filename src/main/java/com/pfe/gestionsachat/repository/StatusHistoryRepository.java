package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByEntiteTypeAndEntiteIdOrderByDateModificationDesc(String entiteType, Long entiteId);
}
