package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.DaHeader;
import com.pfe.gestionsachat.model.ValidationDecision;
import com.pfe.gestionsachat.model.PurchaseOrder;
import com.pfe.gestionsachat.service.AchatWorkflowOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin(origins = "*")
public class AchatWorkflowController {

    @Autowired
    private AchatWorkflowOrchestrator workflowOrchestrator;

    @PostMapping("/validate")
    public ResponseEntity<DaHeader> validate(
            @RequestParam @NonNull Integer daId,
            @RequestParam @NonNull Integer userId,
            @RequestParam @NonNull ValidationDecision decision,
            @RequestParam(required = false) String motif) {
        return ResponseEntity.ok(workflowOrchestrator.processValidation(daId, userId, decision, motif));
    }

    @PostMapping("/check-budget")
    public ResponseEntity<AchatWorkflowOrchestrator.BudgetCheckResult> checkBudget(
            @RequestParam @NonNull Integer daId,
            @RequestParam @NonNull Integer acheteurId) {
        return ResponseEntity.ok(workflowOrchestrator.verifierBudget(daId, acheteurId));
    }

    @PostMapping("/adjust-subfamily")
    public ResponseEntity<DaHeader> adjustSubFamily(
            @RequestParam @NonNull Integer daId,
            @RequestParam @NonNull Integer dafId,
            @RequestParam @NonNull Integer sourceId,
            @RequestParam @NonNull Integer cibleId,
            @RequestParam @NonNull java.math.BigDecimal montant) {
        return ResponseEntity.ok(workflowOrchestrator.ajusterBudgetSousFamille(daId, dafId, sourceId, cibleId, montant));
    }

    @PostMapping("/adjust-family")
    public ResponseEntity<DaHeader> adjustFamily(
            @RequestParam @NonNull Integer daId,
            @RequestParam @NonNull Integer dgId,
            @RequestParam @NonNull Integer cibleId,
            @RequestParam @NonNull java.math.BigDecimal montant) {
        return ResponseEntity.ok(workflowOrchestrator.ajusterBudgetFamille(daId, dgId, cibleId, montant));
    }
    @PostMapping("/create-po")
    public ResponseEntity<PurchaseOrder> createPO(
            @RequestParam @NonNull Integer daId,
            @RequestParam @NonNull Integer acheteurId) {
        return ResponseEntity.ok(workflowOrchestrator.manualCreatePO(daId, acheteurId));
    }

    @PostMapping("/request-adjustment")
    public ResponseEntity<DaHeader> requestAdjustment(
            @RequestParam @NonNull Integer daId,
            @RequestParam @NonNull Integer acheteurId,
            @RequestParam @NonNull String type,
            @RequestParam(required = false) String motif) {
        return ResponseEntity.ok(workflowOrchestrator.solliciterAjustement(daId, acheteurId, type, motif));
    }
}
