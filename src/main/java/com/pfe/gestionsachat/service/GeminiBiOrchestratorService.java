package com.pfe.gestionsachat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiBiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(GeminiBiOrchestratorService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    public GeminiBiOrchestratorService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    private static final String SYSTEM_PROMPT = """
        Tu es un moteur SQL BI Expert pour une application PostgreSQL d'un ERP "Procurement Management" (Gestion des Achats).
        Ta seule et unique tâche est de traduire la demande en langage naturel de l'utilisateur en une requête SQL native PostgreSQL optimisée, accompagnée d'une configuration de visualisation graphique.

        === RÈGLES DE SÉCURITÉ ET D'ARCHITECTURE OBLIGATOIRES ===
        1. FORMAT DE SORTIE : Tu dois retourner UNIQUEMENT un objet JSON brut et valide.
           N'inclus JAMAIS de balises Markdown (```json), ni aucun texte avant ou après l'objet JSON.
        2. REQUÊTE STRICTEMENT SELECT : La requête SQL DOIT être un SELECT sans point-virgule final.
           N'utilise jamais INSERT, UPDATE, DELETE, DROP, TRUNCATE, ALTER, MERGE, EXEC.
        3. SCHÉMA JSON REQUIS :
        {
          "sql": "<requête SELECT valide pour PostgreSQL, sans point-virgule>",
          "visualizations": [
            {
              "type": "<BarChart | LineChart | PieChart>",
              "title": "<Titre du graphique>",
              "xKey": "<alias_exact_colonne_categorielle>",
              "yKey": "<alias_exact_colonne_numerique>"
            }
          ]
        }
        4. CONTRAINTE RECHARTS : Les valeurs de `xKey` et `yKey` doivent correspondre STRICTEMENT aux noms ou alias
           des colonnes retournées par ton SQL (respect strict de la casse). Toute divergence génère un graphique vide.

        === DICTIONNAIRE DE DONNÉES STRICT (SOURCE DE VÉRITÉ VALIDÉE) ===
        N'invente JAMAIS de tables ou de colonnes. Utilise EXCLUSIVEMENT les 7 tables ci-dessous :

        - users             (oid_user PK, nom, email, role, service, actif)
        - family            (id_family PK, categorie, libelle, budget_initial, budget_engage, budget_restant)
        - sub_family        (oid_sub PK, libelle, id_family FK→family.id_family, budget_initial, budget_engage, budget_restant)
        - demande_achat_interne (id PK, demandeur_id FK→users.oid_user, budget_famille_id FK→family.id_family, budget_sous_famille_id FK→sub_family.oid_sub, statut, montant_estime, quantite, is_piece_rechange)
        - purchase_order    (id PK, id_demande_interne FK→demande_achat_interne.id, po_number, montant_total, status)
        - transfer_header   (id PK, requested_by FK→users.oid_user, from_warehouse, to_warehouse, status)
        - demande_ajustement (id PK, demande_interne FK→demande_achat_interne.id, niveau, statut)

        5. CLASSIFICATION MÉTIER DES ACHATS : 
           Les achats sont catégorisés via `family.categorie` (ex: 'INFORMATIQUE', 'BUREAUTIQUE', 'AUTRE') 
           ou via le libellé spécifique de la famille (`family.libelle`) ou sous-famille (`sub_family.libelle`).

        === EXEMPLES DE QUESTIONS ET DE RETOUR ===
        Question : "Montre-moi le total des montants estimés des demandes d'achat par famille."
        Réponse attendue :
        {
          "sql": "SELECT f.categorie AS famille, SUM(d.montant_estime) AS total_montant FROM demande_achat_interne d JOIN family f ON d.budget_famille_id = f.id_family GROUP BY f.categorie",
          "visualizations": [
            {
              "type": "BarChart",
              "title": "Montant estimé des DA par famille",
              "xKey": "famille",
              "yKey": "total_montant"
            }
          ]
        }

        Question : "Nombre de demandes par statut."
        Réponse attendue :
        {
          "sql": "SELECT statut, COUNT(*) AS nombre_demandes FROM demande_achat_interne GROUP BY statut",
          "visualizations": [
            {
              "type": "PieChart",
              "title": "Répartition des demandes d'achat par statut",
              "xKey": "statut",
              "yKey": "nombre_demandes"
            }
          ]
        }

        Compris ? Agis maintenant comme le moteur SQL BI et réponds à la demande de l'utilisateur.
        """;


    public String generateBiPayload(String userPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Payload structure pour l'API Gemini
            Map<String, Object> requestBody = new HashMap<>();
            
            // Instruction Système
            Map<String, Object> systemInstruction = new HashMap<>();
            Map<String, Object> sysPart = new HashMap<>();
            sysPart.put("text", SYSTEM_PROMPT);
            systemInstruction.put("parts", List.of(sysPart));
            requestBody.put("systemInstruction", systemInstruction);

            // Prompt de l'utilisateur
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> userPart = new HashMap<>();
            userPart.put("text", userPrompt);
            contents.put("parts", List.of(userPart));
            requestBody.put("contents", List.of(contents));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = apiUrl + "?key=" + apiKey;
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode candidates = rootNode.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode content = candidates.get(0).path("content");
                    JsonNode parts = content.path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        String generatedText = parts.get(0).path("text").asText();
                        return cleanMarkdownBlocks(generatedText);
                    }
                }
            }
            throw new RuntimeException("Réponse invalide ou vide de Gemini API");
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur de génération IA", e);
        }
    }

    /**
     * Extraction JSON robuste par isolation des délimiteurs structurels.
     *
     * Stratégie : indexOf("{") + lastIndexOf("}") délimite le premier objet JSON
     * valide dans la chaîne, quel que soit le texte verbeux produit par le LLM
     * avant ou après (ex: "Voici le résultat :\n```json\n{...}\n```\nBonne analyse !").
     *
     * Validation finale via objectMapper.readTree() : si l'extraction produit un
     * fragment JSON malformé (ex: guillemet imbriqué tronqué), l'exception remonte
     * immédiatement avec un message explicite plutôt que de propager un JSON corrompu.
     */
    private String cleanMarkdownBlocks(String text) {
        if (text == null || text.isBlank()) return "";

        int firstBrace = text.indexOf('{');
        int lastBrace  = text.lastIndexOf('}');

        if (firstBrace == -1 || lastBrace == -1 || firstBrace >= lastBrace) {
            log.error("Réponse Gemini ne contient aucun objet JSON détectable. Texte reçu : {}", text);
            throw new IllegalStateException(
                    "La réponse de l'IA ne contient pas de JSON structuré valide. " +
                    "Vérifiez le SYSTEM_PROMPT ou réessayez.");
        }

        String extracted = text.substring(firstBrace, lastBrace + 1);

        // Validation structurelle : lève une exception si le JSON est malformé
        try {
            objectMapper.readTree(extracted);
        } catch (Exception parseEx) {
            log.error("JSON extrait de Gemini est malformé : {} | Texte original : {}", extracted, text);
            throw new IllegalStateException(
                    "Le JSON extrait de la réponse IA est malformé : " + parseEx.getMessage(), parseEx);
        }

        log.info("JSON généré par Gemini : {}", extracted);
        return extracted;
    }
}