package com.pfe.gestionsachat.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.gestionsachat.ai.InsightGeneratorService;
import com.pfe.gestionsachat.chatbot.dto.ChatResponse;
import com.pfe.gestionsachat.chatbot.model.ChatMessage;
import com.pfe.gestionsachat.chatbot.model.ChatMessageRole;
import com.pfe.gestionsachat.chatbot.model.ChatSession;
import com.pfe.gestionsachat.chatbot.model.ChatSessionStatut;
import com.pfe.gestionsachat.chatbot.model.SlotState;
import com.pfe.gestionsachat.chatbot.repository.ChatMessageRepository;
import com.pfe.gestionsachat.chatbot.repository.ChatSessionRepository;
import com.pfe.gestionsachat.model.DemandeAchatInterne;
import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.FamilyRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.service.DemandeAchatInterneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrateur conversationnel — gère le cycle de vie des sessions chatbot,
 * l'extraction de slots NLP et la soumission automatique de DA.
 */
@Service
public class DialogService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private NlpService nlpService;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private SubFamilyRepository subFamilyRepository;

    @Autowired
    private DemandeAchatInterneService demandeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InsightGeneratorService insightGeneratorService;

    // ─── MÉTHODE 1 : demarrerSession ─────────────────────────────────────────

    /**
     * Démarre ou reprend une session chatbot pour un utilisateur.
     * - Session ACTIVE de moins de 30 minutes → retournée telle quelle.
     * - Session ACTIVE expirée (>30 min) → marquée ABANDONNEE, nouvelle créée.
     */
    @Transactional
    public ChatSession demarrerSession(Integer userId) {
        Optional<ChatSession> existing = chatSessionRepository.findFirstByUserIdAndStatutOrderByDateCreationDesc(userId,
                ChatSessionStatut.ACTIVE);

        if (existing.isPresent()) {
            ChatSession session = existing.get();
            long minutesEcoulees = ChronoUnit.MINUTES.between(
                    session.getDateDernierMsg(), LocalDateTime.now());

            if (minutesEcoulees > 30) {
                session.setStatut(ChatSessionStatut.ABANDONNEE);
                chatSessionRepository.save(session);
                // continuer → créer une nouvelle session
            } else {
                return session;
            }
        }

        // Créer une nouvelle session
        ChatSession newSession = new ChatSession(userId);
        try {
            newSession.setSlotsJson(objectMapper.writeValueAsString(new SlotState()));
        } catch (Exception e) {
            newSession.setSlotsJson("{}");
        }
        return chatSessionRepository.save(newSession);
    }

    // ─── MÉTHODE 2 : processerMessage ────────────────────────────────────────

    /**
     * Traite un message utilisateur :
     * 1. Sécurité session + ownership
     * 2. Détection intentions spéciales (reset, annulation)
     * 3. Extraction NLP des slots
     * 4. Génération réponse bot
     * 5. Persistance messages + session
     */
    @Transactional
    public ChatResponse processerMessage(String sessionId, Integer userId, String message) {

        // 1. Charger session avec verrou pessimiste
        ChatSession session = chatSessionRepository.findByIdWithLock(sessionId)
                .orElseThrow(() -> new RuntimeException("Session introuvable"));

        // 2. Vérifier ownership et statut
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Accès non autorisé");
        }
        if (session.getStatut() == ChatSessionStatut.CONFIRMEE || session.getStatut() == ChatSessionStatut.ABANDONNEE) {
            throw new RuntimeException(
                    "Impossible de modifier une session " + session.getStatut().name().toLowerCase());
        }

        // 3. Désérialiser slotsJson → SlotState
        SlotState slots = deserializeSlots(session.getSlotsJson());

        // 4. Détecter intentions spéciales — normalisé (sans accents) pour robustesse
        String lower = normalizeMsg(message);

        // 4.0 Commande analytique IA — interceptée avant tout traitement de slots
        if (lower.matches(".*(resume|analyse|bilan|dashboard|rapport|statistique|recap|bilon|analise).*")) {
            String botMsg;
            try {
                botMsg = insightGeneratorService.genererInsights().resumeChatbot();
            } catch (Exception e) {
                botMsg = "📊 Analyse indisponible momentanément. Consultez le tableau de bord BI pour les statistiques complètes.";
            }
            persistMessages(session, message, botMsg, slots);
            updateSession(session, slots);
            return buildResponse(session, botMsg, slots);
        }

        if (lower.contains("recommencer") || lower.contains("reset") || lower.contains("annuler tout")) {
            slots = new SlotState();
            String botMsg = "Conversation réinitialisée. Que souhaitez-vous commander ?";
            persistMessages(session, message, botMsg, slots);
            updateSession(session, slots);
            return buildResponse(session, botMsg, slots);
        }

        if (lower.equals("non") || lower.equals("annule") || lower.equals("annuler") || lower.equals("change")
                || lower.equals("changer") || lower.equals("modifie") || lower.equals("modifier")) {
            String lastSlot = findDernierSlotRempli(slots);
            if (lastSlot != null) {
                remettreSlotANull(slots, lastSlot);
            }
            String botMsg = genererReponse(slots);
            persistMessages(session, message, botMsg, slots);
            updateSession(session, slots);
            return buildResponse(session, botMsg, slots);
        }

        if (slots.isComplet() && (lower.equals("oui") || lower.equals("ok") || lower.equals("je confirme")
                || lower.equals("confirmer") || lower.equals("valider"))) {
            String botMsg = "Merci de valider en cliquant sur le bouton de confirmation.";
            persistMessages(session, message, botMsg, slots);
            updateSession(session, slots);
            return buildResponse(session, botMsg, slots);
        }

        // 5. Extraire les slots
        slots = nlpService.extractSlots(message, slots);

        // 6. Générer réponse bot
        String botMsg = genererReponse(slots);

        // 7 & 8. Sauvegarder messages USER et BOT
        persistMessages(session, message, botMsg, slots);

        // 9 & 10. Mettre à jour et sauvegarder session
        updateSession(session, slots);

        // 11. Retourner ChatResponse
        return buildResponse(session, botMsg, slots);
    }

    // ─── MÉTHODE 3 : genererReponse (privée) ─────────────────────────────────

    /**
     * Génère la prochaine question bot selon le slot manquant.
     */
    private String genererReponse(SlotState slots) {
        String prochainSlot = slots.getProchainSlotManquant();

        switch (prochainSlot) {
            case "DESIGNATION":
                return "Bonjour ! 👋 Que souhaitez-vous commander ?";

            case "QUANTITE":
                return "Quelle quantité souhaitez-vous pour **" + slots.getDesignation() + "** ?";

            case "FAMILLE":
                return "Dans quelle catégorie ?" +
                        "\n• Informatique" +
                        "\n• Logiciels & Licences" +
                        "\n• Bureautique & Mobilier" +
                        "\n• Fournitures & Services";

            case "SOUS_FAMILLE": {
                String familyLib = slots.getFamilyLibelle() != null ? slots.getFamilyLibelle() : "cette catégorie";
                StringBuilder sb = new StringBuilder("Sous quelle catégorie pour **");
                sb.append(familyLib).append("** ?");
                // Charger les sous-familles dynamiquement depuis la base
                if (slots.getFamilyId() != null) {
                    List<SubFamily> subs = subFamilyRepository.findByFamilyId(slots.getFamilyId());
                    if (!subs.isEmpty()) {
                        for (SubFamily s : subs) {
                            sb.append("\n• ").append(s.getLibelle());
                        }
                    }
                }
                return sb.toString();
            }

            case "JUSTIFICATION":
                return "Quelle est la justification de cette demande ?";

            case "URGENCE":
                return "Quel niveau d'urgence ?" +
                        "\n• Normale" +
                        "\n• Urgente" +
                        "\n• Critique";

            case "COMPLET":
                return "📋 **Récapitulatif de votre demande :**" +
                        "\n- **Article**     : " + (slots.getDesignation() != null ? slots.getDesignation() : "-") +
                        "\n- **Quantité**   : " + (slots.getQuantite() != null ? slots.getQuantite() : "-") +
                        "\n- **Catégorie**  : "
                        + (slots.getFamilyLibelle() != null ? slots.getFamilyLibelle() : "Non spécifiée") +
                        "\n- **Sous-cat.**  : "
                        + (slots.getSubFamilyLibelle() != null ? slots.getSubFamilyLibelle() : "Non applicable") +
                        "\n- **Urgence**    : " + (slots.getUrgence() != null ? slots.getUrgence() : "NORMALE") +
                        "\n- **Justif.**    : "
                        + (slots.getJustification() != null ? slots.getJustification() : "Non précisée") +
                        "\n\nConfirmez-vous cette demande ? **(oui/non)**";

            default:
                return "Pouvez-vous reformuler votre demande ?";
        }
    }

    // ─── MÉTHODE 4 : confirmerEtSoumettre ────────────────────────────────────

    /**
     * Valide le SlotState complet, construit et soumet la DA.
     * Règles de sécurité ABSOLUES : ownership + isComplet() vérifiés.
     */
    @Transactional
    public DemandeAchatInterne confirmerEtSoumettre(String sessionId, Integer userId) {

        // 1. Charger avec verrou pessimiste
        ChatSession session = chatSessionRepository.findByIdWithLock(sessionId)
                .orElseThrow(() -> new RuntimeException("Session introuvable"));

        // 2. Vérifier ownership et statut
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Accès non autorisé");
        }
        if (session.getStatut() == ChatSessionStatut.CONFIRMEE || session.getStatut() == ChatSessionStatut.ABANDONNEE) {
            throw new RuntimeException(
                    "Session " + session.getStatut().name().toLowerCase() + " — opération impossible");
        }

        // 3. Désérialiser slots
        SlotState slots = deserializeSlots(session.getSlotsJson());

        // 4. Vérifier complétude
        if (!slots.isComplet()) {
            throw new RuntimeException("La demande est incomplète");
        }

        if (slots.getQuantite() == null || slots.getQuantite() <= 0) {
            throw new RuntimeException("Quantité invalide : doit être supérieure à 0");
        }

        // 5. Charger User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!Boolean.TRUE.equals(user.getActif())) {
            throw new RuntimeException("Utilisateur inactif");
        }

        // 6. Construire DemandeAchatInterne manuellement
        DemandeAchatInterne da = new DemandeAchatInterne();
        da.setDesignation(slots.getDesignation());
        da.setQuantite(slots.getQuantite());
        da.setJustification(slots.getJustification());
        da.setUrgence(slots.getUrgence());
        da.setSubmissionToken(session.getId()); // UUID session réutilisé → idempotence

        // Résoudre et setter budgetFamille
        Family family = familyRepository.findById(slots.getFamilyId())
                .orElseThrow(() -> new RuntimeException("Famille introuvable : " + slots.getFamilyId()));
        da.setBudgetFamille(family);

        // Résoudre et setter budgetSousFamille
        SubFamily subFamily = subFamilyRepository.findById(slots.getSubFamilyId())
                .orElseThrow(() -> new RuntimeException("Sous-famille introuvable : " + slots.getSubFamilyId()));
        da.setBudgetSousFamille(subFamily);

        // categorie déduite de budgetFamille via @PrePersist / setter
        // (setBudgetFamille() synchronise déjà la catégorie)

        // 7. Créer la DA
        DemandeAchatInterne savedDa = demandeService.createDemande(da, user);

        // 8. Soumettre la DA
        demandeService.soumettre(savedDa.getId(), user);

        // 9. Mettre à jour la session
        session.setStatut(ChatSessionStatut.CONFIRMEE);
        session.setDaCreeeId(savedDa.getId());

        // 10. Sauvegarder
        chatSessionRepository.save(session);

        // 11. Retourner la DA
        return savedDa;
    }

    // ─── Helpers privés ──────────────────────────────────────────────────────

    /**
     * Normalise Unicode : supprime les diacritiques pour comparer sans accents.
     * "annulé" → "annule", "modifié" → "modifie".
     */
    private String normalizeMsg(String input) {
        if (input == null)
            return "";
        return Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private SlotState deserializeSlots(String slotsJson) {
        try {
            if (slotsJson == null || slotsJson.isBlank())
                return new SlotState();
            return objectMapper.readValue(slotsJson, SlotState.class);
        } catch (Exception e) {
            return new SlotState();
        }
    }

    private String serializeSlots(SlotState slots) {
        try {
            return objectMapper.writeValueAsString(slots);
        } catch (Exception e) {
            // Ne jamais avaler silencieusement — une perte d'état ici = régression
            // conversationnelle invisible pour l'utilisateur.
            throw new RuntimeException("[DialogService] Échec critique de sérialisation SlotState", e);
        }
    }

    private void persistMessages(ChatSession session, String userMsg, String botMsg, SlotState slots) {
        String slotsJson = serializeSlots(slots);

        ChatMessage userMessage = new ChatMessage(session.getId(), ChatMessageRole.USER, userMsg);
        userMessage.setSlotsSnapshot(slotsJson);
        chatMessageRepository.save(userMessage);

        ChatMessage botMessage = new ChatMessage(session.getId(), ChatMessageRole.BOT, botMsg);
        botMessage.setSlotsSnapshot(slotsJson);
        chatMessageRepository.save(botMessage);
    }

    private void updateSession(ChatSession session, SlotState slots) {
        session.setSlotsJson(serializeSlots(slots));
        session.setDateDernierMsg(LocalDateTime.now());
        chatSessionRepository.save(session);
    }

    private ChatResponse buildResponse(ChatSession session, String botMsg, SlotState slots) {
        return new ChatResponse(
                session.getId(),
                botMsg,
                slots,
                slots.isComplet(),
                session.getStatut() == ChatSessionStatut.CONFIRMEE,
                session.getDaCreeeId());
    }

    /**
     * Identifie le dernier slot rempli dans l'ordre inverse de priorité.
     */
    private String findDernierSlotRempli(SlotState slots) {
        if (slots.getUrgence() != null)
            return "URGENCE";
        if (slots.getJustification() != null)
            return "JUSTIFICATION";
        if (slots.getSubFamilyId() != null)
            return "SOUS_FAMILLE";
        if (slots.getFamilyId() != null)
            return "FAMILLE";
        if (slots.getQuantite() != null)
            return "QUANTITE";
        if (slots.getDesignation() != null)
            return "DESIGNATION";
        return null;
    }

    /**
     * Remet à null le slot identifié.
     */
    private void remettreSlotANull(SlotState slots, String slotName) {
        switch (slotName) {
            case "DESIGNATION":
                slots.setDesignation(null);
                break;
            case "QUANTITE":
                slots.setQuantite(null);
                break;
            case "FAMILLE":
                slots.setFamilyId(null);
                slots.setFamilyLibelle(null);
                break;
            case "SOUS_FAMILLE":
                slots.setSubFamilyId(null);
                slots.setSubFamilyLibelle(null);
                break;
            case "JUSTIFICATION":
                slots.setJustification(null);
                break;
            case "URGENCE":
                slots.setUrgence(null);
                break;
            default:
                break;
        }
    }
}
