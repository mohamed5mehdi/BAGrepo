package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.DemandeAjustement;
import com.pfe.gestionsachat.service.DemandeAjustementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/ajustement")

public class DemandeAjustementController {

    @Autowired
    private DemandeAjustementService demandeAjustementService;

    @Autowired
    private com.pfe.gestionsachat.repository.DemandeAjustementRepository demandeAjustementRepository;

    // RBAC Niv.4 — audit session 3
    @PreAuthorize("hasAnyRole('DG', 'ADMINISTRATEUR')")
    @PostMapping("/famille/soumettre")
    public ResponseEntity<?> soumettreFamille(@RequestBody Map<String, Object> payload) {
        try {
            Long daId = Long.valueOf(payload.get("daId").toString());
            Integer familleCibleId = java.util.Objects.requireNonNull((Integer) payload.get("familleCibleId"), "familleCibleId est requis");
            BigDecimal montantDemande = new BigDecimal(java.util.Objects.requireNonNull(payload.get("montantDemande"), "montantDemande est requis").toString());
            String justification = (String) payload.get("justification");
            Long acheteurId = Long.valueOf(java.util.Objects.requireNonNull(payload.get("acheteurId"), "acheteurId est requis").toString());

            DemandeAjustement daAjust = demandeAjustementService.soumettreAjustementFamille(daId, familleCibleId, montantDemande, justification, acheteurId);
            return ResponseEntity.ok(daAjust);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // RBAC Niv.4 — audit session 3
    @PreAuthorize("hasAnyRole('DG', 'ADMINISTRATEUR')")
    @PostMapping("/{id}/dg/decider")
    public ResponseEntity<?> deciderDg(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Long dgId = Long.valueOf(java.util.Objects.requireNonNull(payload.get("dgId"), "dgId est requis").toString());
            String decision = java.util.Objects.requireNonNull((String) payload.get("decision"), "decision est requise");
            String justification = (String) payload.get("justification");

            DemandeAjustement daAjust = demandeAjustementService.deciderDg(id, dgId, decision, justification);
            return ResponseEntity.ok(daAjust);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // RBAC Niv.4 — audit session 3
    @PreAuthorize("hasAnyRole('DAF', 'ADMINISTRATEUR')")
    @PostMapping("/sous-famille/soumettre")
    public ResponseEntity<?> soumettreSousFamille(@RequestBody Map<String, Object> payload) {
        try {
            Long daId = Long.valueOf(payload.get("daId").toString());
            Integer sourceSousFamilleId = java.util.Objects.requireNonNull((Integer) payload.get("sourceSousFamilleId"), "sourceSousFamilleId est requis");
            Integer cibleSousFamilleId = java.util.Objects.requireNonNull((Integer) payload.get("cibleSousFamilleId"), "cibleSousFamilleId est requis");
            BigDecimal montantDemande = new BigDecimal(java.util.Objects.requireNonNull(payload.get("montantDemande"), "montantDemande est requis").toString());
            String justification = (String) payload.get("justification");
            Long acheteurId = Long.valueOf(java.util.Objects.requireNonNull(payload.get("acheteurId"), "acheteurId est requis").toString());

            DemandeAjustement daAjust = demandeAjustementService.soumettreAjustementSousFamille(daId, sourceSousFamilleId, cibleSousFamilleId, montantDemande, justification, acheteurId);
            return ResponseEntity.ok(daAjust);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // RBAC Niv.4 — audit session 3
    @PreAuthorize("hasAnyRole('DAF', 'ADMINISTRATEUR')")
    @PostMapping("/{id}/daf/decider")
    public ResponseEntity<?> deciderDaf(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Long dafId = Long.valueOf(java.util.Objects.requireNonNull(payload.get("dafId"), "dafId est requis").toString());
            String decision = java.util.Objects.requireNonNull((String) payload.get("decision"), "decision est requise");
            BigDecimal montantFinal = payload.containsKey("montantFinal") && payload.get("montantFinal") != null ? new BigDecimal(payload.get("montantFinal").toString()) : null;
            String justification = (String) payload.get("justification");

            DemandeAjustement daAjust = demandeAjustementService.deciderDaf(id, dafId, decision, montantFinal, justification);
            return ResponseEntity.ok(daAjust);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // RBAC Niv.4 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR', 'ADMINISTRATEUR')")
    @PostMapping("/{id}/acheteur/confirmer")
    public ResponseEntity<?> confirmerAcheteur(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Long acheteurId = Long.valueOf(java.util.Objects.requireNonNull(payload.get("acheteurId"), "acheteurId est requis").toString());
            DemandeAjustement daAjust = demandeAjustementService.confirmerAcheteur(id, acheteurId);
            return ResponseEntity.ok(daAjust);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // RBAC Niv.4 — audit session 3
    @PreAuthorize("hasAnyRole('AMG', 'ADMINISTRATEUR')")
    @PostMapping("/{id}/amg/finaliser")
    public ResponseEntity<?> finaliserAmg(@PathVariable Long id) {
        try {
            DemandeAjustement daAjust = demandeAjustementService.finaliserAmg(id);
            return ResponseEntity.ok(daAjust);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // RBAC Niv.2 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','COMPTABLE','DAF','DG','RESP_ACHAT','MANAGER_N1','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getDemandeAjustement(@PathVariable Long id) {
        return demandeAjustementRepository.findById(id)
            .map(daAjust -> {
                Map<String, Object> response = Map.of(
                    "budgetAvantDemande", daAjust.getBudgetAvantDemande() != null ? daAjust.getBudgetAvantDemande() : "",
                    "budgetApresDemande", daAjust.getBudgetApresDemande() != null ? daAjust.getBudgetApresDemande() : "",
                    "montantDemande", daAjust.getMontantDemande(),
                    "montantFinal", daAjust.getMontantFinal() != null ? daAjust.getMontantFinal() : daAjust.getMontantDemande(),
                    "justificationAcheteur", daAjust.getJustificationAcheteur(),
                    "justificationValideur", daAjust.getJustificationValideur() != null ? daAjust.getJustificationValideur() : "",
                    "statut", daAjust.getStatut(),
                    "type", daAjust.getType()
                );
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}

