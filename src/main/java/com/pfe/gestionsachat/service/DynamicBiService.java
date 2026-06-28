package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.exception.BiSqlException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DynamicBiService {

    private static final Logger log = LoggerFactory.getLogger(DynamicBiService.class);

    /**
     * Pattern de détection des verbes DML dangereux.
     * Couvre les injections via CTE mutants : WITH x AS (DELETE ...) SELECT ...
     * Utilise \\b (word boundary) pour éviter les faux positifs (ex: colonne "inserted_at").
     */
    private static final Pattern DML_INJECTION_PATTERN = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|MERGE|EXEC|EXECUTE)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final EntityManager entityManager;
    private final UserRepository userRepository;

    @Autowired
    public DynamicBiService(EntityManager entityManager, UserRepository userRepository) {
        this.entityManager = entityManager;
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> executeDynamicBiQuery(String llmSql, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("Utilisateur non trouvé"));

        String safeSql = llmSql.trim();

        // ── Règle 1 : Seuls SELECT racine et CTEs (WITH ... SELECT) sont autorisés ──────────
        String upperSql = safeSql.toUpperCase();
        boolean isSelect = upperSql.startsWith("SELECT ");
        boolean isCte    = upperSql.startsWith("WITH ");
        if (!isSelect && !isCte) {
            throw new SecurityException(
                    "Violation de sécurité : La requête doit être un SELECT ou un CTE (WITH ... SELECT).");
        }

        // ── Règle 2 : Détection d'injection DML dans les CTEs mutants ─────────────────────
        // Ex: WITH x AS (DELETE FROM users RETURNING *) SELECT * FROM x
        if (DML_INJECTION_PATTERN.matcher(safeSql).find()) {
            throw new SecurityException(
                    "Violation de sécurité : Verbe DML détecté dans la requête (INSERT/UPDATE/DELETE/DROP…).");
        }

        // ── Règle 3 : Interdiction des requêtes empilées ───────────────────────────────────
        if (safeSql.contains(";")) {
            throw new SecurityException("Violation de sécurité : Les requêtes empilées (;) sont interdites.");
        }

        String finalSql = "SELECT * FROM (" + safeSql + ") AS llm_result";

        Query query = entityManager.createNativeQuery(finalSql, Tuple.class);

        // ── Exécution avec gestion gracieuse de l'absence de colonne de sécurité ─────────
        // Si l'IA omet la colonne `departement`, PostgreSQL lève une PSQLException
        // ("column llm_result.departement does not exist") wrappée par Spring JPA
        // dans une InvalidDataAccessResourceUsageException (sous-type de DataAccessException).
        // On la transforme en IllegalArgumentException (HTTP 400 via GlobalExceptionHandler)
        // pour éviter un crash HTTP 500 opaque et informer le frontend précisément.
        @SuppressWarnings("unchecked")
        List<Tuple> results;
        try {
            results = query.getResultList();
        } catch (DataAccessException dae) {
            // Couche Spring JPA : InvalidDataAccessResourceUsageException, etc.
            log.error("Échec SQL RLS [DataAccessException] — colonne de sécurité absente ou SQL invalide. SQL : {}", finalSql, dae);
            throw new BiSqlException(
                    "L'IA n'a pas inclus la colonne de sécurité requise. " +
                    "Reformulez votre question ou contactez l'administrateur.", finalSql, dae);
        } catch (PersistenceException pe) {
            // Couche JPA/Hibernate : levée directement pour les requêtes natives mal formées
            // (ex: colonne inconnue, type incompatible). Non wrappée par Spring dans certains contextes.
            log.error("Échec SQL RLS [PersistenceException] — requête native rejetée par Hibernate. SQL : {}", finalSql, pe);
            throw new BiSqlException(
                    "La requête SQL générée par l'IA est invalide (colonne inconnue ou type incompatible). " +
                    "Reformulez votre question.", finalSql, pe);
        } catch (Exception ex) {
            // Filet global : capture toute exception résiduelle non prévue
            // pour éviter un crash HTTP 500 opaque vers le frontend React.
            log.error("Échec SQL BI inattendu [Exception] — SQL : {} | Cause : {}", finalSql, ex.getMessage(), ex);
            throw new IllegalArgumentException(
                    "Une erreur inattendue s'est produite lors de l'exécution de la requête BI. " +
                    "Reformulez votre question ou contactez l'administrateur.");
        }
        return mapToGenericJsonContrat(results);
    }

    private List<Map<String, Object>> mapToGenericJsonContrat(List<Tuple> rows) {
        List<Map<String, Object>> jsonResult = new ArrayList<>();

        for (Tuple tuple : rows) {
            Map<String, Object> map = new HashMap<>();
            
            for (TupleElement<?> element : tuple.getElements()) {
                String alias = element.getAlias();
                Object value = tuple.get(element);

                if (value instanceof Number) {
                    map.put(alias, ((Number) value).doubleValue());
                } else {
                    map.put(alias, value != null ? value.toString() : null);
                }
            }
            jsonResult.add(map);
        }
        return jsonResult;
    }
}
