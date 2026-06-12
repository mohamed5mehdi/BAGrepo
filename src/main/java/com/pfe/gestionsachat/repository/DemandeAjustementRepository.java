package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.DemandeAjustement;
import com.pfe.gestionsachat.model.StatutAjustement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DemandeAjustementRepository extends JpaRepository<DemandeAjustement, Long> {

    List<DemandeAjustement> findByDemandeInterne_Id(Long demandeInterneId);

    List<DemandeAjustement> findByStatut(StatutAjustement statut);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DemandeAjustement d WHERE d.demandeInterne.id = :demandeInterneId AND d.statut IN :statuts")
    boolean existsByDemandeInterneIdAndStatutIn(@org.springframework.data.repository.query.Param("demandeInterneId") Long demandeInterneId, java.util.List<StatutAjustement> statuts);
}
