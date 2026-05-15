package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service unifié pour les deux flux d'achat interne BAG :
 *
 * Flux A — Achat général (isPieceRechange = false)
 * BROUILLON → SOUMISE → VALIDE_N1 → VALIDE_TECH
 * → VALIDE_AMG → VALIDE_DAF → VALIDE_DG → PO
 *
 * Flux B — Pièces de rechange SAV (isPieceRechange = true)
 * BROUILLON → DISPONIBLE_STOCK (sortie stock auto, fin)
 * OU
 * BROUILLON → EN_TRAITEMENT → valorisation Acheteur
 * → VALIDE_N1 → VALIDE_TECH → VALIDE_AMG → VALIDE_DAF
 * → VALIDE_DG → PO
 */
@Service
public class DemandeAchatInterneService {

    @Autowired
    private DemandeAchatInterneRepository repository;
    @Autowired
    private StockItemRepository stockItemRepository;
    @Autowired
    private PurchaseOrderService poService;
    @Autowired
    private StatusHistoryRepository historyRepository;

    // ── Création ─────────────────────────────────────────────────────────────

    @Transactional
    public DemandeAchatInterne createDemande(DemandeAchatInterne da, User user) {
        da.setDemandeur(user);
        da.setStatut(StatutDemande.BROUILLON);
        da.setDateCreation(LocalDateTime.now());
        DemandeAchatInterne saved = repository.save(da);
        log(saved, null, StatutDemande.BROUILLON, user, "Demande créée");
        return saved;
    }

    // ── Soumission — point de branchement des deux flux ──────────────────────

    @Transactional
    public DemandeAchatInterne soumettre(Long id, User user) {
        DemandeAchatInterne da = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable : " + id));

        if (da.getStatut() != StatutDemande.BROUILLON) {
            throw new IllegalStateException(
                "Seule une demande en BROUILLON peut être soumise. Statut actuel : " + da.getStatut());
        }

        StatutDemande statutPrecedent = da.getStatut();

        if (Boolean.TRUE.equals(da.getIsPieceRechange())) {
            // ── Flux B : pièce de rechange (Vérification Stock) ───────────
            if (da.getItemCode() == null || da.getItemCode().isBlank()) {
                throw new IllegalArgumentException("itemCode obligatoire pour une demande de pièce de rechange.");
            }

            Optional<StockItem> stockOpt = stockItemRepository.findByItemCode(da.getItemCode()).stream().findFirst();

            boolean disponible = stockOpt.isPresent() && stockOpt.get().getQuantityAvailable() >= da.getQuantite();

            if (disponible) {
                // Sortie de stock automatique avec verrou pessimiste
                StockItem stock = stockItemRepository.findByItemCodeWithLock(da.getItemCode()).stream().findFirst().orElseThrow();
                stock.setQuantityAvailable(stock.getQuantityAvailable() - da.getQuantite());
                stockItemRepository.save(stock);

                da.setIsAvailableInStock(true);
                da.setStatut(StatutDemande.DISPONIBLE_STOCK);
                log(da, statutPrecedent, StatutDemande.DISPONIBLE_STOCK, user,
                    "Pièce disponible en stock — sortie automatique de " + da.getQuantite() + " unités");
            } else {
                da.setIsAvailableInStock(false);
                da.setStatut(StatutDemande.EN_TRAITEMENT);
                log(da, statutPrecedent, StatutDemande.EN_TRAITEMENT, user,
                    "Pièce hors stock — en attente de valorisation par l'acheteur");
            }
        } else {
            // ── Flux A : achat général (Circuit hiérarchique N1) ──────────
            da.setStatut(StatutDemande.SOUMISE);
            log(da, statutPrecedent, StatutDemande.SOUMISE, user, "Demande soumise — en attente de validation N1");
        }

        return repository.save(da);
    }

    // ── Circuit de validation hiérarchique ───────────────────────────────────

    @Transactional
    public DemandeAchatInterne validerN1(Long id, boolean valider, String commentaire, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        StatutDemande from = da.getStatut();
        if (from != StatutDemande.SOUMISE) {
            throw new IllegalStateException("Validation N1 impossible depuis : " + from);
        }
        if (user.getRole() != Role.MANAGER_N1) {
            throw new SecurityException("Seul un Manager N1 peut effectuer cette validation.");
        }
        StatutDemande to = valider ? StatutDemande.VALIDE_N1 : StatutDemande.REJETEE;
        da.setStatut(to);
        log(da, from, to, user, commentaire);
        return repository.save(da);
    }

    @Transactional
    public DemandeAchatInterne validerTechnicien(Long id, boolean valider, String commentaire, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        StatutDemande from = da.getStatut();
        if (from != StatutDemande.VALIDE_N1)
            throw new IllegalStateException("Attendu: VALIDE_N1");
        if (user.getRole() != Role.TECHNICIEN)
            throw new SecurityException("Habilitation TECHNICIEN requise.");
        // Règle BAG : Après validation technique, la DA doit être valorisée par l'acheteur
        // avant d'entrer dans le circuit budgétaire (AMG/DAF/DG)
        StatutDemande to = valider ? StatutDemande.EN_TRAITEMENT : StatutDemande.REJETEE;
        da.setStatut(to);
        log(da, from, to, user, commentaire);
        return repository.save(da);
    }

    @Transactional
    public DemandeAchatInterne validerAMG(Long id, boolean valider, String commentaire, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        StatutDemande from = da.getStatut();
        if (from != StatutDemande.VALIDE_TECH)
            throw new IllegalStateException("Attendu: VALIDE_TECH");
        if (user.getRole() != Role.AMG)
            throw new SecurityException("Habilitation AMG requise.");
        StatutDemande to = valider ? StatutDemande.VALIDE_AMG : StatutDemande.REJETEE;
        da.setStatut(to);
        log(da, from, to, user, commentaire);
        return repository.save(da);
    }

    @Transactional
    public DemandeAchatInterne validerDAF(Long id, boolean valider, String commentaire, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        StatutDemande from = da.getStatut();
        if (from != StatutDemande.VALIDE_AMG)
            throw new IllegalStateException("Attendu: VALIDE_AMG");
        if (user.getRole() != Role.DAF)
            throw new SecurityException("Habilitation DAF requise.");
        StatutDemande to = valider ? StatutDemande.VALIDE_DAF : StatutDemande.REJETEE;
        da.setStatut(to);
        log(da, from, to, user, commentaire);
        return repository.save(da);
    }

    @Transactional
    public DemandeAchatInterne validerDG(Long id, boolean valider, String commentaire, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        StatutDemande from = da.getStatut();
        if (from != StatutDemande.VALIDE_DAF)
            throw new IllegalStateException("Attendu: VALIDE_DAF");
        if (user.getRole() != Role.DG)
            throw new SecurityException("Habilitation DG requise.");
        StatutDemande to = valider ? StatutDemande.VALIDE_DG : StatutDemande.REJETEE;
        da.setStatut(to);
        log(da, from, to, user, commentaire);
        return repository.save(da);
    }

    // ── Valorisation & PO ────────────────────────────────────────────────────

    @Transactional
    public DemandeAchatInterne valoriserDemande(Long id, java.math.BigDecimal prix, Integer supplierId) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        if (da.getStatut() != StatutDemande.EN_TRAITEMENT)
            throw new IllegalStateException("Attendu: EN_TRAITEMENT");
        da.setPrixUnitaire(prix);
        da.setMontantEstime(prix.multiply(java.math.BigDecimal.valueOf(da.getQuantite())));
        da.setFournisseur(new Supplier(supplierId));
        return repository.save(da);
    }

    /**
     * Action Acheteur : confirme que la demande est traitée et prête pour le
     * circuit N1.
     * Cette étape est nécessaire pour les pièces SAV après valorisation.
     */
    @Transactional
    public DemandeAchatInterne traiterAchat(Long id, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        if (da.getStatut() != StatutDemande.EN_TRAITEMENT)
            throw new IllegalStateException("Attendu: EN_TRAITEMENT");
        if (da.getPrixUnitaire() == null || da.getFournisseur() == null)
            throw new IllegalStateException("La demande doit être valorisée d'abord.");
        if (user.getRole() != Role.ACHETEUR)
            throw new SecurityException("Seul l'ACHETEUR peut traiter cette demande.");

        // Une fois valorisée par l'acheteur, la DA passe directement au circuit AMG (Budget)
        // Pour les pièces SAV, cela permet de bypasser N1 et Tech (exigence métier).
        // Pour les achats internes, cela suit la valorisation post-technique.
        da.setStatut(StatutDemande.VALIDE_TECH);
        log(da, StatutDemande.EN_TRAITEMENT, StatutDemande.VALIDE_TECH, user,
                "Achat valorisé — Transmis au circuit AMG pour contrôle budgétaire");
        return repository.save(da);
    }

    @Transactional
    public PurchaseOrder creerPO(Long id, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        if (da.getStatut() != StatutDemande.VALIDE_DG)
            throw new IllegalStateException("Attendu: VALIDE_DG");
        return poService.generateFromInternal(da);
    }

    public List<DemandeAchatInterne> getMesDemandes(User user) {
        return repository.findByDemandeur(user);
    }

    public List<DemandeAchatInterne> getDemandesAValider(User user) {
        return switch (user.getRole()) {
            case MANAGER_N1 -> repository.findByStatutIn(List.of(StatutDemande.SOUMISE)); // Ne voit plus les
                                                                                          // EN_TRAITEMENT
            case TECHNICIEN -> repository.findByStatut(StatutDemande.VALIDE_N1);
            case AMG -> repository.findByStatut(StatutDemande.VALIDE_TECH);
            case DAF -> repository.findByStatut(StatutDemande.VALIDE_AMG);
            case DG -> repository.findByStatut(StatutDemande.VALIDE_DAF);
            case ACHETEUR -> repository.findByStatutIn(List.of(StatutDemande.EN_TRAITEMENT, StatutDemande.VALIDE_DG));
            default -> List.of();
        };
    }

    @Transactional
    public DemandeAchatInterne ajustementBudget(Long id, TypeAjustement type, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        log(da, da.getStatut(), da.getStatut(), user, "Ajustement : " + type);
        return repository.save(da);
    }

    @Transactional
    public DemandeAchatInterne annulerDemande(Long id, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        if (da.getStatut() == StatutDemande.PO_CREE || da.getStatut() == StatutDemande.VALIDE_DG) {
            throw new IllegalStateException("Impossible d'annuler une demande déjà validée par la DG ou avec PO.");
        }
        
        StatutDemande from = da.getStatut();
        
        // Restauration du stock si la pièce était réservée
        if (from == StatutDemande.DISPONIBLE_STOCK && Boolean.TRUE.equals(da.getIsAvailableInStock())) {
            StockItem stock = stockItemRepository.findByItemCodeWithLock(da.getItemCode())
                    .stream().findFirst().orElseThrow();
            stock.setQuantityAvailable(stock.getQuantityAvailable() + da.getQuantite());
            stockItemRepository.save(stock);
        }
        
        da.setStatut(StatutDemande.REJETEE);
        log(da, from, StatutDemande.REJETEE, user, "Demande annulée par l'utilisateur/système — Stock restauré si applicable");
        return repository.save(da);
    }

    private void log(DemandeAchatInterne da, StatutDemande from, StatutDemande to, User user, String msg) {
        historyRepository.save(new StatusHistory("DemandeAchatInterne", da.getId(), from != null ? from.name() : null,
                to.name(), user, msg));
    }
}
