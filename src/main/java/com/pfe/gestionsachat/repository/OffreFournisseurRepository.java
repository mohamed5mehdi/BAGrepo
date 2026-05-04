package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.OffreFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OffreFournisseurRepository extends JpaRepository<OffreFournisseur, Long> {
    List<OffreFournisseur> findByDa_Id(Long daId);
}
