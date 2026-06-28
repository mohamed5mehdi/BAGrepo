package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.DemandeAchatInterne;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.DemandeAchatInterneRepository;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.repository.OffreFournisseurRepository;
import com.pfe.gestionsachat.model.OffreFournisseur;
import com.pfe.gestionsachat.service.DemandeAchatInterneService;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/demandes", produces = "application/json")

public class DemandeAchatInterneController {

    @Autowired
    private DemandeAchatInterneService demandeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemandeAchatInterneRepository demandeRepository;

    @Autowired
    private OffreFournisseurRepository offreRepository;

    // RBAC Niv.2 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','COMPTABLE','DAF','DG','RESP_ACHAT','MANAGER_N1','ADMINISTRATEUR')")
    @GetMapping
    public ResponseEntity<List<DemandeAchatInterne>> getAll() {
        return ResponseEntity.ok(demandeRepository.findAll());
    }

    // RBAC Niv.4 — audit session 3
    @PreAuthorize("hasAnyRole('EMPLOYE', 'ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<DemandeAchatInterne> create(@RequestBody @NonNull DemandeAchatInterne demande, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.createDemande(demande, user));
    }

    // RBAC Niv.2 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','COMPTABLE','DAF','DG','RESP_ACHAT','MANAGER_N1','ADMINISTRATEUR')")
    @GetMapping("/{id}")
    public ResponseEntity<DemandeAchatInterne> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeRepository.findById(id).orElseThrow());
    }

    // RBAC Niv.4 — audit session 3
    @PreAuthorize("hasAnyRole('EMPLOYE', 'ADMINISTRATEUR')")
    @PostMapping("/{id}/soumettre")
    public ResponseEntity<DemandeAchatInterne> soumettre(@PathVariable @NonNull Long id, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.soumettre(id, user));
    }

    @PreAuthorize("hasAnyRole('MANAGER_N1', 'ADMINISTRATEUR')")
    @PutMapping("/{id}/valider-n1")
    public ResponseEntity<DemandeAchatInterne> validerN1(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerN1(id, valider, commentaire, user));
    }

    @PreAuthorize("hasAnyRole('TECHNICIEN', 'ADMINISTRATEUR')")
    @PutMapping("/{id}/valider-technicien")
    public ResponseEntity<DemandeAchatInterne> validerTechnicien(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerTechnicien(id, valider, commentaire, user));
    }

    @PreAuthorize("hasAnyRole('ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'ADMINISTRATEUR')")
    @PutMapping("/{id}/valoriser-achat")
    public ResponseEntity<DemandeAchatInterne> valoriserDemande(
            @PathVariable Long id, 
            @RequestParam java.math.BigDecimal prixUnitaire, 
            @RequestParam Integer supplierId) {
        return ResponseEntity.ok(demandeService.valoriserDemande(id, prixUnitaire, supplierId));
    }

    @PreAuthorize("hasAnyRole('ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'ADMINISTRATEUR')")
    @PutMapping("/{id}/traiter-achat")
    public ResponseEntity<DemandeAchatInterne> traiterAchat(@PathVariable @NonNull Long id, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.traiterAchat(id, user));
    }

    @PreAuthorize("hasAnyRole('AMG', 'ADMINISTRATEUR')")
    @PutMapping("/{id}/valider-amg")
    public ResponseEntity<DemandeAchatInterne> validerAMG(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerAMG(id, valider, commentaire, user));
    }

    @PreAuthorize("hasAnyRole('DAF', 'ADMINISTRATEUR')")
    @PutMapping("/{id}/valider-daf")
    public ResponseEntity<DemandeAchatInterne> validerDAF(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerDAF(id, valider, commentaire, user));
    }

    @PreAuthorize("hasAnyRole('DG', 'ADMINISTRATEUR')")
    @PutMapping("/{id}/valider-dg")
    public ResponseEntity<DemandeAchatInterne> validerDG(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerDG(id, valider, commentaire, user));
    }

    @PreAuthorize("hasAnyRole('ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'ADMINISTRATEUR')")
    @PostMapping("/{id}/ajustement")
    public ResponseEntity<DemandeAchatInterne> ajustement(
            @PathVariable @NonNull Long id, 
            @RequestParam @NonNull com.pfe.gestionsachat.model.TypeAjustement type, 
            @RequestParam @NonNull Integer userId,
            @RequestParam(required = false) java.math.BigDecimal montantDemande,
            @RequestParam(required = false) Integer sourceSousFamilleId,
            @RequestParam(required = false) Integer cibleSousFamilleId,
            @RequestParam(required = false) Integer familleCibleId,
            @RequestParam(required = false) String justification) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.ajustementBudget(id, type, montantDemande, sourceSousFamilleId, cibleSousFamilleId, familleCibleId, justification, user));
    }

    @PreAuthorize("hasAnyRole('DG', 'ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'ADMINISTRATEUR')")
    @PostMapping("/{id}/creer-po")
    public ResponseEntity<com.pfe.gestionsachat.model.PurchaseOrder> creerPO(@PathVariable @NonNull Long id, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.creerPO(id, user));
    }

    // RBAC Niv.2 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','COMPTABLE','DAF','DG','RESP_ACHAT','MANAGER_N1','ADMINISTRATEUR')")
    @GetMapping("/mes-demandes")
    public ResponseEntity<List<DemandeAchatInterne>> getMesDemandes(@RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.getMesDemandes(user));
    }

    // RBAC Niv.2 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','COMPTABLE','DAF','DG','RESP_ACHAT','MANAGER_N1','ADMINISTRATEUR')")
    @GetMapping("/a-valider")
    public ResponseEntity<List<DemandeAchatInterne>> getAValider(@RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.getDemandesAValider(user));
    }
    // RBAC Niv.2 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','COMPTABLE','DAF','DG','RESP_ACHAT','MANAGER_N1','ADMINISTRATEUR')")
    @GetMapping("/{id}/offres")
    public ResponseEntity<List<OffreFournisseur>> getOffres(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.getOffresByDemande(id));
    }

    /**
     * GET /api/demandes/offres/all
     * Retourne TOUTES les offres (devis) pour le Centre de Documents (Administrateur/Acheteur)
     */
    // RBAC Niv.2 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','COMPTABLE','DAF','DG','RESP_ACHAT','MANAGER_N1','ADMINISTRATEUR')")
    @GetMapping("/offres/all")
    public ResponseEntity<List<OffreFournisseur>> getAllOffres() {
        return ResponseEntity.ok(demandeService.getAllOffres());
    }

    public static class OffreRequest {
        public Integer fournisseurId;
        public java.math.BigDecimal prixPropose;
        public String conditions;
        public Integer delai;
    }

    // RBAC Niv.4 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR', 'ADMINISTRATEUR')")
    @PostMapping("/{id}/offres")
    public ResponseEntity<OffreFournisseur> addOffre(@PathVariable Long id, @RequestBody OffreRequest request) {
        return ResponseEntity.ok(demandeService.addOffre(id, request.fournisseurId, request.prixPropose, request.conditions, request.delai));
    }
}

