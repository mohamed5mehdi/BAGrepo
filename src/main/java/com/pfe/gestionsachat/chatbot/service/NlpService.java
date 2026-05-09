package com.pfe.gestionsachat.chatbot.service;

import com.pfe.gestionsachat.chatbot.model.SlotState;
import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.model.UrgenceDemande;
import com.pfe.gestionsachat.repository.FamilyRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service NLP — extraction déterministe de slots à partir de messages en français.
 * Aucune dépendance externe : règles manuelles + regex + fuzzy matching sur libellés DB.
 */
@Service
public class NlpService {

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private SubFamilyRepository subFamilyRepository;

    // ─── Stopwords français pour extractDesignation ───────────────────────────
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "je", "veux", "voudrais", "besoin", "commander", "acheter", "avoir",
            "pour", "le", "la", "les", "un", "une", "des", "du", "de", "et",
            "avec", "svp", "stp", "merci", "urgent", "urgente", "urgence",
            "normale", "critique", "ordinateur"
    ));

    // ─── Mots-clés métier → libellé Family ────────────────────────────────────
    private static final Set<String> KW_INFORMATIQUE = new HashSet<>(Arrays.asList(
            "pc", "laptop", "portable", "écran", "ecran", "souris", "clavier",
            "imprimante", "serveur", "stockage", "disque", "câble", "cable",
            "usb", "hdmi"
    ));
    private static final Set<String> KW_LOGICIELS = new HashSet<>(Arrays.asList(
            "logiciel", "software", "licence", "license", "microsoft", "office",
            "azure", "aws", "cloud", "abonnement", "m365", "windows"
    ));
    private static final Set<String> KW_BUREAUTIQUE = new HashSet<>(Arrays.asList(
            "bureau", "chaise", "table", "mobilier", "meuble", "armoire",
            "clim", "climatisation", "climatiseur", "rideau", "store"
    ));
    private static final Set<String> KW_FOURNITURES = new HashSet<>(Arrays.asList(
            "papier", "stylo", "crayon", "fourniture", "traiteur", "café", "cafe",
            "eau", "nettoyage", "hygiène", "hygiene", "consommable"
    ));

    // ─── MÉTHODE 1 : extractQuantite ─────────────────────────────────────────

    /**
     * Extrait une quantité depuis le message.
     * Priorité : mots → chiffres → "quelques" → null.
     */
    public Integer extractQuantite(String message) {
        if (message == null || message.isBlank()) return null;
        String lower = message.toLowerCase();

        // 1. Mots écrits (ordre du plus spécifique au plus général)
        if (lower.contains("un seul"))  return 1;
        if (lower.contains("une"))      return 1;
        if (lower.contains("vingt"))    return 20;
        if (lower.contains("cent"))     return 100;
        if (lower.contains("dix"))      return 10;
        if (lower.contains("cinq"))     return 5;
        if (lower.contains("quatre"))   return 4;
        if (lower.contains("trois"))    return 3;
        if (lower.contains("deux"))     return 2;
        // "un" seul (pas "une", pas "une seule")
        if (lower.matches(".*\\bun\\b.*")) return 1;

        // 2. Regex chiffres — premier match
        Matcher m = Pattern.compile("(\\d+)").matcher(message);
        if (m.find()) {
            try {
                Integer quantite = Integer.valueOf(m.group(1));
                if (quantite != null && quantite <= 0) {
                    return null; // Force une nouvelle question
                }
                return quantite;
            } catch (NumberFormatException e) {
                return null; // Ignore les nombres trop grands
            }
        }

        // 3. "quelques" → 3
        if (lower.contains("quelques")) return 3;

        return null;
    }

    // ─── MÉTHODE 2 : extractDesignation ──────────────────────────────────────

    /**
     * Extrait la désignation en supprimant stopwords et chiffres,
     * puis capitalise la première lettre.
     */
    public String extractDesignation(String message) {
        if (message == null || message.isBlank()) return null;

        // Supprimer les chiffres puis tokeniser
        String cleaned = message.replaceAll("\\d+", " ");
        String[] tokens = cleaned.toLowerCase().split("[\\s,;.!?]+");

        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!token.isBlank() && !STOPWORDS.contains(token)) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(token);
            }
        }

        String result = sb.toString().trim();
        if (result.isEmpty()) return null;

        // Capitaliser la première lettre
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    // ─── MÉTHODE 3 : extractUrgence ──────────────────────────────────────────

    /**
     * Extrait le niveau d'urgence.
     * Normalisation Unicode pour gérer les accents ("pressé" → "presse").
     */
    public UrgenceDemande extractUrgence(String message) {
        if (message == null || message.isBlank()) return null;
        String normalized = normalize(message);

        // Détecter la négation AVANT tout matching
        boolean negation = normalized.matches(".*(pas|non|aucune?|sans)\\s+(urgence|urgent|critique).*");

        if (negation) return UrgenceDemande.NORMALE;

        if (normalized.contains("critique") || normalized.contains("tres urgent") || normalized.contains("très urgent")) {
            return UrgenceDemande.CRITIQUE;
        }
        if (normalized.contains("urgent") || normalized.contains("urgente") || normalized.contains("rapidement") || normalized.contains("des que possible") || normalized.contains("vite")) {
            return UrgenceDemande.URGENTE;
        }
        if (normalized.contains("normal") || normalized.contains("normale") || normalized.contains("pas presse") || normalized.contains("pas pressé")) {
            return UrgenceDemande.NORMALE;
        }
        return null;
    }

    // ─── MÉTHODE 3bis : extractJustification ────────────────────────────────

    /**
     * Capture la justification : tout message qui n'est pas un slot technique
     * et qui dépasse un seuil minimal de longueur (> 10 chars).
     * Heuristique : si le message contient des mots de justification OU
     * qu'aucun autre slot n'est détectable, on l'utilise comme justification.
     */
    public String extractJustification(String message, String expectedSlot) {
        if (message == null || message.isBlank()) return null;
        String lower = normalize(message);

        // Si le chatbot a explicitement demandé la justification, 
        // on l'accepte s'il est assez long, sauf s'il s'agit juste d'un niveau d'urgence.
        if ("JUSTIFICATION".equals(expectedSlot) && message.trim().length() > 5) {
            if (extractUrgence(message) == null || message.trim().length() > 15) {
                return message.trim();
            }
        }

        // Indicateurs explicites de justification
        boolean hasJustifKeyword = lower.contains("car ")
                || lower.contains("parce que")
                || lower.contains("afin de")
                || (lower.contains("pour ") && message.length() > 15)  // parenthèse explicite — précédence && > ||
                || lower.contains("besoin de")
                || lower.contains("remplacement")
                || lower.contains("renouvellement")
                || lower.contains("projet")
                || lower.contains("formation")
                || lower.contains("reunion")
                || lower.contains("maintenance")
                || lower.contains("panne")
                || lower.contains("obsolete")
                || lower.contains("manque");

        // Aucun slot technique détectable ET message suffisamment long → justification
        boolean isNotSlot = resolveFamily(message) == null && extractUrgence(message) == null;

        if ((hasJustifKeyword || isNotSlot) && message.trim().length() > 10) {
            return message.trim();
        }
        return null;
    }

    // ─── MÉTHODE 4 : resolveFamily ───────────────────────────────────────────

    /**
     * Résout la famille en 2 niveaux :
     * 1. Map statique de mots-clés métier
     * 2. Fuzzy matching sur libellés DB
     */
    public Family resolveFamily(String message) {
        if (message == null || message.isBlank()) return null;
        String lower = message.toLowerCase();
        String[] words = lower.split("[\\s,;.!?]+");

        // Niveau 1 — map statique
        String targetLibelle = null;
        for (String word : words) {
            if (KW_INFORMATIQUE.contains(word)) { targetLibelle = "Informatique"; break; }
            if (KW_LOGICIELS.contains(word))    { targetLibelle = "Logiciels"; break; }
            if (KW_BUREAUTIQUE.contains(word))  { targetLibelle = "Bureautique"; break; }
            if (KW_FOURNITURES.contains(word))  { targetLibelle = "Fournitures"; break; }
        }

        if (targetLibelle != null) {
            final String kw = targetLibelle;
            List<Family> all = familyRepository.findAll();
            Optional<Family> found = all.stream()
                    .filter(f -> f.getLibelle() != null && f.getLibelle().toLowerCase().contains(kw.toLowerCase()))
                    .findFirst();
            if (found.isPresent()) return found.get();
        }

        // Niveau 2 — fuzzy matching sur libellés DB
        List<Family> allFamilies = familyRepository.findAll();
        for (Family family : allFamilies) {
            if (family.getLibelle() == null) continue;
            String[] libWords = family.getLibelle().toLowerCase().split("[\\s&]+");
            for (String lw : libWords) {
                if (!lw.isBlank() && lower.contains(lw)) {
                    return family;
                }
            }
        }

        return null;
    }

    // ─── MÉTHODE 5 : resolveSubFamily ────────────────────────────────────────

    /**
     * Résout la sous-famille en filtrant par familyId.
     * Si une seule sous-famille existe → la retourner directement.
     * Sinon fuzzy matching sur libellés.
     */
    public SubFamily resolveSubFamily(String message, Integer familyId) {
        if (message == null || message.isBlank() || familyId == null) return null;
        String lower = message.toLowerCase();

        List<SubFamily> candidates = subFamilyRepository.findByFamilyId(familyId);
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        // Fuzzy matching
        for (SubFamily sf : candidates) {
            if (sf.getLibelle() == null) continue;
            String[] libWords = sf.getLibelle().toLowerCase().split("[\\s&/]+");
            for (String lw : libWords) {
                if (!lw.isBlank() && lower.contains(lw)) {
                    return sf;
                }
            }
        }

        return null;
    }

    // ─── MÉTHODE 6 : extractSlots ────────────────────────────────────────────

    public SlotState extractSlots(String message, SlotState current) {
        // Cloner current
        SlotState updated = cloneSlotState(current);
        String expectedSlot = updated.getProchainSlotManquant();

        if (updated.getQuantite() == null) {
            Integer q = extractQuantite(message);
            if (q != null) updated.setQuantite(q);
        }

        if (updated.getDesignation() == null) {
            String d = extractDesignation(message);
            if (d != null) updated.setDesignation(d);
        }

        if (updated.getUrgence() == null) {
            UrgenceDemande u = extractUrgence(message);
            if (u != null) updated.setUrgence(u);
        }

        if (updated.getFamilyId() == null) {
            Family fam = resolveFamily(message);
            if (fam != null) {
                updated.setFamilyId(fam.getIdFamily());
                updated.setFamilyLibelle(fam.getLibelle());
            }
        }

        if (updated.getSubFamilyId() == null && updated.getFamilyId() != null) {
            SubFamily sf = resolveSubFamily(message, updated.getFamilyId());
            if (sf != null) {
                updated.setSubFamilyId(sf.getOidSub());
                updated.setSubFamilyLibelle(sf.getLibelle());
            }
        }

        // Justification — après les slots techniques pour éviter les faux positifs
        if (updated.getJustification() == null) {
            String j = extractJustification(message, expectedSlot);
            if (j != null) updated.setJustification(j);
        }

        return updated;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Normalise les accents Unicode : "pressé" → "presse", "annulé" → "annule".
     * Indispensable pour les comparaisons robustes en français.
     */
    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private SlotState cloneSlotState(SlotState src) {
        SlotState clone = new SlotState();
        clone.setDesignation(src.getDesignation());
        clone.setQuantite(src.getQuantite());
        clone.setJustification(src.getJustification());
        clone.setUrgence(src.getUrgence());
        clone.setFamilyId(src.getFamilyId());
        clone.setSubFamilyId(src.getSubFamilyId());
        clone.setFamilyLibelle(src.getFamilyLibelle());
        clone.setSubFamilyLibelle(src.getSubFamilyLibelle());
        return clone;
    }
}
