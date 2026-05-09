package com.pfe.gestionsachat.chatbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.gestionsachat.chatbot.model.ChatSession;
import com.pfe.gestionsachat.chatbot.repository.ChatSessionRepository;
import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.model.Role;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.FamilyRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import com.pfe.gestionsachat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class ChatbotForensicIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private SubFamilyRepository subFamilyRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Family testFamily;
    private SubFamily testSubFamily;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        // Nettoyage contextuel optionnel car @Transactional rollback déjà
        User u = new User();
        u.setNom("Testeur Forensic");
        u.setEmail("forensic@test.com");
        u.setPassword("password");
        u.setActif(true);
        u.setRole(Role.EMPLOYE);
        testUser = userRepository.save(u);

        Family f = new Family();
        f.setLibelle("Informatique");
        f.setBudgetInitial(BigDecimal.valueOf(10000));
        f.setBudgetRestant(BigDecimal.valueOf(10000));
        testFamily = familyRepository.save(f);

        SubFamily sf = new SubFamily();
        sf.setLibelle("PC");
        sf.setBudgetInitial(BigDecimal.valueOf(5000));
        sf.setBudgetRestant(BigDecimal.valueOf(5000));
        sf.setFamily(testFamily);
        testSubFamily = subFamilyRepository.save(sf);

        String response = mockMvc.perform(post("/api/chatbot/session")
                .param("userId", String.valueOf(testUser.getOidUser())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> map = objectMapper.readValue(response, Map.class);
        sessionId = (String) map.get("sessionId");
    }

    @Test
    void testLeTrollNumerique_JustificationMaintientSonIntegrite() throws Exception {
        sendMessage("Je veux 1 ordinateur portable");
        sendMessage("PC"); 
        sendMessage("Normale"); 

        mockMvc.perform(post("/api/chatbot/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "sessionId", sessionId,
                        "userId", testUser.getOidUser(),
                        "message", "il est tombé du 4eme étage en 2024 et a 3 fissures"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complet", is(true)))
                .andExpect(jsonPath("$.slots.justification").value("il est tombé du 4eme étage en 2024 et a 3 fissures"))
                .andExpect(jsonPath("$.slots.quantite").value(1));
    }

    @Test
    void testBoucleInfinieOui_CorrectionConfirmee() throws Exception {
        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow();
        String jsonSlots = String.format(
            "{\"designation\":\"Test\",\"quantite\":1,\"justification\":\"justif\",\"urgence\":\"NORMALE\",\"familyId\":%d,\"subFamilyId\":%d,\"familyLibelle\":\"%s\",\"subFamilyLibelle\":\"%s\"}",
            testFamily.getIdFamily(), testSubFamily.getOidSub(), testFamily.getLibelle(), testSubFamily.getLibelle()
        );
        session.setSlotsJson(jsonSlots);
        chatSessionRepository.save(session);

        mockMvc.perform(post("/api/chatbot/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "sessionId", sessionId,
                        "userId", testUser.getOidUser(),
                        "message", "oui"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botMessage").value("Merci de valider en cliquant sur le bouton de confirmation."));
    }

    @Test
    void testGuillotineSecurite_DesactivationUtilisateurAvantConfirmation() throws Exception {
        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow();
        String jsonSlots = String.format(
            "{\"designation\":\"Test\",\"quantite\":1,\"justification\":\"justif\",\"urgence\":\"NORMALE\",\"familyId\":%d,\"subFamilyId\":%d,\"familyLibelle\":\"%s\",\"subFamilyLibelle\":\"%s\"}",
            testFamily.getIdFamily(), testSubFamily.getOidSub(), testFamily.getLibelle(), testSubFamily.getLibelle()
        );
        session.setSlotsJson(jsonSlots);
        chatSessionRepository.save(session);

        testUser.setActif(false);
        userRepository.save(testUser);

        mockMvc.perform(post("/api/chatbot/confirmer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "sessionId", sessionId,
                        "userId", testUser.getOidUser()
                ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDoubleSoumission_TransactionIsolations() throws Exception {
        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow();
        String jsonSlots = String.format(
            "{\"designation\":\"Test\",\"quantite\":1,\"justification\":\"justif\",\"urgence\":\"NORMALE\",\"familyId\":%d,\"subFamilyId\":%d,\"familyLibelle\":\"%s\",\"subFamilyLibelle\":\"%s\"}",
            testFamily.getIdFamily(), testSubFamily.getOidSub(), testFamily.getLibelle(), testSubFamily.getLibelle()
        );
        session.setSlotsJson(jsonSlots);
        chatSessionRepository.save(session);

        // Requête 1 (Succès)
        mockMvc.perform(post("/api/chatbot/confirmer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "sessionId", sessionId,
                        "userId", testUser.getOidUser()
                ))))
                .andExpect(status().isOk());

        // Requête 2 (Même session) -> Devrait déclencher l'exception car la session est déjà confirmée
        mockMvc.perform(post("/api/chatbot/confirmer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "sessionId", sessionId,
                        "userId", testUser.getOidUser()
                ))))
                .andExpect(status().isBadRequest());
    }

    private void sendMessage(String message) throws Exception {
        mockMvc.perform(post("/api/chatbot/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "sessionId", sessionId,
                        "userId", testUser.getOidUser(),
                        "message", message
                ))))
                .andExpect(status().isOk());
    }
}
