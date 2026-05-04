package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DemandeAchatInterneService {

    @Autowired
    private DemandeAchatInterneRepository demandeRepository;

    @Autowired
    private StatusHistoryRepository historyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private WarehouseService warehouseService;



    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private SubFamilyRepository subFamilyRepository;

    @Transactional
    public DemandeAchatInterne createDemande(DemandeAchatInterne demande, User demandeur) {
        demande.setDemandeur(demandeur);
        demande.setDepartement(demandeur.getService());
        demande.setStatut(StatutDemande.BROUILLON);
        demande.setDateCreation(java.time.LocalDateTime.now());

        // Récupération des objets complets pour garantir la cohérence (Catégorie, Budget)
        if (demande.getBudgetFamille() != null) {
            demande.setBudgetFamille(familyRepository.findById(demande.getBudgetFamille().getIdFamily()).orElse(null));
        }
        if (demande.getBudgetSousFamille() != null) {
            demande.setBudgetSousFamille(subFamilyRepository.findById(demande.getBudgetSousFamille().getOidSub()).orElse(null));
        }
        
        // Calcul de la quantité totale et liaison des détails
        if (demande.getDetails() != null && !demande.getDetails().isEmpty()) {
            int totalQty = 0;
            for (DaDetails detail : demande.getDetails()) {
                detail.setDemandeAchatInterne(demande);
                detail.setSubFamily(demande.getBudgetSousFamille());
                if (detail.getQuantite() != null) totalQty += detail.getQuantite();
            }
            demande.setQuantite(totalQty);
        }

        DemandeAchatInterne saved = demandeRepository.save(demande);
        saveHistory(saved, null, saved.getStatut().name(), demandeur, "Création de la demande");
        return saved;
    }

    @Transactional
    public DemandeAchatInterne soumettre(Long id, User demandeur) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        
        boolean stockSuffisant = warehouseService.verifierStock(demande.getDesignation(), demande.getQuantite());
        
        if (stockSuffisant) {
            warehouseService.affecterStock(demande.getDesignation(), demande.getQuantite(), "Affectation DA #" + id);
            updateStatut(demande, StatutDemande.AFFECTEE, demandeur, "Affectation directe depuis le stock interne");
        } else {
            updateStatut(demande, StatutDemande.SOUMISE, demandeur, "Soumission pour validation N+1 (Stock insuffisant)");
        }
        
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne validerN1(Long id, boolean valider, String commentaire, User n1) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        if (valider) {
            updateStatut(demande, StatutDemande.VALIDEE_N1, n1, commentaire);
        } else {
            if (commentaire == null || commentaire.trim().isEmpty()) {
                throw new RuntimeException("Le commentaire de rejet est obligatoire");
            }
            updateStatut(demande, StatutDemande.REJETEE, n1, commentaire);
            demande.setCommentaireRejet(commentaire);
        }
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne validerTechnicien(Long id, boolean valider, String commentaire, User tech) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        if (valider) {
            updateStatut(demande, StatutDemande.VALIDEE_TECH, tech, commentaire);
        } else {
            if (commentaire == null || commentaire.trim().isEmpty()) {
                throw new RuntimeException("Le commentaire de rejet est obligatoire");
            }
            updateStatut(demande, StatutDemande.REJETEE, tech, commentaire);
            demande.setCommentaireRejet(commentaire);
        }
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne valoriserDemande(Long id, Double prixUnitaire, Integer supplierId) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        demande.setPrixUnitaire(prixUnitaire);
        demande.setFournisseur(supplierRepository.findById(supplierId).orElseThrow());
        demande.setMontantEstime(java.math.BigDecimal.valueOf(prixUnitaire * (demande.getQuantite() != null ? demande.getQuantite() : 1))); 
        
        // Sécurité : si la sous-famille est manquante, on essaie de la récupérer depuis le fournisseur ou de garder l'existante
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne traiterAchat(Long id, User acheteur) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        
        SubFamily bsf = demande.getBudgetSousFamille();
        if (bsf == null) {
            throw new RuntimeException("Erreur : La sous-famille budgétaire n'est pas définie pour cette demande.");
        }

        java.math.BigDecimal montant = demande.getMontantEstime();
        if (montant == null) {
            throw new RuntimeException("Erreur : Le montant estimé n'a pas été défini.");
        }

        // Si budget suffisant -> Circuit de validation financière
        if (bsf.getBudgetRestant() != null && bsf.getBudgetRestant().compareTo(montant) >= 0) {
            updateStatut(demande, StatutDemande.EN_VALIDATION_AMG, acheteur, "Budget suffisant, transmission au circuit de validation AMG/DAF/DG");
        } else {
            // Sinon, on reste en traitement pour que l'acheteur sollicite un ajustement
            updateStatut(demande, StatutDemande.EN_TRAITEMENT, acheteur, "Budget insuffisant détecté (Dispo: " + bsf.getBudgetRestant() + "). Ajustement requis.");
        }
        
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne ajustementBudget(Long id, TypeAjustement type, User acheteur) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        demande.setTypeAjustement(type);
        if (type == TypeAjustement.SOUS_FAMILLE) {
            updateStatut(demande, StatutDemande.AJUSTEMENT_DAF, acheteur, "Demande d'ajustement sous-famille");
        } else {
            updateStatut(demande, StatutDemande.AJUSTEMENT_DG, acheteur, "Demande d'ajustement famille");
        }
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne validerAjustement(Long id, boolean valider, String commentaire, User validateur) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        if (valider) {
            updateStatut(demande, StatutDemande.EN_VALIDATION_AMG, validateur, "Ajustement accepté, passage au circuit final");
        } else {
            updateStatut(demande, StatutDemande.EN_TRAITEMENT, validateur, "Ajustement refusé : " + commentaire);
        }
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne validerAMG(Long id, boolean valider, String commentaire, User amg) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        if (valider) {
            updateStatut(demande, StatutDemande.EN_VALIDATION_DAF, amg, commentaire);
        } else {
            updateStatut(demande, StatutDemande.EN_TRAITEMENT, amg, "Retour Acheteur (AMG) : " + commentaire);
        }
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne validerDAF(Long id, boolean valider, String commentaire, User daf) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        if (valider) {
            updateStatut(demande, StatutDemande.EN_VALIDATION_DG, daf, commentaire);
        } else {
            updateStatut(demande, StatutDemande.EN_TRAITEMENT, daf, "Retour Acheteur (DAF) : " + commentaire);
        }
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne validerDG(Long id, boolean valider, String commentaire, User dg) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        if (valider) {
            updateStatut(demande, StatutDemande.APPROUVEE, dg, commentaire);
        } else {
            updateStatut(demande, StatutDemande.REJETEE, dg, "Rejet définitif (DG) : " + commentaire);
            demande.setCommentaireRejet(commentaire);
        }
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeAchatInterne creerPO(Long id, User acheteur) {
        DemandeAchatInterne demande = demandeRepository.findById(id).orElseThrow();
        if (demande.getStatut() != StatutDemande.APPROUVEE) {
            throw new RuntimeException("La demande doit être APPROUVEE pour créer un PO");
        }
        updateStatut(demande, StatutDemande.PO_CREE, acheteur, "Génération du Bon de Commande (PO)");
        return demandeRepository.save(demande);
    }

    private void updateStatut(DemandeAchatInterne demande, StatutDemande nouveauStatut, User utilisateur, String commentaire) {
        String statutAvant = demande.getStatut() != null ? demande.getStatut().name() : null;
        demande.setStatut(nouveauStatut);
        saveHistory(demande, statutAvant, nouveauStatut.name(), utilisateur, commentaire);
        
        // Notify Demandeur
        notificationService.notifyUser(demande.getDemandeur(), "Mise à jour de votre demande #" + demande.getId(), 
            "Le statut de votre demande est passé à " + nouveauStatut + ". " + (commentaire != null ? commentaire : ""));
        
        // Notify next validator (simplified logic)
        notifyNextValidator(demande);
    }

    private void notifyNextValidator(DemandeAchatInterne demande) {
        // Logic to notify manager, technician, buyer, etc. based on new status
        String topic = null;
        switch (demande.getStatut()) {
            case SOUMISE: topic = "MANAGER_N1"; break;
            case VALIDEE_N1: topic = "TECHNICIEN"; break;
            case VALIDEE_TECH: topic = "ACHETEUR"; break;
            case EN_VALIDATION_AMG: topic = "AMG"; break;
            case EN_VALIDATION_DAF: case AJUSTEMENT_DAF: topic = "DAF"; break;
            case EN_VALIDATION_DG: case AJUSTEMENT_DG: topic = "DG"; break;
            default: break; // Other statuses don't need notifications
        }
        if (topic != null) {
            notificationService.notifyTopic(topic, "Nouvelle demande en attente de validation : #" + demande.getId());
        }
    }

    private void saveHistory(DemandeAchatInterne demande, String avant, String apres, User utilisateur, String commentaire) {
        StatusHistory history = new StatusHistory("DemandeAchatInterne", demande.getId(), avant, apres, utilisateur, commentaire);
        historyRepository.save(history);
        
        AuditLog log = new AuditLog();
        log.setAction("CHANGEMENT_STATUT");
        log.setEntite("DemandeAchatInterne");
        log.setEntiteId(demande.getId());
        log.setUtilisateur(utilisateur);
        log.setValeurAvant(avant);
        log.setValeurApres(apres);
        auditLogRepository.save(log);
    }

    public List<DemandeAchatInterne> getMesDemandes(User demandeur) {
        return demandeRepository.findByDemandeur(demandeur);
    }

    public List<DemandeAchatInterne> getDemandesAValider(User utilisateur) {
        Role role = utilisateur.getRole();
        switch (role) {
            case MANAGER_N1:
                return demandeRepository.findByStatutAndDemandeur_N1(StatutDemande.SOUMISE, utilisateur);
            case TECHNICIEN:
                return demandeRepository.findByStatut(StatutDemande.VALIDEE_N1);
            case ACHETEUR:
                return demandeRepository.findByStatutIn(List.of(StatutDemande.VALIDEE_TECH, StatutDemande.EN_TRAITEMENT));
            case AMG:
                return demandeRepository.findByStatut(StatutDemande.EN_VALIDATION_AMG);
            case DAF:
                return demandeRepository.findByStatutIn(List.of(StatutDemande.EN_VALIDATION_DAF, StatutDemande.AJUSTEMENT_DAF));
            case DG:
                return demandeRepository.findByStatutIn(List.of(StatutDemande.EN_VALIDATION_DG, StatutDemande.AJUSTEMENT_DG));
            default:
                return List.of();
        }
    }
}
