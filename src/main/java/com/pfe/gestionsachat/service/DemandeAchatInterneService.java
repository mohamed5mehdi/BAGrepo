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
    @Autowired
    private FamilyRepository familyRepository;
    @Autowired
    private SubFamilyRepository subFamilyRepository;
    @Autowired
    private BudgetPiecesService budgetPiecesService;

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
        if (da.getBudgetSousFamille() != null) {
            Integer oidSub = da.getBudgetSousFamille().getOidSub();
            if (oidSub != null) {
                SubFamily sf = subFamilyRepository.findById(oidSub).orElse(null);
                da.setBudgetSousFamille(sf);
                if (sf != null && sf.getFamily() != null) {
                    da.setBudgetFamille(sf.getFamily());
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

        // Préparation du détail SAV si nécessaire
        if (Boolean.TRUE.equals(da.getIsPieceRechange()) && da.getItemCode() != null) {
            DaDetails detail = new DaDetails();
            detail.setItemCode(da.getItemCode());
            detail.setItemName(da.getDesignation());
            detail.setQuantite(da.getQuantite() != null ? da.getQuantite() : 1);
            detail.setJustification(da.getJustification());
            detail.setSubFamily(da.getBudgetSousFamille()); 
            if (da.getDetails() == null) da.setDetails(new java.util.ArrayList<>());
            da.getDetails().add(detail);
        }

        // Nettoyage et Liaison de TOUS les détails
        if (da.getDetails() != null) {
            da.getDetails().forEach(detail -> {
                detail.setOidDetail(null); // IMPORTANT : Force Hibernate à traiter comme "New"
                detail.setDemandeAchatInterne(da);
                
                // Sécurisation de la sous-famille par item
                if (detail.getSubFamily() != null) {
                    Integer oidSub = detail.getSubFamily().getOidSub();
                    if (oidSub != null) {
                        detail.setSubFamily(subFamilyRepository.findById(oidSub).orElse(da.getBudgetSousFamille()));
                    } else {
                        detail.setSubFamily(da.getBudgetSousFamille());
                    }
                } else {
                    detail.setSubFamily(da.getBudgetSousFamille());
                }
            });
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
        if (da.getDetails() != null && payload.getDetails() != null) {
            // Remplacement de la collection pour laisser JPA gérer l'orphanRemoval
            da.getDetails().clear();
            da.getDetails().addAll(payload.getDetails());
            da.getDetails().forEach(d -> d.setDemandeAchatInterne(da));
        }

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
            
            // Fallback : si le code n'est pas à la racine, on regarde dans le premier détail
            if ((effectiveItemCode == null || effectiveItemCode.isBlank()) && da.getDetails() != null && !da.getDetails().isEmpty()) {
                effectiveItemCode = da.getDetails().get(0).getItemCode();
            }

            if (effectiveItemCode == null || effectiveItemCode.isBlank()) {
                throw new IllegalArgumentException("Code article obligatoire pour une demande de pièce de rechange.");
            }

            Optional<StockItem> stockOpt = stockItemRepository.findByItemCode(effectiveItemCode).stream().findFirst();

            Integer effectiveQuantity = da.getQuantite();
            if (effectiveQuantity == null && da.getDetails() != null && !da.getDetails().isEmpty()) {
                effectiveQuantity = da.getDetails().get(0).getQuantite();
            }
            if (effectiveQuantity == null) effectiveQuantity = 1;

            boolean disponible = stockOpt.isPresent() && stockOpt.get().getQuantityAvailable() >= effectiveQuantity;

            if (disponible) {
                // Sortie de stock automatique avec verrou pessimiste (Fix PhD : utiliser effectiveItemCode)
                StockItem stock = stockItemRepository.findByItemCodeWithLock(effectiveItemCode).stream().findFirst().orElseThrow();
                stock.setQuantityAvailable(stock.getQuantityAvailable() - effectiveQuantity);
                stockItemRepository.save(stock);

                da.setIsAvailableInStock(true);
                da.setStatut(StatutDemande.DISPONIBLE_STOCK);
                log(da, statutPrecedent, StatutDemande.DISPONIBLE_STOCK, user,
                    "Pièce [" + effectiveItemCode + "] disponible en stock — sortie automatique de " + effectiveQuantity + " unités");
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
        StatutDemande to = valider ? StatutDemande.EN_TRAITEMENT : StatutDemande.REJETEE;
        if (!valider) restituerBudgetInterne(da);
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
        StatutDemande to = valider ? StatutDemande.VALIDE_DAF : StatutDemande.REJETEE;
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
        StatutDemande to = valider ? StatutDemande.VALIDE_DG : StatutDemande.REJETEE;
        if (!valider) restituerBudgetInterne(da);
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
        java.math.BigDecimal newTotal = prix.multiply(java.math.BigDecimal.valueOf(da.getQuantite() != null ? da.getQuantite() : 1));

        if (Boolean.TRUE.equals(da.getIsPieceRechange())) {
            // ── Flux B Pièces : consommation sur le pool dédié, JAMAIS sur une SF ──
            // Restitution de l'ancienne estimation si re-valorisation
            if (da.getMontantEstime() != null) {
                budgetPiecesService.restituterBudgetPieces(da.getMontantEstime(), da.getId());
            }
            budgetPiecesService.consommerBudgetPieces(newTotal, da.getId());
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
        da.setFournisseur(new Supplier(supplierId));

        // Synchroniser le prix dans les détails pour le PO
        if (da.getDetails() != null && !da.getDetails().isEmpty()) {
            da.getDetails().forEach(d -> d.setPrixUnitaire(prix));
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
        if (da.getStatut() != StatutDemande.EN_TRAITEMENT)
            throw new IllegalStateException("Attendu: EN_TRAITEMENT");
        if (da.getPrixUnitaire() == null || da.getFournisseur() == null)
            throw new IllegalStateException("La demande doit être valorisée d'abord.");
        if (user.getRole() != Role.ACHETEUR)
            throw new SecurityException("Seul l'ACHETEUR peut traiter cette demande.");

        if (Boolean.TRUE.equals(da.getIsPieceRechange())) {
            da.setStatut(StatutDemande.VALIDE_DG);
        } else {
            da.setStatut(StatutDemande.VALIDE_TECH);
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
        return switch (user.getRole()) {
            case MANAGER_N1 -> repository.findByStatutAndFluxAAndManagerId(
                StatutDemande.SOUMISE, user.getOidUser()
            );
            case TECHNICIEN -> repository.findByStatut(StatutDemande.VALIDE_N1);
            case AMG -> repository.findByStatutAndIsPieceRechange(StatutDemande.VALIDE_TECH, false);
            case DAF -> repository.findByStatutAndIsPieceRechange(StatutDemande.VALIDE_AMG, false);
            case DG -> repository.findByStatutAndIsPieceRechange(StatutDemande.VALIDE_DAF, false);
            case ACHETEUR -> repository.findByStatutIn(List.of(
                StatutDemande.EN_TRAITEMENT,
                StatutDemande.VALIDE_DG
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
            
            if (sourceSousFamilleId != null && montantDemande != null && montantDemande.compareTo(java.math.BigDecimal.ZERO) > 0) {
                com.pfe.gestionsachat.model.SubFamily source = subFamilyRepository.findByIdWithLock(sourceSousFamilleId).orElseThrow();
                if (!source.hasEnoughBudget(montantDemande)) {
                    throw new RuntimeException("Budget insuffisant dans la sous-famille source pour l'ajustement demandé.");
                }
                // FIX P4 — deductBudget() décrémente budgetRestant ET incrémente budgetEngage atomiquement.
                // L'ancienne implémentation n'incrémentait que budgetEngage sans toucher budgetRestant,
                // laissant le budget disponible faussement élevé (fuite silencieuse).
                java.math.BigDecimal budgetAvant = source.getBudgetRestant();
                source.deductBudget(montantDemande);
                subFamilyRepository.save(source);
                // Mise à jour de la famille parente pour maintenir la cohérence hiérarchique
                if (source.getFamily() != null) {
                    com.pfe.gestionsachat.model.Family fam = familyRepository.findByIdWithLock(source.getFamily().getIdFamily()).orElseThrow();
                    fam.deductBudget(montantDemande);
                    familyRepository.save(fam);
                }
                daAjust.setBudgetAvantDemande("Source [" + source.getLibelle() + "]: " + budgetAvant);
                daAjust.setBudgetApresDemande("Source [" + source.getLibelle() + "]: " + source.getBudgetRestant());
            }
        }
        
        demandeAjustementRepository.save(daAjust);
        log(da, from, da.getStatut(), user, "Ajustement : " + type);
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
            budgetPiecesService.restituterBudgetPieces(da.getMontantEstime(), da.getId());
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
