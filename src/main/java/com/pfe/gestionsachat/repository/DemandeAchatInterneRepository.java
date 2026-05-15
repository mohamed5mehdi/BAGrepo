package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.DemandeAchatInterne;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeAchatInterneRepository extends JpaRepository<DemandeAchatInterne, Long> {
    List<DemandeAchatInterne> findByDemandeur(User demandeur);
    
    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille"})
    List<DemandeAchatInterne> findByStatut(StatutDemande statut);
    
    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille"})
    List<DemandeAchatInterne> findByStatutIn(List<StatutDemande> statuts);

    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille", "demandeur"})
    List<DemandeAchatInterne> findByStatutAndDemandeur_N1(StatutDemande statut, User n1);

    java.util.Optional<DemandeAchatInterne> findBySubmissionToken(String token);

    java.util.Optional<DemandeAchatInterne> findFirstByDemandeurAndDesignationAndQuantiteOrderByDateCreationDesc(User demandeur, String designation, Integer quantite);

    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille"})
    java.util.Optional<DemandeAchatInterne> findById(Long id);
}
