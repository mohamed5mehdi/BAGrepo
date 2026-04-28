package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Integer> {
    Optional<Family> findByLibelle(String libelle);
    boolean existsByLibelle(String libelle);
}