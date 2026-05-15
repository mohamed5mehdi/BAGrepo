package com.pfe.gestionsachat.chatbot.controller;

import com.pfe.gestionsachat.chatbot.dto.ChatResponse;
import com.pfe.gestionsachat.chatbot.model.ChatMessage;
import com.pfe.gestionsachat.chatbot.model.ChatSession;
import com.pfe.gestionsachat.chatbot.model.SlotState;
import com.pfe.gestionsachat.chatbot.repository.ChatMessageRepository;
import com.pfe.gestionsachat.chatbot.repository.ChatSessionRepository;
import com.pfe.gestionsachat.chatbot.service.DialogService;
import com.pfe.gestionsachat.model.DemandeAchatInterne;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Point d'entrée REST du chatbot d'achat.
 * Toutes les réponses valident l'existence et l'état actif de l'utilisateur.
 */
@RestController
@RequestMapping("/api/chatbot")

public class ChatbotController {

    @Autowired
    private DialogService dialogService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private UserRepository userRepository;

    // ─── POST /api/chatbot/session ────────────────────────────────────────────

    /**
     * Démarre ou reprend une session chatbot pour un utilisateur authentifié.
     * Retourne un ChatResponse avec le message de bienvenue.
     */
    @PostMapping("/session")
    public ResponseEntity<?> demarrerSession(@RequestParam Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getActif())) {
            return ResponseEntity.status(403).body("Accès interdit : utilisateur inactif ou introuvable.");
        }

        try {
            ChatSession session = dialogService.demarrerSession(userId);

            SlotState slots = new SlotState();
            String welcome = "Bonjour ! 👋 Que souhaitez-vous commander ?";

            ChatResponse response = new ChatResponse(
                    session.getId(),
                    welcome,
                    slots,
                    false,
                    false,
                    null
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(500).body("Erreur démarrage session : " + ex.getMessage());
        }
    }

    // ─── POST /api/chatbot/message ────────────────────────────────────────────

    /**
     * Traite un message utilisateur et retourne la réponse bot avec le SlotState mis à jour.
     * Body attendu : { "sessionId": "...", "userId": 1, "message": "..." }
     */
    @PostMapping("/message")
    public ResponseEntity<?> envoyerMessage(@RequestBody Map<String, Object> body) {
        Integer payloadUserId = extractUserId(body.get("userId"));
        String sessionId = (String) body.get("sessionId");
        String message = (String) body.get("message");

        if (payloadUserId == null || sessionId == null || message == null) {
            return ResponseEntity.badRequest().body("Paramètres manquants : sessionId, userId et message requis.");
        }

        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow(() -> new RuntimeException("Session introuvable"));
        
        if (!session.getUserId().equals(payloadUserId)) {
            return ResponseEntity.status(403).body("Accès non autorisé");
        }

        Optional<User> userOpt = userRepository.findById(payloadUserId);
        if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getActif())) {
            return ResponseEntity.status(403).body("Compte désactivé");
        }

        try {
            ChatResponse response = dialogService.processerMessage(sessionId, payloadUserId, message);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(400).body(ex.getMessage());
        }
    }

    // ─── POST /api/chatbot/confirmer ──────────────────────────────────────────

    /**
     * Confirme la demande et soumet la DA.
     * Body attendu : { "sessionId": "...", "userId": 1 }
     */
    @PostMapping("/confirmer")
    public ResponseEntity<?> confirmerDemande(@RequestBody Map<String, Object> body) {
        Integer payloadUserId = extractUserId(body.get("userId"));
        String sessionId = (String) body.get("sessionId");

        if (payloadUserId == null || sessionId == null) {
            return ResponseEntity.badRequest().body("Paramètres manquants : sessionId et userId requis.");
        }

        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow(() -> new RuntimeException("Session introuvable"));

        if (!session.getUserId().equals(payloadUserId)) {
            return ResponseEntity.status(403).body("Accès non autorisé");
        }

        Optional<User> userOpt = userRepository.findById(payloadUserId);
        if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getActif())) {
            return ResponseEntity.status(403).body("Compte désactivé");
        }

        try {
            DemandeAchatInterne da = dialogService.confirmerEtSoumettre(sessionId, payloadUserId);
            return ResponseEntity.ok(da);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(400).body(ex.getMessage());
        }
    }

    // ─── GET /api/chatbot/session/{sessionId}/messages ────────────────────────

    /**
     * Retourne l'historique complet des messages d'une session, triés chronologiquement.
     */
    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable String sessionId,
            @RequestParam Integer userId) {

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getActif())) {
            return ResponseEntity.status(403).body("Accès interdit : utilisateur inactif ou introuvable.");
        }

        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByDateEnvoiAsc(sessionId);
        return ResponseEntity.ok(messages);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Extrait un Integer depuis un Object JSON (Number ou String).
     */
    private Integer extractUserId(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number) return ((Number) raw).intValue();
        try { return Integer.parseInt(raw.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}

