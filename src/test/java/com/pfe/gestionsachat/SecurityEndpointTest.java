package com.pfe.gestionsachat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.gestionsachat.model.DemandeAchatInterne;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testPublicEndpointsAreAccessible() throws Exception {
        // Vérifie que les endpoints d'auth ne sont pas bloqués par un 401/403
        mockMvc.perform(get("/api/auth/login?email=test&password=test"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    assert statusCode != 403 : "Security bloquée sur un endpoint public (auth/login)";
                });
    }

    // --- BLOC A : Non-régression des correctifs C1 à C8 ---

    // C1: POST /api/demandes
    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void createDemande_asEmploye_returns200() throws Exception {
        mockMvc.perform(post("/api/demandes")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DemandeAchatInterne())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void createDemande_asComptable_returns403() throws Exception {
        mockMvc.perform(post("/api/demandes")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DemandeAchatInterne())))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createDemande_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/demandes")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DemandeAchatInterne())))
                .andExpect(status().isUnauthorized());
    }

    // C2: POST /api/demandes/{id}/soumettre
    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void soumettreDemande_asEmploye_returns422() throws Exception {
        mockMvc.perform(post("/api/demandes/1/soumettre")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void soumettreDemande_asComptable_returns403() throws Exception {
        mockMvc.perform(post("/api/demandes/1/soumettre")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void soumettreDemande_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/demandes/1/soumettre")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // C3: POST /api/ajustement/famille/soumettre
    @Test
    @WithMockUser(roles = "DG")
    public void ajustementFamille_asDg_returns400() throws Exception {
        String body = "{\"type\":\"FAMILLE\",\"statut\":\"EN_ATTENTE\",\"montantDemande\":1000,\"justificationAcheteur\":\"test\"}";
        mockMvc.perform(post("/api/ajustement/famille/soumettre")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void ajustementFamille_asComptable_returns403() throws Exception {
        String body = "{\"type\":\"FAMILLE\",\"statut\":\"EN_ATTENTE\",\"montantDemande\":1000,\"justificationAcheteur\":\"test\"}";
        mockMvc.perform(post("/api/ajustement/famille/soumettre")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    public void ajustementFamille_withoutJwt_returns401() throws Exception {
        String body = "{\"type\":\"FAMILLE\",\"statut\":\"EN_ATTENTE\",\"montantDemande\":1000,\"justificationAcheteur\":\"test\"}";
        mockMvc.perform(post("/api/ajustement/famille/soumettre")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    // C4: POST /api/ajustement/sous-famille/soumettre
    @Test
    @WithMockUser(roles = "DAF")
    public void ajustementSousFamille_asDaf_returns400() throws Exception {
        String body = "{\"type\":\"FAMILLE\",\"statut\":\"EN_ATTENTE\",\"montantDemande\":1000,\"justificationAcheteur\":\"test\"}";
        mockMvc.perform(post("/api/ajustement/sous-famille/soumettre")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void ajustementSousFamille_asComptable_returns403() throws Exception {
        String body = "{\"type\":\"FAMILLE\",\"statut\":\"EN_ATTENTE\",\"montantDemande\":1000,\"justificationAcheteur\":\"test\"}";
        mockMvc.perform(post("/api/ajustement/sous-famille/soumettre")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    public void ajustementSousFamille_withoutJwt_returns401() throws Exception {
        String body = "{\"type\":\"FAMILLE\",\"statut\":\"EN_ATTENTE\",\"montantDemande\":1000,\"justificationAcheteur\":\"test\"}";
        mockMvc.perform(post("/api/ajustement/sous-famille/soumettre")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    // C5: POST /api/purchase-orders/{id}/auto-grn-grc
    @Test
    @WithMockUser(roles = "ACHETEUR")
    public void autoGrnGrc_asAcheteur_returns422() throws Exception {
        mockMvc.perform(post("/api/purchase-orders/1/auto-grn-grc")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void autoGrnGrc_asComptable_returns403() throws Exception {
        mockMvc.perform(post("/api/purchase-orders/1/auto-grn-grc")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void autoGrnGrc_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/purchase-orders/1/auto-grn-grc")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // C6: POST /api/chatbot/session
    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void chatbotSession_asEmploye_returns200() throws Exception {
        mockMvc.perform(post("/api/chatbot/session")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void chatbotSession_asComptable_returns200() throws Exception {
        mockMvc.perform(post("/api/chatbot/session")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void chatbotSession_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/chatbot/session")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // C7: POST /api/chatbot/message
    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void chatbotMessage_asEmploye_returns500() throws Exception {
        String body = "{\"sessionId\":\"1\",\"userId\":1,\"message\":\"test\"}";
        mockMvc.perform(post("/api/chatbot/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void chatbotMessage_asComptable_returns500() throws Exception {
        String body = "{\"sessionId\":\"1\",\"userId\":1,\"message\":\"test\"}";
        mockMvc.perform(post("/api/chatbot/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void chatbotMessage_withoutJwt_returns401() throws Exception {
        String body = "{\"sessionId\":\"1\",\"userId\":1,\"message\":\"test\"}";
        mockMvc.perform(post("/api/chatbot/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    // C8: POST /api/chatbot/confirmer
    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void chatbotConfirmer_asEmploye_returns500() throws Exception {
        String body = "{\"sessionId\":\"1\",\"userId\":1}";
        mockMvc.perform(post("/api/chatbot/confirmer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void chatbotConfirmer_asComptable_returns500() throws Exception {
        String body = "{\"sessionId\":\"1\",\"userId\":1}";
        mockMvc.perform(post("/api/chatbot/confirmer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void chatbotConfirmer_withoutJwt_returns401() throws Exception {
        String body = "{\"sessionId\":\"1\",\"userId\":1}";
        mockMvc.perform(post("/api/chatbot/confirmer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --- BLOC B : Régression des endpoints déjà protégés ---
    @Test
    @WithMockUser(roles = "MANAGER_N1")
    public void validerN1_asManagerN1_returns200() throws Exception {
        mockMvc.perform(put("/api/demandes/1/valider-n1")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valider\":true,\"commentaire\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void validerN1_asEmploye_returns403() throws Exception {
        mockMvc.perform(put("/api/demandes/1/valider-n1")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valider\":true,\"commentaire\":\"ok\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DAF")
    public void validerDaf_asDaf_returns422() throws Exception {
        mockMvc.perform(put("/api/demandes/1/valider-daf")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valider\":true,\"commentaire\":\"ok\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "ACHETEUR")
    public void validerDaf_asAcheteur_returns403() throws Exception {
        mockMvc.perform(put("/api/demandes/1/valider-daf")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valider\":true,\"commentaire\":\"ok\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATEUR")
    public void createAdminUser_asAdministrateur_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void createAdminUser_asEmploye_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    // --- BLOC C : Minimum sécurité GET nus ---
    @Test
    public void getDemandes_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/demandes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getPurchaseOrders_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getGrn_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/grn"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getInvoice_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void downloadGrn_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/grn/1/pdf"))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════
    // BLOC D — Logistique Aval
    // PurchaseOrder · GRN · Invoice · Transfer
    // ═══════════════════════════════════════════════

    // --- PurchaseOrderController ---

    // PUT /api/purchase-orders/{id}/approve : RESP_ACHAT -> returnsNot403
    @Test
    @WithMockUser(roles = "RESP_ACHAT")
    public void approvePurchaseOrder_asRespAchat_returnsNot403() throws Exception {
        mockMvc.perform(put("/api/purchase-orders/1/approve")
                .param("userId", "999"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void approvePurchaseOrder_asComptable_returns403() throws Exception {
        mockMvc.perform(put("/api/purchase-orders/1/approve")
                .param("userId", "999"))
                .andExpect(status().isForbidden());
    }

    // D2: PurchaseOrder - Reject
    @Test
    @WithMockUser(roles = "RESP_ACHAT")
    public void rejectPurchaseOrder_asRespAchat_returnsNot403() throws Exception {
        mockMvc.perform(put("/api/purchase-orders/1/reject")
                .param("userId", "999")
                .param("motif", "test"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    // D3: PurchaseOrder - Short Close
    @Test
    @WithMockUser(roles = "ACHETEUR")
    public void shortClosePurchaseOrder_asAcheteur_returnsNot403() throws Exception {
        mockMvc.perform(put("/api/purchase-orders/1/short-close")
                .param("userId", "999")
                .param("motif", "test"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "MAGASINIER")
    public void shortClosePurchaseOrder_asMagasinier_returns403() throws Exception {
        mockMvc.perform(put("/api/purchase-orders/1/short-close")
                .param("userId", "999")
                .param("motif", "test"))
                .andExpect(status().isForbidden());
    }

    // D4: GRN - Valider
    @Test
    @WithMockUser(roles = "MAGASINIER")
    public void validerGrn_asMagasinier_returnsNot403() throws Exception {
        mockMvc.perform(put("/api/grn/1/valider")
                .param("userId", "999"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "ACHETEUR")
    public void validerGrn_asAcheteur_returns403() throws Exception {
        mockMvc.perform(put("/api/grn/1/valider")
                .param("userId", "999"))
                .andExpect(status().isForbidden());
    }

    // D5: Invoice - Match
    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void matchInvoice_asComptable_returnsNot403() throws Exception {
        mockMvc.perform(post("/api/invoice/1/match")
                .param("userId", "999"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "ACHETEUR")
    public void matchInvoice_asAcheteur_returns403() throws Exception {
        mockMvc.perform(post("/api/invoice/1/match")
                .param("userId", "999"))
                .andExpect(status().isForbidden());
    }

    // D6: Invoice - Approve
    @Test
    @WithMockUser(roles = "DAF")
    public void approveInvoice_asDaf_returnsNot403() throws Exception {
        mockMvc.perform(post("/api/invoice/1/approve")
                .param("userId", "999"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void approveInvoice_asComptable_returns403() throws Exception {
        mockMvc.perform(post("/api/invoice/1/approve")
                .param("userId", "999"))
                .andExpect(status().isForbidden());
    }

    // D7: Transfer - Bulk
    @Test
    @WithMockUser(roles = "MAGASINIER")
    public void bulkTransfer_asMagasinier_returnsNot403() throws Exception {
        mockMvc.perform(post("/api/transfers/bulk")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    // D8: Transfer - Ship
    @Test
    @WithMockUser(roles = "MAGASINIER")
    public void shipTransfer_asMagasinier_returnsNot403() throws Exception {
        mockMvc.perform(put("/api/transfers/1/ship")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void shipTransfer_asEmploye_returns403() throws Exception {
        mockMvc.perform(put("/api/transfers/1/ship")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    // D9: Transfer - Receive
    @Test
    @WithMockUser(roles = "MAGASINIER_DEST")
    public void receiveTransfer_asMagasinierDest_returnsNot403() throws Exception {
        mockMvc.perform(put("/api/transfers/1/receive")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "MAGASINIER")
    public void receiveTransfer_asMagasinier_returns403() throws Exception {
        mockMvc.perform(put("/api/transfers/1/receive")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    // D10: Transfer - Delete
    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void deleteTransfer_asEmploye_returnsNot403() throws Exception {
        mockMvc.perform(delete("/api/transfers/1")
                .param("userId", "1"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    public void approvePurchaseOrder_withoutJwt_returns401() throws Exception {
        mockMvc.perform(put("/api/purchase-orders/1/approve")
                .param("userId", "999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void rejectPurchaseOrder_asComptable_returns403() throws Exception {
        mockMvc.perform(put("/api/purchase-orders/1/reject")
                .param("userId", "999")
                .param("motif", "test"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void rejectPurchaseOrder_withoutJwt_returns401() throws Exception {
        mockMvc.perform(put("/api/purchase-orders/1/reject")
                .param("userId", "999")
                .param("motif", "test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MAGASINIER")
    public void createGrn_asMagasinier_returnsNot403() throws Exception {
        mockMvc.perform(post("/api/grn")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "EMPLOYE")
    public void createGrn_asEmploye_returns403() throws Exception {
        mockMvc.perform(post("/api/grn")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createGrn_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/grn")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void createInvoice_asComptable_returnsNot403() throws Exception {
        mockMvc.perform(post("/api/invoice")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "ACHETEUR")
    public void createInvoice_asAcheteur_returns403() throws Exception {
        mockMvc.perform(post("/api/invoice")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createInvoice_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/invoice")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MAGASINIER")
    public void createTransfer_asMagasinier_returnsNot403() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    assert result.getResponse().getStatus() != 403 : "Bloqué à tort";
                });
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void createTransfer_asComptable_returns403() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createTransfer_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/transfers")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void matchInvoice_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/invoice/1/match")
                .param("userId", "999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void approveInvoice_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/invoice/1/approve")
                .param("userId", "999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "COMPTABLE")
    public void bulkTransfer_asComptable_returns403() throws Exception {
        mockMvc.perform(post("/api/transfers/bulk")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void bulkTransfer_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/transfers/bulk")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shipTransfer_withoutJwt_returns401() throws Exception {
        mockMvc.perform(put("/api/transfers/1/ship")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void receiveTransfer_withoutJwt_returns401() throws Exception {
        mockMvc.perform(put("/api/transfers/1/receive")
                .param("userId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MAGASINIER")
    public void deleteTransfer_asMagasinier_returns403() throws Exception {
        mockMvc.perform(delete("/api/transfers/1")
                .param("userId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteTransfer_withoutJwt_returns401() throws Exception {
        mockMvc.perform(delete("/api/transfers/1")
                .param("userId", "1"))
                .andExpect(status().isUnauthorized());
    }
}
