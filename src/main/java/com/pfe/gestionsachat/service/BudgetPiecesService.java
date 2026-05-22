package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.dto.BudgetPiecesDto;
import com.pfe.gestionsachat.exception.BudgetInsuffisantException;
import com.pfe.gestionsachat.exception.EquationBudgetaireException;
import com.pfe.gestionsachat.model.AuditLog;
import com.pfe.gestionsachat.model.BudgetPieces;
import com.pfe.gestionsachat.repository.AuditLogRepository;
import com.pfe.gestionsachat.repository.BudgetPiecesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;

/**
 * Service dédié au pool budgétaire global pièces de rechange.
 *
 * Règle d'isolation stricte : ce service est le SEUL point d'accès au pool Pièces.
 * Aucun autre service ne touche à BudgetPieces directement.
 *
 * Invariant garanti : budget_initial = budget_engage + budget_restant
 */
@Service
public class BudgetPiecesService {

    private static final Logger log = LoggerFactory.getLogger(BudgetPiecesService.class);

    /** Tolérance d'arrondi : 2 centimes */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

    @Autowired private BudgetPiecesRepository budgetPiecesRepository;
    @Autowired private AuditLogRepository     auditLogRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Consommation — imputation d'une demande pièce hors stock
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Impute un montant sur le pool pièces de l'exercice courant.
     * budget_engage  += montant
     * budget_restant -= montant
     *
     * @throws BudgetInsuffisantException   si budget_restant < montant
     * @throws EquationBudgetaireException  si l'équation est rompue après imputation
     * @throws IllegalStateException        si aucun pool pièces n'existe pour l'exercice courant
     */
    @Transactional
    @Retryable(
        retryFor  = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff   = @Backoff(delay = 100, multiplier = 2.0)
    )
    public BudgetPiecesDto consommerBudgetPieces(BigDecimal montant, Long daId) {
        String exercice = String.valueOf(Year.now().getValue());

        BudgetPieces pool = budgetPiecesRepository.findByExerciceWithLock(exercice)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun pool budgétaire Pièces trouvé pour l'exercice " + exercice
                        + ". Le Financier doit initialiser le budget avant toute imputation."));

        BigDecimal restantAvant = orZero(pool.getBudgetRestant());

        if (!pool.hasEnoughBudget(montant)) {
            throw new BudgetInsuffisantException(
                    pool.getId(),
                    restantAvant,
                    montant);
        }

        pool.deductBudget(montant);
        budgetPiecesRepository.save(pool);

        validerEquationPieces(pool);

        log.info("Budget Pièces consommé : DA={} montant={} exercice={} restant_après={}",
                daId, montant, exercice, pool.getBudgetRestant());

        return toDto(pool);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Restitution — annulation / rejet PO sur une DA pièce
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Restitue un montant au pool pièces (suite à annulation ou rejet du PO).
     * budget_engage  -= montant
     * budget_restant += montant
     */
    @Transactional
    @Retryable(
        retryFor  = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff   = @Backoff(delay = 100, multiplier = 2.0)
    )
    public BudgetPiecesDto restituterBudgetPieces(BigDecimal montant, Long daId) {
        String exercice = String.valueOf(Year.now().getValue());

        BudgetPieces pool = budgetPiecesRepository.findByExerciceWithLock(exercice)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun pool budgétaire Pièces pour l'exercice " + exercice));

        pool.addBudget(montant);
        budgetPiecesRepository.save(pool);

        validerEquationPieces(pool);

        log.info("Budget Pièces restitué : DA={} montant={} exercice={} restant_après={}",
                daId, montant, exercice, pool.getBudgetRestant());

        return toDto(pool);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Consultation — état du pool
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retourne l'état du pool pièces de l'exercice courant.
     */
    @Transactional(readOnly = true)
    public BudgetPiecesDto getEtatBudgetPieces() {
        String exercice = String.valueOf(Year.now().getValue());
        BudgetPieces pool = budgetPiecesRepository.findByExercice(exercice)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun pool budgétaire Pièces pour l'exercice " + exercice));
        return toDto(pool);
    }

    /**
     * Retourne l'état du pool pièces d'un exercice donné.
     */
    @Transactional(readOnly = true)
    public BudgetPiecesDto getEtatBudgetPiecesParExercice(String exercice) {
        BudgetPieces pool = budgetPiecesRepository.findByExercice(exercice)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun pool budgétaire Pièces pour l'exercice " + exercice));
        return toDto(pool);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers internes
    // ══════════════════════════════════════════════════════════════════════════

    private void validerEquationPieces(BudgetPieces pool) {
        BigDecimal initial  = orZero(pool.getBudgetInitial());
        BigDecimal engage   = orZero(pool.getBudgetEngage());
        BigDecimal restant  = orZero(pool.getBudgetRestant());
        BigDecimal somme    = engage.add(restant);
        BigDecimal ecart    = initial.subtract(somme).abs();

        if (ecart.compareTo(TOLERANCE) > 0) {
            String message = String.format(
                    "RUPTURE ÉQUATION [BudgetPieces exercice=%s]: initial=%.2f ≠ engage=%.2f + restant=%.2f (Δ=%.2f)",
                    pool.getExercice(), initial, engage, restant, ecart);

            log.error(message);

            AuditLog audit = new AuditLog(
                    "EQUATION_PIECES_ROMPUE",
                    "BudgetPieces",
                    pool.getId().longValue(),
                    null,
                    String.format("initial=%.2f, engage=%.2f, restant=%.2f", initial, engage, restant),
                    message);
            auditLogRepository.save(audit);

            throw new EquationBudgetaireException(
                    "BudgetPieces", pool.getId(), initial, engage, restant);
        }
    }

    private BudgetPiecesDto toDto(BudgetPieces pool) {
        return new BudgetPiecesDto(
                pool.getId(),
                pool.getExercice(),
                pool.getBudgetInitial(),
                pool.getBudgetEngage(),
                pool.getBudgetRestant());
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * Calcule le taux de consommation en pourcentage.
     */
    public static BigDecimal calculerTaux(BigDecimal consomme, BigDecimal initial) {
        if (initial == null || initial.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return consomme.multiply(BigDecimal.valueOf(100))
                       .divide(initial, 2, RoundingMode.HALF_UP);
    }
}
