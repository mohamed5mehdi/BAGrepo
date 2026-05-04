package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Integer> {
    Optional<Family> findByLibelle(String libelle);
    boolean existsByLibelle(String libelle);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Family f WHERE f.idFamily = :id")
    java.util.Optional<Family> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Integer id);
}