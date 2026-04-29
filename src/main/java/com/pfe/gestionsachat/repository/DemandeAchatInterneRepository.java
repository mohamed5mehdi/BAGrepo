package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.DemandeAchatInterne;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeAchatInterneRepository extends JpaRepository<DemandeAchatInterne, Long> {
    List<DemandeAchatInterne> findByDemandeur(User demandeur);
    List<DemandeAchatInterne> findByStatut(StatutDemande statut);
    List<DemandeAchatInterne> findByStatutIn(List<StatutDemande> statuts);
}
