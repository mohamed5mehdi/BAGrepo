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
 * BROUILLON → EN_TRAITEMENT → [valoriserDemande Acheteur] → [traiterAchat] → APPROUVEE → creerPO() → PENDING_APPROVAL → approvePO(RESP_ACHAT) → GRN → GRC POSTED
 */
@Service
public class DemandeAchatInterneService {

    @Autowired
    private DemandeAchatInterneRepository repository;
    @Autowired
    private StockItemRepository stockItemRepository;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private PurchaseOrderService poService;
    @Autowired
    private StatusHistoryRepository historyRepository;
    @Autowired
    private FamilyRepository familyRepository;
    @Autowired
    private SubFamilyRepository subFamilyRepository;
    @Autowired
    private BudgetPiecesService budgetPiecesService;
    @Autowired
    private OffreFournisseurRepository offreRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private NotificationService notificationService;

    public List<OffreFournisseur> getOffresByDemande(Long demandeId) {
        return offreRepository.findByDa_Id(demandeId);
    }

    public List<OffreFournisseur> getAllOffres() {
        return offreRepository.findAll();
    }

    @Transactional
    public OffreFournisseur addOffre(Long demandeId, Integer fournisseurId, java.math.BigDecimal prixPropose, String conditions, Integer delai) {
        if (prixPropose == null || prixPropose.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix proposé doit être strictement positif pour constituer un devis valide.");
        }
        DemandeAchatInterne da = repository.findById(demandeId).orElseThrow();
        Supplier supplier = supplierRepository.findById(fournisseurId).orElseThrow();
        OffreFournisseur offre = new OffreFournisseur(da, supplier, prixPropose, conditions, delai);
        return offreRepository.save(offre);
    }

    // ── Création ─────────────────────────────────────────────────────────────

    @Transactional
    public DemandeAchatInterne createDemande(DemandeAchatInterne da, User user) {
        // Idempotency check
        if (da.getSubmissionToken() != null && !da.getSubmissionToken().isEmpty()) {
            Optional<DemandeAchatInterne> existing = repository.findBySubmissionToken(da.getSubmissionToken());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // Anti-flood check
        Optional<DemandeAchatInterne> recentOpt = repository.findFirstByDemandeurAndDesignationAndQuantiteOrderByDateCreationDesc(user, da.getDesignation(), da.getQuantite());
        if (recentOpt.isPresent() && recentOpt.get().getDateCreation() != null &&
            recentOpt.get().getDateCreation().isAfter(LocalDateTime.now().minusMinutes(5))) {
            throw new RuntimeException("Une demande identique a été soumise récemment.");
        }

        da.setDemandeur(user);
        da.setStatut(StatutDemande.BROUILLON);
        da.setDateCreation(LocalDateTime.now());
        
        // Nettoyage des IDs de la DA elle-même pour éviter tout conflit d'update
        da.setId(null);

        // Rechargement des entités budgétaires (Header)
        // IMPORTANT : forcer le rechargement complet depuis la DB pour garantir
        // que Family.getCategorie() est disponible (pas un proxy Hibernate vide).
        if (da.getBudgetSousFamille() != null) {
            Integer oidSub = da.getBudgetSousFamille().getOidSub();
            if (oidSub != null) {
                SubFamily sf = subFamilyRepository.findById(oidSub).orElse(null);
                da.setBudgetSousFamille(sf);
                if (sf != null && sf.getFamily() != null) {
                    // Rechargement complet pour éviter le proxy Hibernate partiel
                    Family fam = familyRepository.findById(sf.getFamily().getIdFamily()).orElse(sf.getFamily());
                    da.setBudgetFamille(fam);
                } else if (da.getBudgetFamille() != null) {
                    Integer idFamily = da.getBudgetFamille().getIdFamily();
                    if (idFamily != null) {
                        da.setBudgetFamille(familyRepository.findById(idFamily).orElse(null));
                    }
                }
            }
        } else if (da.getBudgetFamille() != null) {
            Integer idFamily = da.getBudgetFamille().getIdFamily();
            if (idFamily != null) {
                da.setBudgetFamille(familyRepository.findById(idFamily).orElse(null));
            }
        }

        if (da.getCategorie() == null && da.getBudgetFamille() != null) {
            Family fam = da.getBudgetFamille();
            if (fam.getCategorie() == null && fam.getIdFamily() != null) {
                 fam = familyRepository.findById(fam.getIdFamily()).orElse(fam);
            }
            da.setCategorie(fam.getCategorie());
        }

        DemandeAchatInterne saved = repository.save(da);
        log(saved, null, StatutDemande.BROUILLON, user, "Demande créée avec succès");
        return saved;
    }

    @Transactional
    public DemandeAchatInterne updateDemande(Long id, DemandeAchatInterne payload, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        if (da.getStatut() != StatutDemande.BROUILLON && da.getStatut() != StatutDemande.EN_TRAITEMENT) {
            throw new IllegalStateException("Modification impossible dans le statut actuel : " + da.getStatut());
        }

        // Phase 2 : Gestion de l'orphanRemoval par JPA (SANS restitution budgétaire)
        // Analyse Forensique : Le budget N'EST PAS engagé lors de la création d'une DemandeAchatInterne.
        // L'engagement se fait exclusivement dans PurchaseOrderService.generateFromInternal.
        // Restituer le budget ici provoquerait une "création monétaire" fictive.

        da.setDesignation(payload.getDesignation());
        da.setQuantite(payload.getQuantite());
        da.setJustification(payload.getJustification());
        // da.setMontantEstime(payload.getMontantEstime()); // Retire pour protéger le pré-engagement
        
        log(da, da.getStatut(), da.getStatut(), user, "Mise à jour de la demande (sans impact budgétaire)");
        return repository.save(da);
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
            String effectiveItemCode = da.getItemCode();


            if (effectiveItemCode == null || effectiveItemCode.isBlank()) {
                throw new IllegalArgumentException("Code article obligatoire pour une demande de pièce de rechange.");
            }

            Integer effectiveQuantity = da.getQuantite();
            if (effectiveQuantity == null) effectiveQuantity = 1;

            Warehouse centralWarehouse = warehouseService.getDefaultWarehouse();
            Optional<StockItem> lockedStockOpt = stockItemRepository.findByItemCodeAndWarehouseIdWithLock(effectiveItemCode, centralWarehouse.getId());

            boolean disponible = lockedStockOpt.isPresent() && lockedStockOpt.get().getQuantityAvailable() >= effectiveQuantity;

            if (disponible) {
                // Sortie de stock déléguée à WarehouseService : verrou anticipé sur l'entrepôt central
                // + génération automatique du StockMovement (traçabilité restaurée).
                warehouseService.removeStock(effectiveItemCode, effectiveQuantity, "SAV-DA-" + da.getId());

                // IMPUTATION BUDGET PIECES (Fix SAV Fuite)
                StockItem stock = lockedStockOpt.get();
                java.math.BigDecimal totalCost = stock.getUnitCost() != null ? stock.getUnitCost().multiply(java.math.BigDecimal.valueOf(effectiveQuantity)) : java.math.BigDecimal.ZERO;
                if (totalCost.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    budgetPiecesService.consommerBudgetPieces(totalCost, da.getId(), String.valueOf(da.getDateCreation().getYear()));
                }

                da.setIsAvailableInStock(true);
                da.setStatut(StatutDemande.DISPONIBLE_STOCK);
                log(da, statutPrecedent, StatutDemande.DISPONIBLE_STOCK, user,
                    "Pièce [" + effectiveItemCode + "] disponible en stock — sortie automatique de " + effectiveQuantity + " unités");
            } else {
                da.setIsAvailableInStock(false);
                da.setStatut(StatutDemande.EN_TRAITEMENT);
                log(da, statutPrecedent, StatutDemande.EN_TRAITEMENT, user,
                    "Pièce hors stock — en attente de valorisation par l'acheteur");
                if (notificationService != null) {
                    notificationService.notifyTopic("acheteurs", "Nouvelle pièce SAV hors stock en attente de valorisation: " + effectiveItemCode);
                }
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
        StatutDemande to = valider ? StatutDemande.VALIDE_N1 : StatutDemande.BROUILLON;
        if (!valider) restituerBudgetInterne(da);
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
        StatutDemande to = valider ? StatutDemande.VALIDE_TECH : StatutDemande.BROUILLON;
        if (!valider) restituerBudgetInterne(da);
        da.setStatut(to);
        log(da, from, to, user, commentaire);
        return repository.save(da);
    }

    @Transactional
    public DemandeAchatInterne validerAMG(Long id, boolean valider, String commentaire, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        StatutDemande from = da.getStatut();
        if (from != StatutDemande.VALIDE_ACHETEUR)
            throw new IllegalStateException("Attendu: VALIDE_ACHETEUR");
        if (user.getRole() != Role.AMG)
            throw new SecurityException("Habilitation AMG requise.");
        StatutDemande to = valider ? StatutDemande.VALIDE_AMG : StatutDemande.BROUILLON;
        if (!valider) restituerBudgetInterne(da);
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
        StatutDemande to = valider ? StatutDemande.VALIDE_DAF : StatutDemande.BROUILLON;
        if (!valider) restituerBudgetInterne(da);
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
        StatutDemande to = valider ? StatutDemande.VALIDE_DG : StatutDemande.BROUILLON;
        if (!valider) restituerBudgetInterne(da);
        da.setStatut(to);
        log(da, from, to, user, commentaire);
        return repository.save(da);
    }

    // ── Valorisation & PO ────────────────────────────────────────────────────

    @Transactional
    public DemandeAchatInterne valoriserDemande(Long id, java.math.BigDecimal prix, Integer supplierId) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        if (da.getStatut() != StatutDemande.VALIDE_TECH && da.getStatut() != StatutDemande.EN_TRAITEMENT)
            throw new IllegalStateException("Attendu: VALIDE_TECH ou EN_TRAITEMENT");

        da.setPrixUnitaire(prix);
        java.math.BigDecimal newTotal = prix.multiply(java.math.BigDecimal.valueOf(da.getQuantite() != null ? da.getQuantite() : 1));

        if (Boolean.TRUE.equals(da.getIsPieceRechange())) {
            // ── Flux B Pièces : consommation sur le pool dédié, JAMAIS sur une SF ──
            String exercice = String.valueOf(da.getDateCreation().getYear());
            // Restitution de l'ancienne estimation si re-valorisation
            if (da.getMontantEstime() != null) {
                budgetPiecesService.restituterBudgetPieces(da.getMontantEstime(), da.getId(), exercice);
            }
            budgetPiecesService.consommerBudgetPieces(newTotal, da.getId(), exercice);
        } else {
            // ── Flux A Général : consommation sur la sous-famille budgétaire ──
            if (da.getBudgetSousFamille() == null) {
                throw new IllegalStateException(
                    "La demande doit être affectée à une sous-famille budgétaire avant valorisation.");
            }
            SubFamily sf = subFamilyRepository.findByIdWithLock(da.getBudgetSousFamille().getOidSub()).orElseThrow();
            // Restitution de l'ancienne estimation si re-valorisation
            if (da.getMontantEstime() != null) sf.addBudget(da.getMontantEstime());
            if (!sf.hasEnoughBudget(newTotal)) {
                throw new IllegalStateException("Budget insuffisant dans la sous-famille pour valoriser cette demande.");
            }
            sf.deductBudget(newTotal);
            subFamilyRepository.save(sf);
            if (sf.getFamily() != null) {
                Family fam = familyRepository.findByIdWithLock(sf.getFamily().getIdFamily()).orElseThrow();
                if (da.getMontantEstime() != null) fam.addBudget(da.getMontantEstime());
                fam.deductBudget(newTotal);
                familyRepository.save(fam);
            }
        }

        da.setMontantEstime(newTotal);
        da.setFournisseur(supplierRepository.findById(supplierId).orElseThrow(() -> new RuntimeException("Fournisseur introuvable")));

        if (Boolean.TRUE.equals(da.getIsPieceRechange())) {
            StatutDemande oldStatut = da.getStatut();
            da.setStatut(StatutDemande.APPROUVEE);
            log(da, oldStatut, StatutDemande.APPROUVEE, null, "Auto-confirmation post-valorisation SAV");
        }

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
        if (da.getStatut() != StatutDemande.VALIDE_TECH && da.getStatut() != StatutDemande.EN_TRAITEMENT)
            throw new IllegalStateException("Attendu: VALIDE_TECH ou EN_TRAITEMENT");
        if (da.getPrixUnitaire() == null || da.getFournisseur() == null)
            throw new IllegalStateException("La demande doit être valorisée d'abord.");
        
        if (!user.getRole().isAcheteur()) {
            throw new SecurityException("Seul un ACHETEUR peut traiter cette demande.");
        }
        
        CategorieDemande daCategorie = da.getCategorie();
        if (daCategorie == null && da.getBudgetFamille() != null && da.getBudgetFamille().getIdFamily() != null) {
            Family fam = familyRepository.findById(da.getBudgetFamille().getIdFamily()).orElse(null);
            if (fam != null) {
                daCategorie = fam.getCategorie();
            }
        }
        
        if (!Boolean.TRUE.equals(da.getIsPieceRechange())) {
            if (user.getRole() != Role.ACHETEUR && user.getRole().getCategorieAssignee() != daCategorie) {
                throw new SecurityException("Habilitation insuffisante : Vous ne pouvez traiter que les demandes de la catégorie " + user.getRole().getCategorieAssignee());
            }
        }

        if (Boolean.TRUE.equals(da.getIsPieceRechange())) {
            da.setStatut(StatutDemande.APPROUVEE);
        } else {
            da.setStatut(StatutDemande.VALIDE_ACHETEUR);
        }
        return repository.save(da);
    }

    @Transactional
    public PurchaseOrder creerPO(Long id, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        
        // Autoriser la création de PO si :
        // 1. Flux Standard : Validé par DG
        // 2. Flux SAV : Valorisé par l'Acheteur (Statut APPROUVEE via bypass)
        if (da.getStatut() != StatutDemande.VALIDE_DG && da.getStatut() != StatutDemande.APPROUVEE) {
            throw new IllegalStateException("Statut invalide pour génération PO : " + da.getStatut());
        }

        // Sécurité contre la double génération (Bug Silencieux)
        if (da.getPurchaseOrder() != null) {
            throw new IllegalStateException("Un Bon de Commande existe déjà pour cette demande (PO #" + da.getPurchaseOrder().getPoNumber() + ")");
        }
        
        PurchaseOrder po = poService.generateFromInternal(da);
        
        // Une fois le PO généré, on marque la DA comme traitée (PO_CREE → visible Demandeur)
        da.setStatut(StatutDemande.PO_CREE);
        repository.save(da);
        
        return po;
    }

    public List<DemandeAchatInterne> getMesDemandes(User user) {
        return repository.findByDemandeur(user);
    }

    public List<DemandeAchatInterne> getDemandesAValider(User user) {
        if (user.getRole().isAcheteur()) {
            List<StatutDemande> statutsAcheteur = List.of(StatutDemande.VALIDE_TECH, StatutDemande.EN_TRAITEMENT, StatutDemande.VALIDE_DG, StatutDemande.APPROUVEE);
            if (user.getRole() == Role.ACHETEUR) {
                return repository.findByStatutIn(statutsAcheteur);
            } else {
                return repository.findByStatutInAndCategorie(statutsAcheteur, user.getRole().getCategorieAssignee());
            }
        }
        if (user.getRole() == Role.ADMINISTRATEUR) {
            return repository.findAll();
        }
        return switch (user.getRole()) {
            case MANAGER_N1 -> repository.findByStatutAndFluxAAndManagerId(
                StatutDemande.SOUMISE, user.getOidUser()
            );
            case TECHNICIEN -> repository.findByStatut(StatutDemande.VALIDE_N1);
            case AMG -> repository.findByStatutAndIsPieceRechange(StatutDemande.VALIDE_ACHETEUR, false);
            // DAF voit : les DA en attente de validation normale + les DA en attente d'ajustement sous-famille
            case DAF -> repository.findByStatutIn(List.of(
                StatutDemande.VALIDE_AMG,
                StatutDemande.EN_ATTENTE_AJUSTEMENT_DAF
            ));
            // DG voit : les DA en attente de validation finale + les DA en attente d'ajustement famille
            case DG -> repository.findByStatutIn(List.of(
                StatutDemande.VALIDE_DAF,
                StatutDemande.EN_ATTENTE_AJUSTEMENT_DG
            ));
            case RESP_ACHAT -> List.of(); // Le Resp. Achat valide uniquement les POs (Schema 2)
            default -> List.of();
        };
    }

    @Autowired
    private com.pfe.gestionsachat.repository.DemandeAjustementRepository demandeAjustementRepository;

    @Transactional
    public DemandeAchatInterne ajustementBudget(Long id, TypeAjustement type,
            java.math.BigDecimal montantDemande, Integer sourceSousFamilleId,
            Integer cibleSousFamilleId, Integer familleCibleId,
            String justification, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        StatutDemande from = da.getStatut();

        // Guard anti-doublon : requête JPQL ciblée — O(1) vs O(N) table scan
        boolean ajustementActif = demandeAjustementRepository.existsByDemandeInterneIdAndStatutIn(
                id,
                java.util.List.of(
                    com.pfe.gestionsachat.model.StatutAjustement.EN_ATTENTE_DG,
                    com.pfe.gestionsachat.model.StatutAjustement.EN_ATTENTE_DAF,
                    com.pfe.gestionsachat.model.StatutAjustement.EN_ATTENTE_ACHETEUR
                ));
        if (ajustementActif) {
            throw new IllegalStateException(
                "Un ajustement budgétaire est déjà en cours pour cette demande (id=" + id + ").");
        }

        da.setTypeAjustement(type);

        com.pfe.gestionsachat.model.DemandeAjustement daAjust = new com.pfe.gestionsachat.model.DemandeAjustement();
        daAjust.setDemandeInterne(da);
        daAjust.setType(type);
        daAjust.setMontantDemande(montantDemande != null ? montantDemande : java.math.BigDecimal.ZERO);
        daAjust.setAcheteurId((long) user.getOidUser());
        daAjust.setJustificationAcheteur(justification != null ? justification : "Ajustement requis");
        daAjust.setStatutDemandeInterneAvantAjustement(from);

        if (type == TypeAjustement.FAMILLE) {
            da.setStatut(StatutDemande.EN_ATTENTE_AJUSTEMENT_DG);
            daAjust.setStatut(com.pfe.gestionsachat.model.StatutAjustement.EN_ATTENTE_DG);
            daAjust.setFamilleCibleId(familleCibleId);
        } else {
            da.setStatut(StatutDemande.EN_ATTENTE_AJUSTEMENT_DAF);
            daAjust.setStatut(com.pfe.gestionsachat.model.StatutAjustement.EN_ATTENTE_DAF);
            daAjust.setSourceSousFamilleId(sourceSousFamilleId);
            daAjust.setCibleSousFamilleId(cibleSousFamilleId);
            // La pré-réservation budgétaire (deductBudget source) est exclusivement
            // portée par DemandeAjustementService.soumettreAjustementSousFamille().
            // Toute déduction ici constituerait un double-dip silencieux.
        }

        demandeAjustementRepository.save(daAjust);
        log(da, from, da.getStatut(), user, "Ajustement : " + type);
        return repository.save(da);
    }

    @Transactional
    public DemandeAchatInterne annulerDemande(Long id, User user) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        if (da.getStatut() == StatutDemande.PO_CREE || da.getStatut() == StatutDemande.VALIDE_DG || da.getStatut() == StatutDemande.APPROUVEE) {
            throw new IllegalStateException("Impossible d'annuler une demande déjà validée par la DG, approuvée ou avec PO.");
        }
        
        StatutDemande from = da.getStatut();
        
        // Restauration du stock si la pièce était réservée
        if (from == StatutDemande.DISPONIBLE_STOCK && Boolean.TRUE.equals(da.getIsAvailableInStock())) {
            // Délégation à WarehouseService : cible l'entrepôt central et génère
            // le StockMovement réciproque (IN_RECEIPT) pour la traçabilité.
            warehouseService.addStock(da.getItemCode(), da.getQuantite(), "ANNUL-SAV-DA-" + da.getId());
        }
        
        da.setStatut(StatutDemande.REJETEE);
        restituerBudgetInterne(da);
        log(da, from, StatutDemande.REJETEE, user, "Demande annulée par l'utilisateur/système — Stock restauré si applicable");
        return repository.save(da);
    }

    @Transactional
    public void rejeterDaSuiteAuPO(Long id, User responsable, String motif) {
        DemandeAchatInterne da = repository.findById(id).orElseThrow();
        StatutDemande from = da.getStatut();
        da.setStatut(StatutDemande.REJETEE);
        restituerBudgetInterne(da);
        
        String msg = "Demande rejetée automatiquement suite au rejet du Bon de Commande.";
        if (motif != null && !motif.isBlank()) {
            msg += " Motif : " + motif;
        }
        
        log(da, from, StatutDemande.REJETEE, responsable, msg);
        repository.save(da);
    }

    private void log(DemandeAchatInterne da, StatutDemande from, StatutDemande to, User user, String msg) {
        historyRepository.save(new StatusHistory("DemandeAchatInterne", da.getId(), from != null ? from.name() : null,
                to.name(), user, msg));
    }

    private void restituerBudgetInterne(DemandeAchatInterne da) {
        if (da.getMontantEstime() == null) return; // Rien à restituer

        if (Boolean.TRUE.equals(da.getIsPieceRechange())) {
            // ── Flux B Pièces : restitution sur le pool dédié ──────────────
            String exercice = String.valueOf(da.getDateCreation().getYear());
            budgetPiecesService.restituterBudgetPieces(da.getMontantEstime(), da.getId(), exercice);
        } else if (da.getBudgetSousFamille() != null) {
            // ── Flux A Général : restitution sur la sous-famille ───────────
            SubFamily sf = subFamilyRepository.findByIdWithLock(da.getBudgetSousFamille().getOidSub()).orElseThrow();
            sf.addBudget(da.getMontantEstime());
            subFamilyRepository.save(sf);
            if (sf.getFamily() != null) {
                Family fam = familyRepository.findByIdWithLock(sf.getFamily().getIdFamily()).orElseThrow();
                fam.addBudget(da.getMontantEstime());
                familyRepository.save(fam);
            }
        }
        da.setMontantEstime(null); // Eviter la double restitution
    }
}
