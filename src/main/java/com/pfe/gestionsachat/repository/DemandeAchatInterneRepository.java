package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.DemandeAchatInterne;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeAchatInterneRepository extends JpaRepository<DemandeAchatInterne, Long> {
    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille"})
    List<DemandeAchatInterne> findByDemandeur(User demandeur);
    
    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille"})
    List<DemandeAchatInterne> findByStatut(StatutDemande statut);
    
    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille"})
    List<DemandeAchatInterne> findByStatutIn(List<StatutDemande> statuts);

    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille", "demandeur"})
    List<DemandeAchatInterne> findByStatutAndDemandeur_N1(StatutDemande statut, User n1);

    /**
     * Recherche les demandes filtrées par statut et type (Pièce ou Standard).
     */
    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille", "demandeur"})
    List<DemandeAchatInterne> findByStatutAndIsPieceRechange(StatutDemande statut, Boolean isPieceRechange);

    /**
     * Recherche les demandes du Flux A (Standard) en attente de validation pour un Manager spécifique.
     * Note : Utilise demandeur.n1.oidUser pour correspondre au modèle User actuel.
     */
    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille"})
    @Query("SELECT d FROM DemandeAchatInterne d WHERE d.statut = :statut AND d.isPieceRechange = false AND d.demandeur.n1.oidUser = :managerId")
    List<DemandeAchatInterne> findByStatutAndFluxAAndManagerId(
        @Param("statut") StatutDemande statut,
        @Param("managerId") Integer managerId
    );

    java.util.Optional<DemandeAchatInterne> findBySubmissionToken(String token);

    java.util.Optional<DemandeAchatInterne> findFirstByDemandeurAndDesignationAndQuantiteOrderByDateCreationDesc(User demandeur, String designation, Integer quantite);

    @EntityGraph(attributePaths = {"budgetFamille", "budgetSousFamille"})
    java.util.Optional<DemandeAchatInterne> findById(Long id);
}
