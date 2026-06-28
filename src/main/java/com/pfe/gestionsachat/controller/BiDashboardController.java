package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.service.DynamicBiService;
import com.pfe.gestionsachat.service.GeminiBiOrchestratorService;
import com.pfe.gestionsachat.service.OverviewBiService;
import com.pfe.gestionsachat.exception.BiSqlException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bi")
public class BiDashboardController {

    private final DynamicBiService dynamicBiService;
    private final GeminiBiOrchestratorService geminiOrchestrator;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final OverviewBiService overviewBiService;

    @Autowired
    public BiDashboardController(DynamicBiService dynamicBiService, GeminiBiOrchestratorService geminiOrchestrator, com.fasterxml.jackson.databind.ObjectMapper objectMapper, OverviewBiService overviewBiService) {
        this.dynamicBiService = dynamicBiService;
        this.geminiOrchestrator = geminiOrchestrator;
        this.objectMapper = objectMapper;
        this.overviewBiService = overviewBiService;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','DAF','DG','ACHETEUR','MANAGER_N1','DEMANDEUR')")
    @GetMapping("/overview")
    public ResponseEntity<OverviewBiService.BiOverviewDto> getBiOverview() {
        return ResponseEntity.ok(overviewBiService.getOverview());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','DAF','DG','ACHETEUR','MANAGER_N1','DEMANDEUR')")
    @PostMapping("/query")
    public ResponseEntity<BiResponseDto> executeBiQuery(
            @RequestParam @org.springframework.lang.NonNull Integer userId,
            @RequestBody @org.springframework.lang.NonNull BiQueryRequest request) {
        
        String currentQuestion = request.getUserQuestion();

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                // 1. L'IA génère un payload structuré (Generative UI)
                String jsonPayload = geminiOrchestrator.generateBiPayload(currentQuestion);
                com.fasterxml.jackson.databind.JsonNode rootNode;
                try {
                    rootNode = objectMapper.readTree(jsonPayload);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new IllegalArgumentException("La réponse de l'IA est malformée.");
                }
                
                String sql = rootNode.path("sql").asText("");
                if (sql.isBlank()) {
                    throw new IllegalStateException("La requête SQL générée par l'IA est vide.");
                }

                // 2. Le moteur sécurisé exécute le SQL et mappe les données
                List<Map<String, Object>> data = dynamicBiService.executeDynamicBiQuery(sql, userId);
                
                // 3. Extraction des métadonnées visuelles
                List<VisualizationConfig> visualizations = new java.util.ArrayList<>();
                com.fasterxml.jackson.databind.JsonNode vizNode = rootNode.path("visualizations");
                if (vizNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode v : vizNode) {
                        VisualizationConfig config = new VisualizationConfig();
                        config.setType(v.path("type").asText(null));
                        config.setTitle(v.path("title").asText(null));
                        config.setxKey(v.path("xKey").asText(null));
                        config.setyKey(v.path("yKey").asText(null));
                        visualizations.add(config);
                    }
                }
                
                BiResponseDto response = new BiResponseDto();
                response.setData(data);
                response.setVisualizations(visualizations);
                
                return ResponseEntity.ok(response);

            } catch (BiSqlException e) {
                if (attempt < 3) {
                    currentQuestion = request.getUserQuestion() +
                            "\n\nLa requête SQL précédente a échoué avec l'erreur PostgreSQL suivante : " + e.getMessage() +
                            "\nSQL fautif : " + e.getSqlFautif() +
                            "\nCorrige le SQL pour répondre à la question initiale en respectant strictement les règles et le dictionnaire de données.";
                } else {
                    throw new IllegalArgumentException("Échec après 3 tentatives : " + e.getMessage());
                }
            }
        }
        
        throw new IllegalArgumentException("Échec après 3 tentatives.");
    }

    public static class BiQueryRequest {
        private String userQuestion;
        public String getUserQuestion() { return userQuestion; }
        public void setUserQuestion(String userQuestion) { this.userQuestion = userQuestion; }
    }

    public static class BiResponseDto {
        private List<Map<String, Object>> data;
        private List<VisualizationConfig> visualizations;

        public List<Map<String, Object>> getData() { return data; }
        public void setData(List<Map<String, Object>> data) { this.data = data; }

        public List<VisualizationConfig> getVisualizations() { return visualizations; }
        public void setVisualizations(List<VisualizationConfig> visualizations) { this.visualizations = visualizations; }
    }

    public static class VisualizationConfig {
        private String type;
        private String title;
        private String xKey;
        private String yKey;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getxKey() { return xKey; }
        public void setxKey(String xKey) { this.xKey = xKey; }

        public String getyKey() { return yKey; }
        public void setyKey(String yKey) { this.yKey = yKey; }
    }
}
