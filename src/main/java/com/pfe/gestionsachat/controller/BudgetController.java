package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.dto.*;
import com.pfe.gestionsachat.exception.BudgetInsuffisantException;
import com.pfe.gestionsachat.exception.EquationBudgetaireException;
import com.pfe.gestionsachat.service.BudgetPiecesService;
import com.pfe.gestionsachat.service.BudgetSuiviService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller REST – Suivi et imputation budgétaire
 *
 * Base URL : /api/budget
 *
 * Équation fondamentale garantie :
 *   opening_val (budget_initial) = consumed_val (budget_engage) + current_val (budget_restant)
 *
 * Deux pools totalement étanches :
 *   - Familles/Sous-Familles : achats généraux (isPieceRechange=false)
 *   - Pièces               : achats pièces de rechange (isPieceRechange=true)
 */
@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private static final Logger log = LoggerFactory.getLogger(BudgetController.class);

    @Autowired private BudgetSuiviService  budgetSuiviService;
    @Autowired private BudgetPiecesService budgetPiecesService;

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/budget/suivi
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Tableau de bord budgétaire global (familles + sous-familles).
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG
     */
    @GetMapping("/suivi")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG')")
    public ResponseEntity<List<BudgetFamilleDto>> getSuiviBudgetaire() {
        log.info("GET /api/budget/suivi");
        return ResponseEntity.ok(budgetSuiviService.getSuiviBudgetaire());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/budget/consommer
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Imputation d'une DA validée sur une sous-famille (achats généraux uniquement).
     * Rôles autorisés : FINANCIER, ADMIN
     */
    @PostMapping("/consommer")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN')")
    public ResponseEntity<?> consommerBudget(@Valid @RequestBody ConsommerBudgetRequest request) {
        log.info("POST /api/budget/consommer – DA={} SF={} montant={}",
                request.getDaId(), request.getSousFamilleId(), request.getMontant());
        try {
            ConsommerBudgetResponse response = budgetSuiviService.consommerBudget(request);
            return ResponseEntity.ok(response);

        } catch (BudgetInsuffisantException e) {
            log.warn("Budget insuffisant : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(erreur("BUDGET_INSUFFISANT", e.getMessage(),
                            Map.of(
                                    "sous_famille_id", e.getSousFamilleId(),
                                    "budget_restant",  e.getBudgetRestant(),
                                    "montant_demande", e.getMontantDemande())));

        } catch (EquationBudgetaireException e) {
            log.error("Rupture équation budgétaire : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(erreur("EQUATION_BUDGETAIRE_ROMPUE", e.getMessage(),
                            Map.of(
                                    "entite",      e.getEntite(),
                                    "entite_id",   e.getEntiteId(),
                                    "opening_val", e.getBudgetInitial(),
                                    "consumed_val",e.getBudgetEngage(),
                                    "current_val", e.getBudgetRestant())));

        } catch (IllegalArgumentException e) {
            log.warn("Argument invalide : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(erreur("RESSOURCE_INTROUVABLE", e.getMessage(), null));

        } catch (IllegalStateException e) {
            log.warn("Statut DA incompatible : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(erreur("STATUT_DA_INCOMPATIBLE", e.getMessage(), null));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/budget/famille/{id}
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Détail d'une famille budgétaire avec toutes ses sous-familles.
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG, ACHETEUR
     */
    @GetMapping("/famille/{id}")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG','ACHETEUR')")
    public ResponseEntity<?> getFamilleDetail(@PathVariable Integer id) {
        log.info("GET /api/budget/famille/{}", id);
        try {
            return ResponseEntity.ok(budgetSuiviService.getFamilleDetail(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(erreur("RESSOURCE_INTROUVABLE", e.getMessage(), null));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/budget/alertes
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Liste toutes les familles et sous-familles dont le taux de consommation dépasse 80%.
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG
     */
    @GetMapping("/alertes")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG')")
    public ResponseEntity<List<AlerteBudgetaireDto>> getAlertes() {
        log.info("GET /api/budget/alertes");
        return ResponseEntity.ok(budgetSuiviService.getAlertes());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/budget/pieces
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * État du pool budgétaire global dédié aux pièces de rechange (exercice courant).
     * Pool totalement étanche du circuit Famille/Sous-Famille.
     *
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG
     */
    @GetMapping("/pieces")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG')")
    public ResponseEntity<?> getBudgetPieces() {
        log.info("GET /api/budget/pieces");
        try {
            return ResponseEntity.ok(budgetPiecesService.getEtatBudgetPieces());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(erreur("POOL_PIECES_INTROUVABLE", e.getMessage(), null));
        }
    }

    /**
     * État du pool budgétaire pièces pour un exercice donné.
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG
     */
    @GetMapping("/pieces/{exercice}")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG')")
    public ResponseEntity<?> getBudgetPiecesParExercice(@PathVariable String exercice) {
        log.info("GET /api/budget/pieces/{}", exercice);
        try {
            return ResponseEntity.ok(budgetPiecesService.getEtatBudgetPiecesParExercice(exercice));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(erreur("POOL_PIECES_INTROUVABLE", e.getMessage(), null));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/budget/allocation/verification
    // GET /api/budget/allocation/verification/{familleId}
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie l'invariant Σ SubFamily.budgetInitial = Family.budgetInitial pour toutes les familles.
     *
     * NON BLOQUANT — retourne la liste des écarts pour affichage rouge dans l'interface.
     * niveau = "OK"          → équation respectée
     * niveau = "SOUS_ALLOUE" → Σ SF < Family (budget non entièrement distribué)
     * niveau = "SUR_ALLOUE"  → Σ SF > Family (les SF dépassent l'enveloppe — CRITIQUE)
     *
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG
     */
    @GetMapping("/allocation/verification")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG')")
    public ResponseEntity<List<AlerteAllocationDto>> verifierAllocationGlobale() {
        log.info("GET /api/budget/allocation/verification");
        return ResponseEntity.ok(budgetSuiviService.verifierAllocationGlobale());
    }

    /**
     * Vérifie l'invariant Σ SF.budgetInitial = Family.budgetInitial pour une famille donnée.
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG
     */
    @GetMapping("/allocation/verification/{familleId}")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG')")
    public ResponseEntity<?> verifierAllocationFamille(@PathVariable Integer familleId) {
        log.info("GET /api/budget/allocation/verification/{}", familleId);
        try {
            return ResponseEntity.ok(budgetSuiviService.verifierAllocationFamilleParId(familleId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(erreur("RESSOURCE_INTROUVABLE", e.getMessage(), null));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helper – structure d'erreur uniforme
    // ══════════════════════════════════════════════════════════════════════════

    private Map<String, Object> erreur(String code, String message, Map<String, Object> details) {
        if (details != null) {
            return Map.of(
                    "code",      code,
                    "message",   message,
                    "details",   details,
                    "timestamp", LocalDateTime.now().toString());
        }
        return Map.of(
                "code",      code,
                "message",   message,
                "timestamp", LocalDateTime.now().toString());
    }
}
