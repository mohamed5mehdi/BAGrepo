package com.pfe.gestionsachat.config;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AIDataSeeder — Ordre 2, s'exécute après DataInitializer.
 * Génère 300 DA avec :
 * - montantEstime sur toutes les DAs (fix risque #5)
 * - dateCreation étalée sur 6 mois (fix risque #4)
 * - dateValidation renseignée (fix risque #3)
 * - 6 départements différents (fix risque #6)
 * - budgetRestant décrémenté sur les DAs APPROUVEE/PO_CREE (fix risque #2)
 * - StatusHistory persisté (fix risque #7 — ajout index via query nommée)
 * - Family rechargée avant chaque deductBudget (fix risque #8 — @Version safe)
 */
@Component
@Order(2)
public class AIDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AIDataSeeder.class);

    @Autowired
    private DemandeAchatInterneRepository daRepo;
    @Autowired
    private FamilyRepository familyRepo;
    @Autowired
    private SubFamilyRepository subFamilyRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private StatusHistoryRepository statusHistoryRepo;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ─── Données de référence métier BAG ────────────────────────────────────

    private static final String[] DEPARTEMENTS = {
            "INFORMATIQUE", "LOGISTIQUE BAG", "TECHNIQUE & MAINTENANCE",
            "COMMERCIAL & VENTES", "RESSOURCES HUMAINES", "DIRECTION GENERALE"
    };

    // designation → [prixMin, prixMax] en MAD
    private static final Object[][] CATALOGUE_IT = {
            { "Laptop Dell Latitude 5540", 12000, 18000 },
            { "Laptop HP EliteBook 840 G10", 14000, 20000 },
            { "MacBook Pro M3 14 pouces", 24000, 32000 },
            { "Écran Dell 27\" 4K USB-C", 3500, 5500 },
            { "Écran LG UltraWide 34\"", 4200, 6800 },
            { "Imprimante HP LaserJet Pro M404", 3800, 6000 },
            { "Imprimante multifonction Canon", 4500, 7500 },
            { "Switch Cisco Catalyst 2960-X", 9000, 16000 },
            { "Routeur FortiGate 60F", 12000, 22000 },
            { "NAS Synology DS923+", 8500, 14000 },
            { "UPS APC Smart-UPS 1500VA", 4200, 7000 },
            { "Scanner Fujitsu fi-7300NX", 5500, 9000 },
            { "Webcam Logitech 4K Pro", 900, 1800 },
            { "Clé USB 128Go Kingston", 150, 400 },
            { "Disque externe SSD 2To", 1200, 2500 },
    };

    private static final Object[][] CATALOGUE_SOFT = {
            { "Licences Microsoft 365 E3 (x10)", 4500, 9000 },
            { "Abonnement Azure DevOps (annuel)", 6000, 12000 },
            { "Licence AutoCAD LT 2025", 8000, 15000 },
            { "Suite Adobe Creative Cloud (x5)", 7500, 14000 },
            { "Logiciel DMS Automotive BAG", 20000, 45000 },
            { "Antivirus Kaspersky Business (x50)", 3500, 6000 },
            { "Licence Sage 100 Compta", 12000, 25000 },
            { "Abonnement Zoom Business (annuel)", 2400, 4800 },
    };

    private static final Object[][] CATALOGUE_BUR = {
            { "Bureau ergonomique réglable", 3500, 6500 },
            { "Chaise ergonomique Herman Miller", 4500, 9000 },
            { "Armoire sécurisée 4 tiroirs", 2200, 4500 },
            { "Tableau blanc magnétique 180x120", 1200, 2800 },
            { "Vidéoprojecteur Epson EB-L200F", 9000, 16000 },
            { "Climatiseur Split Daikin 18000BTU", 8500, 14000 },
            { "Meuble rangement open-space", 2800, 5200 },
            { "Téléphone IP Cisco 8841", 1800, 3200 },
    };

    private static final Object[][] CATALOGUE_DIV = {
            { "Ramettes papier A4 80g (carton)", 180, 350 },
            { "Fournitures bureau (lot mensuel)", 800, 1800 },
            { "Service nettoyage mensuel", 3500, 8000 },
            { "Café & consommables (mensuel)", 600, 1200 },
            { "Fournitures sanitaires", 400, 900 },
            { "Service maintenance photocopieur", 1500, 3500 },
            { "Prestation formation interne", 5000, 18000 },
            { "Traiteur réunion direction", 2000, 6000 },
    };

    // Délais de validation réalistes par rôle (en heures)
    private static final Map<String, int[]> DELAIS_PAR_ROLE = new LinkedHashMap<>();
    static {
        DELAIS_PAR_ROLE.put("MANAGER_N1", new int[] { 4, 24 });
        DELAIS_PAR_ROLE.put("TECHNICIEN", new int[] { 8, 48 });
        DELAIS_PAR_ROLE.put("AMG", new int[] { 12, 72 });
        DELAIS_PAR_ROLE.put("DAF", new int[] { 24, 120 });
        DELAIS_PAR_ROLE.put("DG", new int[] { 48, 168 });
    }

    private final Random rnd = new Random(42); // seed fixe → résultats reproductibles

    // ─── Entry point ─────────────────────────────────────────────────────────

    @Override
    public void run(String... args) {
        // ─── ÉTAPE 1 : RÉPARATION SQL NATIVE (AVANT TOUTE OPÉRATION JPA) ────────────────
        log.info("🛡️  AIDataSeeder : Migration SQL native pour intégrité des Enums...");
        try {
            // Désactivation des contraintes de vérification pour permettre la migration
            jdbcTemplate.execute("ALTER TABLE demande_achat_interne DROP CONSTRAINT IF EXISTS demande_achat_interne_statut_check");
            jdbcTemplate.execute("ALTER TABLE da_header DROP CONSTRAINT IF EXISTS da_header_statut_check");

            // Migration des anciens statuts (nomenclature historique BAG)
            String[] tables = {"demande_achat_interne", "da_header"};
            for (String table : tables) {
                jdbcTemplate.update("UPDATE " + table + " SET statut = 'VALIDE_N1' WHERE statut = 'VALIDEE_N1'");
                jdbcTemplate.update("UPDATE " + table + " SET statut = 'VALIDE_TECH' WHERE statut = 'VALIDEE_TECH'");
                jdbcTemplate.update("UPDATE " + table + " SET statut = 'VALIDE_AMG' WHERE statut = 'EN_VALIDATION_AMG'");
                jdbcTemplate.update("UPDATE " + table + " SET statut = 'VALIDE_DAF' WHERE statut = 'EN_VALIDATION_DAF'");
                jdbcTemplate.update("UPDATE " + table + " SET statut = 'VALIDE_DG' WHERE statut = 'EN_VALIDATION_DG'");
            }
            
            // Nettoyage de l'historique (Utilisation des noms de colonnes SQL générés par JPA : statut_avant / statut_apres)
            jdbcTemplate.update("UPDATE status_history SET statut_avant = 'VALIDE_N1' WHERE statut_avant = 'VALIDEE_N1'");
            jdbcTemplate.update("UPDATE status_history SET statut_apres = 'VALIDE_N1' WHERE statut_apres = 'VALIDEE_N1'");
            
            log.info("✅ AIDataSeeder : Migration SQL terminée avec succès.");
        } catch (Exception e) {
            log.warn("⚠️  AIDataSeeder : Erreur lors de la migration SQL (peut être ignoré si déjà fait) : {}", e.getMessage());
        }

        // ─── ÉTAPE 2 : RÉPARATION DES RELATIONS ORPHELINES ──────────────────────
        User demandeurFix = userRepo.findByEmail("demandeur@test.com").orElse(null);
        if (demandeurFix != null) {
            log.info("🔍 [Reparation] Vérification des DAs orphelines pour ID : {}", demandeurFix.getOidUser());
            // Utilisation d'une requête native pour éviter le crash Hibernate si un enum est encore invalide
            List<Long> idsToFix = jdbcTemplate.queryForList(
                "SELECT id FROM demande_achat_interne WHERE demandeur_id IS NULL OR demandeur_id != ?", 
                Long.class, demandeurFix.getOidUser());
            
            if (!idsToFix.isEmpty()) {
                jdbcTemplate.update("UPDATE demande_achat_interne SET demandeur_id = ? WHERE id IN (" + 
                    String.join(",", idsToFix.stream().map(Object::toString).toList()) + ")", 
                    demandeurFix.getOidUser());
                log.info("✅ [Reparation] {} DAs réattribuées à l'ID {}", idsToFix.size(), demandeurFix.getOidUser());
            }
        }
        // ------------------------------

        if (daRepo.count() >= 10) {
            log.info("ℹ️  AIDataSeeder : {} DAs présentes — skip.", daRepo.count());
            return;
        }
        log.info("🤖 AIDataSeeder : démarrage du seeding IA (300 DAs)...");
        seedDemandes();
        log.info("✅ AIDataSeeder : terminé. {} DAs en base.", daRepo.count());
    }

    // ─── Seeding principal ────────────────────────────────────────────────────

    @Transactional
    public void seedDemandes() {
        // Charger les utilisateurs validateurs
        User n1 = userRepo.findByEmail("n1@test.com").orElse(null);
        User tech = userRepo.findByEmail("tech@test.com").orElse(null);
        User amg = userRepo.findByEmail("amg@test.com").orElse(null);
        User daf = userRepo.findByEmail("daf@test.com").orElse(null);
        User dg = userRepo.findByEmail("dg@test.com").orElse(null);
        User demandeur = userRepo.findByEmail("demandeur@test.com").orElse(null);

        if (demandeur == null || n1 == null) {
            log.warn("⚠️  AIDataSeeder : utilisateurs non trouvés — DataInitializer a-t-il tourné ?");
            return;
        }

        // Charger familles et sous-familles
        List<Family> families = familyRepo.findAll();
        List<SubFamily> allSubs = subFamilyRepo.findAll();

        if (families.isEmpty()) {
            log.warn("⚠️  AIDataSeeder : aucune famille trouvée.");
            return;
        }

        // Mapper sous-familles par famille pour accès rapide
        Map<Integer, List<SubFamily>> subsByFamily = new HashMap<>();
        for (SubFamily sf : allSubs) {
            subsByFamily.computeIfAbsent(sf.getFamily().getIdFamily(), k -> new ArrayList<>()).add(sf);
        }

        // Distribution des statuts sur 300 DAs
        // BROUILLON(15), SOUMISE(40), VALIDE_N1(30), VALIDE_TECH(25),
        // VALIDE_AMG(15), VALIDE_DAF(15), VALIDE_DG(10),
        // APPROUVEE(50), PO_CREE(50), REJETEE(20), AFFECTEE(30)
        List<StatutDemande> pool = new ArrayList<>();
        addToPool(pool, StatutDemande.BROUILLON, 15);
        addToPool(pool, StatutDemande.SOUMISE, 40);
        addToPool(pool, StatutDemande.VALIDE_N1, 30);
        addToPool(pool, StatutDemande.VALIDE_TECH, 25);
        addToPool(pool, StatutDemande.VALIDE_AMG, 15);
        addToPool(pool, StatutDemande.VALIDE_DAF, 15);
        addToPool(pool, StatutDemande.VALIDE_DG, 10);
        addToPool(pool, StatutDemande.APPROUVEE, 50);
        addToPool(pool, StatutDemande.PO_CREE, 45);
        addToPool(pool, StatutDemande.REJETEE, 20);
        addToPool(pool, StatutDemande.AFFECTEE, 35);
        Collections.shuffle(pool, rnd);

        List<DemandeAchatInterne> batch = new ArrayList<>();
        // Fix risque #1 : Map<daIndex, List<StatusHistory>> pour liaisons correctes
        // après save
        Map<Integer, List<StatusHistory>> historyByDaIndex = new HashMap<>();

        for (int i = 0; i < pool.size(); i++) {
            StatutDemande statut = pool.get(i);

            // Fix risque #4 : répartition temporelle sur 6 mois
            LocalDateTime dateCreation = LocalDateTime.now()
                    .minusMonths(rnd.nextInt(6))
                    .minusDays(rnd.nextInt(28))
                    .minusHours(rnd.nextInt(23));

            // Fix risque #6 : département varié
            String dept = DEPARTEMENTS[i % DEPARTEMENTS.length];

            // Sélectionner famille et sous-famille cohérentes
            Family famille = families.get(rnd.nextInt(families.size()));
            List<SubFamily> subs = subsByFamily.getOrDefault(famille.getIdFamily(), List.of());
            SubFamily sousFamille = subs.isEmpty() ? null : subs.get(rnd.nextInt(subs.size()));

            // Sélectionner catalogue selon famille
            Object[][] catalogue = cataloguePourFamille(famille);
            Object[] article = catalogue[rnd.nextInt(catalogue.length)];
            String designation = (String) article[0];
            int prixMin = (int) article[1];
            int prixMax = (int) article[2];

            int quantite = 1 + rnd.nextInt(20);
            // Fix risque #5 : montantEstime toujours renseigné
            BigDecimal prixUnit = BigDecimal.valueOf(prixMin + rnd.nextInt(prixMax - prixMin));
            BigDecimal montant = prixUnit.multiply(BigDecimal.valueOf(quantite))
                    .setScale(2, RoundingMode.HALF_UP);

            UrgenceDemande urgence = pickUrgence();
            String justification = pickJustification(designation, dept);

            // Construire la DA
            DemandeAchatInterne da = new DemandeAchatInterne();
            da.setDemandeur(demandeur);
            da.setDepartement(dept);
            da.setDesignation(designation);
            da.setQuantite(quantite);
            da.setMontantEstime(montant);
            da.setPrixUnitaire(prixUnit);
            da.setJustification(justification);
            da.setUrgence(urgence);
            da.setStatut(statut);
            da.setDateCreation(dateCreation);
            da.setBudgetFamille(famille);
            if (sousFamille != null)
                da.setBudgetSousFamille(sousFamille);

            // Fix risque #3 : dateValidation renseignée pour les statuts avancés
            if (isValidated(statut)) {
                int delaiHeures = 4 + rnd.nextInt(72);
                da.setDateValidation(dateCreation.plusHours(delaiHeures));
            }

            // Token unique pour idempotence (non null → pas de collision unique constraint)
            da.setSubmissionToken("SEED-" + UUID.randomUUID());

            batch.add(da);

            // Fix bug critique : conserver les entrées StatusHistory groupées par index DA
            historyByDaIndex.put(i, buildHistory(da, statut, dateCreation, n1, tech, amg, daf, dg, i));
        }

        // Persister en batch
        List<DemandeAchatInterne> saved = daRepo.saveAll(batch);
        log.info("   📋 {} DAs persistées.", saved.size());

        // Fix bug critique : lier entiteId après save en utilisant l'index DA correct
        List<StatusHistory> allHistory = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            Long daId = saved.get(i).getId();
            List<StatusHistory> entries = historyByDaIndex.getOrDefault(i, List.of());
            entries.forEach(h -> h.setEntiteId(daId));
            allHistory.addAll(entries);
        }
        statusHistoryRepo.saveAll(allHistory);
        log.info("   📜 {} entrées StatusHistory persistées.", allHistory.size());

        // Fix risque #2 + #8 : décrémenter budgetRestant famille (rechargement safe
        // @Version)
        decrementBudgets(saved);
    }

    // ─── Décrémentation budget (fix risques #2 et #8) ────────────────────────

    private void decrementBudgets(List<DemandeAchatInterne> savedDas) {
        // Agréger les montants par famille pour minimiser les save() et éviter
        // OptimisticLock
        Map<Integer, BigDecimal> consumptionByFamily = new HashMap<>();
        for (DemandeAchatInterne da : savedDas) {
            if (da.getBudgetFamille() == null || da.getMontantEstime() == null)
                continue;
            if (!isConsommeBudget(da.getStatut()))
                continue;
            Integer fid = da.getBudgetFamille().getIdFamily();
            consumptionByFamily.merge(fid, da.getMontantEstime(), BigDecimal::add);
        }

        // Une seule lecture + save par famille → pas de conflit @Version
        for (Map.Entry<Integer, BigDecimal> entry : consumptionByFamily.entrySet()) {
            // Fix risque #8 : rechargement depuis DB pour avoir la version courante
            Family f = familyRepo.findById(entry.getKey()).orElse(null);
            if (f == null)
                continue;
            BigDecimal toDeduct = entry.getValue();
            // Ne pas dépasser le budget initial (sécu analytique)
            if (f.getBudgetRestant() != null && toDeduct.compareTo(f.getBudgetRestant()) > 0) {
                toDeduct = f.getBudgetRestant().multiply(BigDecimal.valueOf(0.85));
            }
            f.deductBudget(toDeduct);
            familyRepo.save(f);
            log.info("   💰 Famille '{}' : -{} MAD engagés.", f.getLibelle(), toDeduct.toPlainString());
        }
    }

    // ─── Construction StatusHistory ───────────────────────────────────────────

    private List<StatusHistory> buildHistory(
            DemandeAchatInterne da,
            StatutDemande statut,
            LocalDateTime dateCreation,
            User n1, User tech, User amg, User daf, User dg,
            int idx) {

        List<StatusHistory> list = new ArrayList<>();
        LocalDateTime cursor = dateCreation;

        // SOUMISE toujours en premier
        list.add(hist("DemandeAchatInterne", null, "BROUILLON", "SOUMISE", da.getDemandeur(),
                cursor, "Demande soumise via chatbot"));

        if (statut == StatutDemande.SOUMISE)
            return list;

        cursor = cursor.plusHours(4 + rnd.nextInt(20));
        list.add(hist("DemandeAchatInterne", null, "SOUMISE", "VALIDE_N1", n1,
                cursor, "Validé par manager N1"));

        if (statut == StatutDemande.VALIDE_N1)
            return list;

        cursor = cursor.plusHours(8 + rnd.nextInt(40));
        list.add(hist("DemandeAchatInterne", null, "VALIDE_N1", "VALIDE_TECH", tech,
                cursor, "Validation technique confirmée"));

        if (statut == StatutDemande.VALIDE_TECH)
            return list;

        cursor = cursor.plusHours(12 + rnd.nextInt(48));
        list.add(hist("DemandeAchatInterne", null, "VALIDE_TECH", "VALIDE_AMG", amg,
                cursor, "Transmis au département AMG"));

        if (statut == StatutDemande.VALIDE_AMG)
            return list;

        cursor = cursor.plusHours(24 + rnd.nextInt(72));
        list.add(hist("DemandeAchatInterne", null, "VALIDE_AMG", "VALIDE_DAF", daf,
                cursor, "Analyse financière DAF"));

        if (statut == StatutDemande.VALIDE_DAF)
            return list;

        if (statut == StatutDemande.REJETEE) {
            cursor = cursor.plusHours(12 + rnd.nextInt(48));
            list.add(hist("DemandeAchatInterne", null, "VALIDE_DAF", "REJETEE", daf,
                    cursor, pickRejectionReason()));
            return list;
        }

        cursor = cursor.plusHours(24 + rnd.nextInt(96));
        list.add(hist("DemandeAchatInterne", null, "VALIDE_DAF", "VALIDE_DG", dg,
                cursor, "Escalade DG — montant significatif"));

        if (statut == StatutDemande.VALIDE_DG)
            return list;

        cursor = cursor.plusHours(48 + rnd.nextInt(120));
        list.add(hist("DemandeAchatInterne", null, "VALIDE_DG", "APPROUVEE", dg,
                cursor, "Approuvée par la Direction Générale"));

        if (statut == StatutDemande.APPROUVEE)
            return list;

        cursor = cursor.plusHours(8 + rnd.nextInt(24));
        list.add(hist("DemandeAchatInterne", null, "APPROUVEE", "PO_CREE", daf,
                cursor, "Bon de commande généré"));

        if (statut == StatutDemande.PO_CREE)
            return list;

        cursor = cursor.plusHours(24 + rnd.nextInt(72));
        list.add(hist("DemandeAchatInterne", null, "PO_CREE", "EN_LIVRAISON", daf,
                cursor, "Commande expédiée par le fournisseur"));

        if (statut == StatutDemande.AFFECTEE) {
            cursor = cursor.plusHours(24 + rnd.nextInt(120));
            list.add(hist("DemandeAchatInterne", null, "EN_LIVRAISON", "AFFECTEE", tech,
                    cursor, "Article réceptionné et affecté"));
        }

        return list;
    }

    private StatusHistory hist(String type, Long entiteId, String avant, String apres,
            User user, LocalDateTime date, String comment) {
        StatusHistory h = new StatusHistory(type, entiteId, avant, apres, user, comment);
        h.setDateModification(date);
        return h;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void addToPool(List<StatutDemande> pool, StatutDemande statut, int count) {
        for (int i = 0; i < count; i++)
            pool.add(statut);
    }

    private boolean isValidated(StatutDemande s) {
        return s != StatutDemande.BROUILLON && s != StatutDemande.SOUMISE;
    }

    private boolean isConsommeBudget(StatutDemande s) {
        return s == StatutDemande.APPROUVEE
                || s == StatutDemande.PO_CREE
                || s == StatutDemande.EN_LIVRAISON
                || s == StatutDemande.AFFECTEE;
    }

    private UrgenceDemande pickUrgence() {
        int r = rnd.nextInt(100);
        if (r < 10)
            return UrgenceDemande.CRITIQUE;
        if (r < 30)
            return UrgenceDemande.URGENTE;
        return UrgenceDemande.NORMALE;
    }

    private Object[][] cataloguePourFamille(Family f) {
        if (f.getLibelle() == null)
            return CATALOGUE_DIV;
        String lib = f.getLibelle().toLowerCase();
        if (lib.contains("informatique") || lib.contains("matériel"))
            return CATALOGUE_IT;
        if (lib.contains("logiciel") || lib.contains("licence"))
            return CATALOGUE_SOFT;
        if (lib.contains("bureau") || lib.contains("mobilier"))
            return CATALOGUE_BUR;
        return CATALOGUE_DIV;
    }

    private String pickJustification(String designation, String dept) {
        String[] justifs = {
                "Équipement en fin de vie, remplacement urgent requis",
                "Nouveau projet nécessite cet équipement pour " + dept,
                "Stock épuisé — rupture opérationnelle imminente",
                "Conformité réglementaire BAG exige cette mise à niveau",
                "Demande validée lors du comité de direction mensuel",
                "Extension de capacité suite à augmentation d'effectifs",
                "Contrat cadre à renouveler avant fin du trimestre",
                "Panne critique de l'équipement existant — impact production",
                "Budget annuel alloué non encore consommé — opportunité",
                "Audit interne a identifié ce besoin comme prioritaire"
        };
        return justifs[rnd.nextInt(justifs.length)];
    }

    private String pickRejectionReason() {
        String[] reasons = {
                "Budget famille épuisé pour ce trimestre",
                "Doublon avec commande existante en cours",
                "Fournisseur proposé non référencé BAG",
                "Montant estimé non justifié — demande de devis comparatifs",
                "Priorité non confirmée par la direction concernée"
        };
        return reasons[rnd.nextInt(reasons.length)];
    }
}
