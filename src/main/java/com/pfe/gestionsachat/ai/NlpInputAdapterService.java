package com.pfe.gestionsachat.ai;

import com.pfe.gestionsachat.model.DemandeAchatInterne;
import com.pfe.gestionsachat.model.UrgenceDemande;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.service.DemandeAchatInterneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NLP Input Adapter (Architecture Hexagonale).
 * Traduit un prompt en langage naturel vers un objet métier compréhensible 
 * par le coeur de l'ERP (DemandeAchatInterneService).
 */
@Service
public class NlpInputAdapterService {

    @Autowired
    private DemandeAchatInterneService demandeService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Traduit un prompt en langage naturel en une DemandeAchatInterne persistée.
     * Toute la logique métier (idempotence, anti-flood, statut, audit) est
     * déléguée à {@link DemandeAchatInterneService#createDemande}.
     *
     * @param prompt  Texte brut saisi par l'employé via le Chatbot.
     * @param userId  Identifiant de l'utilisateur connecté (issu du JWT).
     * @return La demande créée avec statut BROUILLON.
     */
    @Transactional
    public DemandeAchatInterne createDemandeFromPrompt(String prompt, Integer userId) {
        // 1. Initialisation d'une Demande vierge
        DemandeAchatInterne nouvelleDemande = new DemandeAchatInterne();
        
        // 2. Extraction NLP de la Quantité (Ex: "Il me faut 5 PC")
        Matcher quantiteMatcher = Pattern.compile("\\b(\\d+)\\b").matcher(prompt);
        if (quantiteMatcher.find()) {
            nouvelleDemande.setQuantite(Integer.parseInt(quantiteMatcher.group(1)));
        } else {
            nouvelleDemande.setQuantite(1); // Valeur par défaut
        }

        // 3. Extraction NLP de l'Urgence (Recherche sémantique basique)
        if (prompt.toLowerCase().contains("urgent") || prompt.toLowerCase().contains("vite") || prompt.toLowerCase().contains("asap")) {
            nouvelleDemande.setUrgence(UrgenceDemande.URGENTE);
        } else {
            nouvelleDemande.setUrgence(UrgenceDemande.NORMALE);
        }

        // 4. Extraction du Département (Ex: "pour le service IT")
        Matcher deptMatcher = Pattern.compile("service\\s+([a-zA-Z]+)|departement\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE).matcher(prompt);
        if (deptMatcher.find()) {
            nouvelleDemande.setDepartement(deptMatcher.group(1) != null ? deptMatcher.group(1).toUpperCase() : deptMatcher.group(2).toUpperCase());
        }

        // 5. La désignation prend le reste du prompt (hors mots de liaison - simplifié ici)
        nouvelleDemande.setDesignation(prompt);
        nouvelleDemande.setJustification("Généré automatiquement via Chatbot AI.");

        // 6. INJECTION DANS LE COEUR MÉTIER :
        // On ne recode aucune règle métier ! On appelle le service officiel
        // qui gère l'idempotence (token), l'anti-flood et les verrous budgétaires.
        User demandeur = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'id : " + userId));
        return demandeService.createDemande(nouvelleDemande, demandeur);
    }
}
