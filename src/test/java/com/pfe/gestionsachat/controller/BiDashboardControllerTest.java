package com.pfe.gestionsachat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.gestionsachat.exception.BiSqlException;
import com.pfe.gestionsachat.service.DynamicBiService;
import com.pfe.gestionsachat.service.GeminiBiOrchestratorService;
import com.pfe.gestionsachat.service.OverviewBiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiDashboardControllerTest {

    @Mock
    private DynamicBiService dynamicBiService;

    @Mock
    private GeminiBiOrchestratorService geminiOrchestrator;

    @Mock
    private OverviewBiService overviewBiService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private BiDashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new BiDashboardController(dynamicBiService, geminiOrchestrator, objectMapper, overviewBiService);
    }

    @Test
    @DisplayName("T1 - Retry mechanism succeeds on 3rd attempt")
    void shouldRetryAndSucceedOnThirdAttempt() {
        // GIVEN
        String validJsonResponse = "{\"sql\":\"SELECT * FROM table\", \"visualizations\":[]}";
        when(geminiOrchestrator.generateBiPayload(anyString())).thenReturn(validJsonResponse);

        List<Map<String, Object>> mockData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("col1", "val1");
        mockData.add(row);

        when(dynamicBiService.executeDynamicBiQuery(anyString(), eq(1)))
                .thenThrow(new BiSqlException("erreur simulée 1", "SQL fautif 1", null))
                .thenThrow(new BiSqlException("erreur simulée 2", "SQL fautif 2", null))
                .thenReturn(mockData);

        BiDashboardController.BiQueryRequest request = new BiDashboardController.BiQueryRequest();
        request.setUserQuestion("Question originale");

        // WHEN
        ResponseEntity<BiDashboardController.BiResponseDto> response = controller.executeBiQuery(1, request);

        // THEN
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(mockData);

        verify(dynamicBiService, times(3)).executeDynamicBiQuery(anyString(), eq(1));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(geminiOrchestrator, times(3)).generateBiPayload(captor.capture());

        List<String> prompts = captor.getAllValues();
        assertThat(prompts).hasSize(3);
        assertThat(prompts.get(0)).isEqualTo("Question originale");
        assertThat(prompts.get(1)).contains("erreur simulée 1");
        assertThat(prompts.get(2)).contains("erreur simulée 2");
        
        System.out.println("=== ARGUMENTS DU PROMPT CAPTURÉS ===");
        for (int i = 0; i < prompts.size(); i++) {
            System.out.println("--- Tentative " + (i + 1) + " ---");
            System.out.println(prompts.get(i));
        }
    }

    @Test
    @DisplayName("T2 - Fails after 3 attempts with BiSqlException")
    void shouldFailAfterThreeAttempts() {
        // GIVEN
        String validJsonResponse = "{\"sql\":\"SELECT * FROM table\", \"visualizations\":[]}";
        when(geminiOrchestrator.generateBiPayload(anyString())).thenReturn(validJsonResponse);

        when(dynamicBiService.executeDynamicBiQuery(anyString(), eq(1)))
                .thenThrow(new BiSqlException("erreur simulée 1", "SQL fautif 1", null))
                .thenThrow(new BiSqlException("erreur simulée 2", "SQL fautif 2", null))
                .thenThrow(new BiSqlException("erreur simulée 3", "SQL fautif 3", null));

        BiDashboardController.BiQueryRequest request = new BiDashboardController.BiQueryRequest();
        request.setUserQuestion("Question originale");

        // WHEN / THEN
        assertThatThrownBy(() -> controller.executeBiQuery(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Échec après 3 tentatives : erreur simulée 3");

        verify(dynamicBiService, times(3)).executeDynamicBiQuery(anyString(), eq(1));
    }

    @Test
    @DisplayName("T3 - SecurityException propagates immediately without retry")
    void shouldPropagateSecurityExceptionImmediately() {
        // GIVEN
        String validJsonResponse = "{\"sql\":\"SELECT * FROM table\", \"visualizations\":[]}";
        when(geminiOrchestrator.generateBiPayload(anyString())).thenReturn(validJsonResponse);

        when(dynamicBiService.executeDynamicBiQuery(anyString(), eq(1)))
                .thenThrow(new SecurityException("Accès refusé"));

        BiDashboardController.BiQueryRequest request = new BiDashboardController.BiQueryRequest();
        request.setUserQuestion("Question originale");

        // WHEN / THEN
        assertThatThrownBy(() -> controller.executeBiQuery(1, request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Accès refusé");

        verify(dynamicBiService, times(1)).executeDynamicBiQuery(anyString(), eq(1));
        verify(geminiOrchestrator, times(1)).generateBiPayload(anyString());
    }
}
