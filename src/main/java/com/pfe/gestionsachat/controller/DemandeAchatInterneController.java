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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/demandes", produces = "application/json")
@CrossOrigin(origins = "*")
public class DemandeAchatInterneController {

    @Autowired
    private DemandeAchatInterneService demandeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemandeAchatInterneRepository demandeRepository;

    @Autowired
    private OffreFournisseurRepository offreRepository;

    @GetMapping
    public ResponseEntity<List<DemandeAchatInterne>> getAll() {
        return ResponseEntity.ok(demandeRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<DemandeAchatInterne> create(@RequestBody @NonNull DemandeAchatInterne demande, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.createDemande(demande, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeAchatInterne> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeRepository.findById(id).orElseThrow());
    }

    @PostMapping("/{id}/soumettre")
    public ResponseEntity<DemandeAchatInterne> soumettre(@PathVariable @NonNull Long id, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.soumettre(id, user));
    }

    @PutMapping("/{id}/valider-n1")
    public ResponseEntity<DemandeAchatInterne> validerN1(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerN1(id, valider, commentaire, user));
    }

    @PutMapping("/{id}/valider-technicien")
    public ResponseEntity<DemandeAchatInterne> validerTechnicien(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerTechnicien(id, valider, commentaire, user));
    }

    @PutMapping("/{id}/valoriser-achat")
    public ResponseEntity<DemandeAchatInterne> valoriserDemande(
            @PathVariable Long id, 
            @RequestParam java.math.BigDecimal prixUnitaire, 
            @RequestParam Integer supplierId) {
        return ResponseEntity.ok(demandeService.valoriserDemande(id, prixUnitaire, supplierId));
    }

    @PutMapping("/{id}/traiter-achat")
    public ResponseEntity<DemandeAchatInterne> traiterAchat(@PathVariable @NonNull Long id, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.traiterAchat(id, user));
    }

    @PutMapping("/{id}/valider-amg")
    public ResponseEntity<DemandeAchatInterne> validerAMG(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerAMG(id, valider, commentaire, user));
    }

    @PutMapping("/{id}/valider-daf")
    public ResponseEntity<DemandeAchatInterne> validerDAF(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerDAF(id, valider, commentaire, user));
    }

    @PutMapping("/{id}/valider-dg")
    public ResponseEntity<DemandeAchatInterne> validerDG(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean valider = (boolean) payload.get("valider");
        String commentaire = (String) payload.get("commentaire");
        return ResponseEntity.ok(demandeService.validerDG(id, valider, commentaire, user));
    }

    @PostMapping("/{id}/ajustement")
    public ResponseEntity<DemandeAchatInterne> ajustement(@PathVariable @NonNull Long id, @RequestParam @NonNull com.pfe.gestionsachat.model.TypeAjustement type, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.ajustementBudget(id, type, user));
    }

    @PostMapping("/{id}/creer-po")
    public ResponseEntity<com.pfe.gestionsachat.model.PurchaseOrder> creerPO(@PathVariable @NonNull Long id, @RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.creerPO(id, user));
    }

    @GetMapping("/mes-demandes")
    public ResponseEntity<List<DemandeAchatInterne>> getMesDemandes(@RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.getMesDemandes(user));
    }

    @GetMapping("/a-valider")
    public ResponseEntity<List<DemandeAchatInterne>> getAValider(@RequestParam @NonNull Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(demandeService.getDemandesAValider(user));
    }
    @GetMapping("/{id}/offres")
    public ResponseEntity<List<OffreFournisseur>> getOffres(@PathVariable Long id) {
        return ResponseEntity.ok(offreRepository.findByDa_Id(id));
    }
}
