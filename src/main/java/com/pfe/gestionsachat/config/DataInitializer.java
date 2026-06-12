package com.pfe.gestionsachat.config;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.model.POStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import org.springframework.core.annotation.Order;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private SupplierRepository supplierRepository;

    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private DemandeAchatInterneRepository demandeAchatInterneRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private com.pfe.gestionsachat.service.WarehouseService warehouseService;
    @Autowired private GrnHeaderRepository grnHeaderRepository;
    @Autowired private OffreFournisseurRepository offreFournisseurRepository;
    @Autowired private BCryptPasswordEncoder encoder;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Autowired private BudgetPiecesRepository budgetPiecesRepository;

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Override
    @SuppressWarnings("null")
    public void run(String... args) throws Exception {
        // 0. Drop enum check constraints that prevent adding new roles
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        } catch (Exception e) {
            log.warn("Could not drop users_role_check: {}", e.getMessage());
        }

        // 1. Nettoyage complet et robuste via TRUNCATE avec RESTART IDENTITY pour éviter les décalages d'IDs
        try {
            jdbcTemplate.execute("TRUNCATE TABLE audit_log, status_history, justification, action, budget_transfer, purchase_order, offre_fournisseur, da_details, da_header, demande_achat_interne, demande_ajustement, sub_family, family, budget_pieces, warehouse, stock_item, stock_movement, grn_details, grn_header, grc_details, grc_header, credit_note, invoice, transfer_header, transfer_line, users RESTART IDENTITY CASCADE");
            log.info("✅ Nettoyage par TRUNCATE terminé.");
        } catch (Exception e) {
            log.warn("⚠️ Échec du TRUNCATE : {}", e.getMessage());
            familyRepository.deleteAll();
            userRepository.deleteAll();
        }

        // On récupère ou on crée le demandeur
        User demandeur;
        if (userRepository.count() == 0) {
            log.info("👥 Création des utilisateurs système...");
            
            User n1       = new User("Younesse Filali",    "n1@test.com",       encoder.encode("password"), Role.MANAGER_N1);
            User tech     = new User("Halim Mansouri",     "tech@test.com",     encoder.encode("password"), Role.TECHNICIEN);
            User acheteur = new User("Abdsamad Alami",     "acheteur@test.com", encoder.encode("password"), Role.ACHETEUR);
            User acheteurInfo = new User("Acheteur Info",  "acheteur.info@test.com", encoder.encode("password"), Role.ACHETEUR_INFORMATIQUE);
            User acheteurBureau = new User("Acheteur Bureau",  "acheteur.bureau@test.com", encoder.encode("password"), Role.ACHETEUR_BUREAUTIQUE);
            User acheteurMob = new User("Acheteur Mobilier",  "acheteur.mob@test.com", encoder.encode("password"), Role.ACHETEUR_MOBILIER);
            User acheteurCons = new User("Acheteur Consommable",  "acheteur.cons@test.com", encoder.encode("password"), Role.ACHETEUR_CONSOMMABLE);
            User acheteurAutre = new User("Acheteur Autre",  "acheteur.autre@test.com", encoder.encode("password"), Role.ACHETEUR_AUTRE);
            User amg      = new User("Ilham Bennani",      "amg@test.com",      encoder.encode("password"), Role.AMG);
            User daf      = new User("Ibtissame Saadalh",  "daf@test.com",      encoder.encode("password"), Role.DAF);
            User dg       = new User("Abdelhamid Barakate", "dg@test.com",       encoder.encode("password"), Role.DG);
            User admin    = new User("Zaid Ziani",         "admin@test.com",    encoder.encode("password"), Role.ADMINISTRATEUR);
            
            demandeur = new User("Mehdi Benjelloun", "demandeur@test.com", encoder.encode("password"), Role.EMPLOYE);
            demandeur.setService("LOGISTIQUE BAG");
            demandeur.setN1(n1);
            
            User magasinier = new User("Hassan Rahhali",   "magasinier@test.com", encoder.encode("password"), Role.MAGASINIER);
            User comptable   = new User("Nadia Berrada",    "comptable@test.com",  encoder.encode("password"), Role.COMPTABLE);
            User respAchat   = new User("Omar Kettani",     "resp.achat@test.com", encoder.encode("password"), Role.RESP_ACHAT);

            userRepository.saveAll(List.of(n1, tech, acheteur, acheteurInfo, acheteurBureau, acheteurMob, acheteurCons, acheteurAutre, amg, daf, dg, admin, demandeur, magasinier, comptable, respAchat));
        } else {
            demandeur = userRepository.findByEmail("demandeur@test.com").orElse(null);
        }

        log.info("🏗️ Début du seeding des données...");

        // Familles — budgets annuels réalistes grande entreprise (en DHS)
        Family famIT     = new Family("Matériel Informatique",  BigDecimal.valueOf(2_000_000.0));
        famIT.setCategorie(CategorieDemande.INFORMATIQUE);

        Family famSoft   = new Family("Licences & Logiciels",   BigDecimal.valueOf(1_200_000.0));
        famSoft.setCategorie(CategorieDemande.INFORMATIQUE);

        Family famBur    = new Family("Bureautique & Mobilier", BigDecimal.valueOf(800_000.0));
        famBur.setCategorie(CategorieDemande.BUREAUTIQUE);

        Family famDivers = new Family("Fournitures & Services", BigDecimal.valueOf(600_000.0));
        famDivers.setCategorie(CategorieDemande.AUTRE);
        familyRepository.saveAll(List.of(famIT, famSoft, famBur, famDivers));
        log.info("📂 4 Familles créées.");

        // Sous-Familles — ventilation cohérente avec l'enveloppe Famille
        // Règle : Σ SubFamily.budgetInitial = Family.budgetInitial
        SubFamily sfLaptop  = new SubFamily("PC Portables & Stations",          BigDecimal.valueOf(900_000.0), famIT);
        SubFamily sfPeri    = new SubFamily("Périphériques (Écrans, Claviers)", BigDecimal.valueOf(600_000.0), famIT);
        SubFamily sfStock   = new SubFamily("Stockage & Serveurs",               BigDecimal.valueOf(500_000.0), famIT);

        SubFamily sfCloud   = new SubFamily("Abonnements Cloud (Azure/AWS)",     BigDecimal.valueOf(500_000.0), famSoft);
        SubFamily sfOffice  = new SubFamily("Suites Bureautiques (M365)",        BigDecimal.valueOf(400_000.0), famSoft);
        SubFamily sfSpec    = new SubFamily("Logiciels Métiers",                 BigDecimal.valueOf(300_000.0), famSoft);

        SubFamily sfMobilier = new SubFamily("Bureaux & Chaises Ergonomiques",   BigDecimal.valueOf(450_000.0), famBur);
        SubFamily sfClim     = new SubFamily("Climatisation & Aménagement",      BigDecimal.valueOf(350_000.0), famBur);

        SubFamily sfFourni   = new SubFamily("Fournitures de bureau",            BigDecimal.valueOf(300_000.0), famDivers);
        SubFamily sfTraiteur = new SubFamily("Services Traiteur & Réception",    BigDecimal.valueOf(300_000.0), famDivers);

        subFamilyRepository.saveAll(List.of(
            sfLaptop, sfPeri, sfStock,
            sfCloud, sfOffice, sfSpec,
            sfMobilier, sfClim,
            sfFourni, sfTraiteur
        ));
        log.info("📂 10 Sous-Familles créées.");

        // 4. Fournisseurs (Noms professionnels adaptés avec détails enrichis)
        Supplier sup1 = new Supplier("Alpha IT Solutions", "Mehdi Tazi", "Casablanca Tech Park", "INFORMATIQUE", 5, 3);
        sup1.setEmail("contact@alpha-it.ma");
        sup1.setPhone("0522-123456");
        sup1.setIsCertified(true);
        sup1.setIce("001524879654123");

        Supplier sup2 = new Supplier("Global Office Systems", "Sami Alami", "Rabat Business Center", "MOBILIER", 4, 7);
        sup2.setEmail("sales@global-office.ma");
        sup2.setPhone("0537-654321");
        sup2.setIce("002365478965412");

        Supplier sup3 = new Supplier("Elite Services Group", "Fatima Zahra", "Tanger Med", "DIVERS", 3, 5);
        sup3.setEmail("info@elite-services.ma");
        sup3.setIce("003147852369874");
        
        supplierRepository.saveAll(List.of(sup1, sup2, sup3));
        log.info("🚚 3 Fournisseurs créés.");

        // 5. Entrepôt par défaut
        if (warehouseRepository.count() == 0) {
            Warehouse w = new Warehouse();
            w.setName("Magasin Central BAG");
            w.setLocation("Casablanca - Siège");
            w.setType(WarehouseType.CENTRAL);
            warehouseRepository.save(w);
        }

        // 6. Demandes d'achat (Exemples Internes)
        
        // DA 1: En attente N1
        DemandeAchatInterne da1 = new DemandeAchatInterne();
        da1.setDesignation("Renouvellement parc Laptops (Dev Team)");
        da1.setDemandeur(demandeur);
        da1.setStatut(StatutDemande.SOUMISE);
        da1.setQuantite(3);
        da1.setMontantEstime(BigDecimal.valueOf(28_000.0).multiply(BigDecimal.valueOf(3)));
        da1 = demandeAchatInterneRepository.save(da1);
        log.info("✅ DA Interne-1 créée.");

        // DA 2: En attente Technicien
        DemandeAchatInterne da2 = new DemandeAchatInterne();
        da2.setDesignation("Installation Climatisation Salle Réunion");
        da2.setDemandeur(demandeur);
        da2.setStatut(StatutDemande.VALIDE_N1);
        da2.setQuantite(1);
        da2.setMontantEstime(BigDecimal.valueOf(8_500.0));
        demandeAchatInterneRepository.save(da2);

        // DA 3: En attente Acheteur
        DemandeAchatInterne da3 = new DemandeAchatInterne();
        da3.setDesignation("Licences Microsoft 365 Business");
        da3.setDemandeur(demandeur);
        da3.setStatut(StatutDemande.VALIDE_TECH);
        da3.setQuantite(50);
        da3.setMontantEstime(BigDecimal.valueOf(450.0).multiply(BigDecimal.valueOf(50)));
        demandeAchatInterneRepository.save(da3);

        // DA 4: VALIDÉE & PO GÉNÉRÉ (Pour tester la logistique)
        if (purchaseOrderRepository.count() == 0) {
            DemandeAchatInterne da4 = new DemandeAchatInterne();
            da4.setDesignation("Fournitures de bureau (Stock)");
            da4.setDemandeur(demandeur);
            da4.setStatut(StatutDemande.PO_CREE);
            da4.setQuantite(100);
            da4.setMontantEstime(BigDecimal.valueOf(65.0).multiply(BigDecimal.valueOf(100)));
            da4.setItemCode("PPR-A4");
            da4.setFournisseur(sup2);
            demandeAchatInterneRepository.save(da4);
            
            PurchaseOrder po = new PurchaseOrder();
            po.setDemandeInterne(da4);
            po.setStatut(POStatus.APPROVED);  // Seedé en APPROVED pour tests logistique (GRN/GRC)
            po.setMontantTotal(BigDecimal.valueOf(6500.0));
            po.setDateCreation(java.time.LocalDate.now());
            po.setFournisseur(sup2);
            PurchaseOrder savedPo = purchaseOrderRepository.save(po);
            savedPo.setPoNumber("PO-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format("%05d", savedPo.getIdPo()));
            purchaseOrderRepository.save(savedPo);
            log.info("📦 PO Seedé pour test logistique : {}", savedPo.getPoNumber());
            
            // Seed a GRN linked to this PO
            com.pfe.gestionsachat.model.GrnHeader grn = new com.pfe.gestionsachat.model.GrnHeader();
            grn.setPurchaseOrder(savedPo);
            grn.setDeliveryNoteNumber("BL-2026-TEST");
            grn.setReceiptDate(java.time.LocalDate.now());
            grn.setStatus(com.pfe.gestionsachat.model.GrnStatus.ENTRY_COMPLETED);
            grn.setSupplier(sup2);
            grn.setReceivedBy(userRepository.findByEmail("magasinier@test.com").orElse(null));
            
            com.pfe.gestionsachat.model.GrnDetails grnDetail = new com.pfe.gestionsachat.model.GrnDetails();
            grnDetail.setGrnHeader(grn);
            grnDetail.setItemCode("PPR-A4");
            grnDetail.setItemName("Ramette Papier A4");
            grnDetail.setOrderedQuantity(100);
            grnDetail.setShippedQuantity(0);
            grnDetail.setReceivedQuantity(100);
            grnDetail.setAcceptedQuantity(100);
            grnDetail.setRejectedQuantity(0);
            grnDetail.setQualityStatus(com.pfe.gestionsachat.model.QualityStatus.APPROVED);
            grn.setDetails(java.util.List.of(grnDetail));

            com.pfe.gestionsachat.model.GrnHeader savedGrn = grnHeaderRepository.save(grn);
            savedGrn.setGrnNumber("GRN-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format("%05d", savedGrn.getId()));
            grnHeaderRepository.save(savedGrn);
            
            // FORENSIC FIX: Update stock to avoid silent inconsistency!
            warehouseService.addStock(
                "PPR-A4", 
                "Ramette Papier A4", 
                100, 
                savedGrn.getGrnNumber()
            );
            
            log.info("📦 GRN Seedé pour test logistique : {}", savedGrn.getGrnNumber());
        }
        // 7. Demandes d'Achat Internes (flux principal)
        if (demandeAchatInterneRepository.count() == 0) {
            DemandeAchatInterne di1 = new DemandeAchatInterne();
            di1.setDemandeur(demandeur);
            di1.setDepartement("LOGISTIQUE BAG");
            di1.setCategorie(CategorieDemande.INFORMATIQUE);
            di1.setDesignation("Imprimante HP LaserJet Pro");
            di1.setQuantite(2);
            di1.setJustification("Imprimantes actuelles en panne depuis 2 semaines");
            di1.setUrgence(UrgenceDemande.URGENTE);
            di1.setStatut(StatutDemande.SOUMISE);
            demandeAchatInterneRepository.save(di1);

            DemandeAchatInterne di2 = new DemandeAchatInterne();
            di2.setDemandeur(demandeur);
            di2.setDepartement("LOGISTIQUE BAG");
            di2.setCategorie(CategorieDemande.BUREAUTIQUE);
            di2.setDesignation("Ramettes papier A4 80g");
            di2.setQuantite(50);
            di2.setJustification("Stock épuisé pour le trimestre");
            di2.setUrgence(UrgenceDemande.NORMALE);
            di2.setStatut(StatutDemande.VALIDE_N1);
            di2.setBudgetFamille(famBur);
            di2.setBudgetSousFamille(sfMobilier);
            demandeAchatInterneRepository.save(di2);

            DemandeAchatInterne di3 = new DemandeAchatInterne();
            di3.setDemandeur(demandeur);
            di3.setDepartement("LOGISTIQUE BAG");
            di3.setCategorie(CategorieDemande.MOBILIER);
            di3.setDesignation("Chaise ergonomique bureau");
            di3.setQuantite(5);
            di3.setJustification("Renouvellement mobilier open-space");
            di3.setUrgence(UrgenceDemande.NORMALE);
            di3.setStatut(StatutDemande.VALIDE_TECH); // Set to VALIDE_TECH for Buyer processing
            di3.setMontantEstime(BigDecimal.valueOf(15000.0));
            di3.setBudgetFamille(famBur);
            di3.setBudgetSousFamille(sfMobilier);
            demandeAchatInterneRepository.save(di3);

            di1.setBudgetFamille(famIT);
            di1.setBudgetSousFamille(sfLaptop);
            demandeAchatInterneRepository.save(di1);

            DemandeAchatInterne di4 = new DemandeAchatInterne();
            di4.setDemandeur(demandeur);
            di4.setDepartement("LOGISTIQUE BAG");
            di4.setCategorie(CategorieDemande.CONSOMMABLE);
            di4.setDesignation("Cartouches encre HP 305XL");
            di4.setQuantite(20);
            di4.setJustification("Consommable courant");
            di4.setUrgence(UrgenceDemande.NORMALE);
            di4.setStatut(StatutDemande.BROUILLON);
            demandeAchatInterneRepository.save(di4);

            log.info("📋 4 Demandes d'achat internes seedées");

            // 8. Offres Fournisseurs (Pour DA-006 / di3 et autres)
            log.info("💰 Création des offres comparatives...");
            offreFournisseurRepository.saveAll(List.of(
                // Offres pour DI3
                new OffreFournisseur(di3, sup1, BigDecimal.valueOf(14500.0), "Garantie 3 ans incluse, Livraison Express (48h)", 2),
                new OffreFournisseur(di3, sup2, BigDecimal.valueOf(13800.0), "Remise 5% sur volume, Livraison sous 1 semaine", 7),
                new OffreFournisseur(di3, sup3, BigDecimal.valueOf(15200.0), "Service Premium, Installation et configuration sur site", 3),
                
                // Offres pour DI1 (Laptops)
                new OffreFournisseur(di1, sup1, BigDecimal.valueOf(27500.0), "Garantie 5 ans ProSupport", 5),
                new OffreFournisseur(di1, sup2, BigDecimal.valueOf(29000.0), "Modèles équivalents HP", 10),
                
                // Offres pour DI2 (Papier A4)
                new OffreFournisseur(di2, sup2, BigDecimal.valueOf(3250.0), "Livraison gratuite", 3),
                new OffreFournisseur(di2, sup3, BigDecimal.valueOf(3100.0), "Paiement à la livraison", 1)
            ));
        }

        log.info("✅ Données BAG initialisées avec succès !");

        // 9. Seeding des Rayons et Articles (Flux de Pièces)
        if (stockItemRepository.count() < 10) {
            log.info("🛠️ Seeding des Rayons et Pièces de Rechange (Taxonomie Réaliste)...");
            Warehouse central = warehouseRepository.findAll().get(0);
            Warehouse magTanger = warehouseRepository.findByName("Magasin Tanger BAG").orElseGet(() -> {
                Warehouse w = new Warehouse(); w.setName("Magasin Tanger BAG"); w.setLocation("Tanger"); w.setType(WarehouseType.REGIONAL); return warehouseRepository.save(w);
            });
            Warehouse magMarrakech = warehouseRepository.findByName("Magasin Marrakech BAG").orElseGet(() -> {
                Warehouse w = new Warehouse(); w.setName("Magasin Marrakech BAG"); w.setLocation("Marrakech"); w.setType(WarehouseType.REGIONAL); return warehouseRepository.save(w);
            });
            Warehouse magAgadir = warehouseRepository.findByName("Magasin Agadir BAG").orElseGet(() -> {
                Warehouse w = new Warehouse(); w.setName("Magasin Agadir BAG"); w.setLocation("Agadir"); w.setType(WarehouseType.REGIONAL); return warehouseRepository.save(w);
            });
            Warehouse agenceCasa = warehouseRepository.findByName("Agence Casablanca").orElseGet(() -> {
                Warehouse w = new Warehouse(); w.setName("Agence Casablanca"); w.setLocation("Casablanca - Agence Commerciale"); w.setType(WarehouseType.REGIONAL); return warehouseRepository.save(w);
            });
            Warehouse magRabat = warehouseRepository.findByName("Magasin Rabat BAG").orElseGet(() -> {
                Warehouse w = new Warehouse(); w.setName("Magasin Rabat BAG"); w.setLocation("Rabat"); w.setType(WarehouseType.REGIONAL); return warehouseRepository.save(w);
            });

            // ── NOMENCLATURE CODE SITE : LOC-{SITE}-{YYYYMM}-{HEX8} ────────────────────────────
            // Règle : le suffixe HEX8 est déterministe = hash(itemCode + sitePrefix) & 0xFFFFFFFF
            // Cela garantit que le même article au même site a toujours le même code entre redémarrages.
            String yyyymm = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            java.util.function.BiFunction<String, String, String> loc = (sitePrefix, itemCode) -> {
                long hash = (long)(itemCode + sitePrefix).hashCode() & 0xFFFFFFFFL;
                return String.format("LOC-%s-%s-%08X", sitePrefix, yyyymm, hash);
            };

            // Helper : (Warehouse, sitePrefix, itemCode, itemName, qty, unitCost) -> StockItem
            java.util.function.Function<Object[], StockItem> createItem = (Object[] d) -> {
                Warehouse w  = (Warehouse) d[0];
                String site  = (String)    d[1];  // prefix : BAG, CASA, TNG, MRK, AGA, RBT
                String code  = (String)    d[2];
                String name  = (String)    d[3];
                int qty      = (int)       d[4];
                double cost  = (double)    d[5];
                StockItem item = new StockItem();
                item.setWarehouse(w);
                item.setItemCode(code);
                item.setItemName(name);
                item.setCategory(ItemCategory.PIECE_RECHANGE);
                item.setLocationCode(loc.apply(site, code));  // ← LOC-CASA-202606-XXXXXXXX
                item.setQuantityAvailable(qty);
                item.setUnitCost(BigDecimal.valueOf(cost));
                return item;
            };

            List<StockItem> newItems = List.of(
                // ══ ZONE A — Consommables & Fluides ══════════════════════════════
                createItem.apply(new Object[]{central,      "BAG",  "FILT-001",  "Filtre à huile Premium",             0, 45.0}), // ⚠️ RUPTURE POUR DEMO
                createItem.apply(new Object[]{central,      "BAG",  "HUI-5W40",  "Huile Moteur 5W40 5L",               0, 150.0}), // ⚠️ RUPTURE POUR DEMO
                createItem.apply(new Object[]{central,      "BAG",  "LIQ-FR-1L", "Liquide de Frein DOT4 1L",         250, 65.0}),

                createItem.apply(new Object[]{agenceCasa,   "CASA", "FILT-001",  "Filtre à huile Premium",            50, 45.0}),
                createItem.apply(new Object[]{agenceCasa,   "CASA", "HUI-5W40",  "Huile Moteur 5W40 5L",               0, 150.0}), // ⚠️ RUPTURE

                createItem.apply(new Object[]{magTanger,    "TNG",  "FILT-001",  "Filtre à huile Premium",            20, 45.0}),
                createItem.apply(new Object[]{magTanger,    "TNG",  "HUI-5W40",  "Huile Moteur 5W40 5L",              10, 150.0}),

                // ══ ZONE B — Liaisons au Sol & Freinage ══════════════════════════
                createItem.apply(new Object[]{central,      "BAG",  "FREIN-AV",  "Plaquettes de frein avant",        200, 320.0}),
                createItem.apply(new Object[]{central,      "BAG",  "PNEU-205",  "Pneu Michelin 205/55 R16",         400, 950.0}),
                createItem.apply(new Object[]{central,      "BAG",  "AMORT-AV",  "Amortisseur avant à gaz",          150, 850.0}),

                createItem.apply(new Object[]{agenceCasa,   "CASA", "FREIN-AV",  "Plaquettes de frein avant",          0, 320.0}), // ⚠️ RUPTURE
                createItem.apply(new Object[]{agenceCasa,   "CASA", "PNEU-205",  "Pneu Michelin 205/55 R16",          12, 950.0}),

                createItem.apply(new Object[]{magMarrakech, "MRK",  "FREIN-AV",  "Plaquettes de frein avant",         30, 320.0}),
                createItem.apply(new Object[]{magMarrakech, "MRK",  "AMORT-AV",  "Amortisseur avant à gaz",            8, 850.0}),

                // ══ ZONE C — Moteur & Électricité ════════════════════════════════
                createItem.apply(new Object[]{central,      "BAG",  "BATT-70",   "Batterie 12V 70Ah",                100, 850.0}),
                createItem.apply(new Object[]{central,      "BAG",  "COUR-DIST", "Kit Courroie de distribution",      80, 600.0}),

                createItem.apply(new Object[]{agenceCasa,   "CASA", "BATT-70",   "Batterie 12V 70Ah",                  5, 850.0}),
                createItem.apply(new Object[]{agenceCasa,   "CASA", "COUR-DIST", "Kit Courroie de distribution",       0, 600.0}), // ⚠️ RUPTURE

                createItem.apply(new Object[]{magAgadir,    "AGA",  "COUR-DIST", "Kit Courroie de distribution",       5, 600.0}),
                createItem.apply(new Object[]{magRabat,     "RBT",  "BATT-70",   "Batterie 12V 70Ah",                  0, 850.0}), // ⚠️ RUPTURE

                // ══ ZONE D — Outillage & Équipement ══════════════════════════════
                createItem.apply(new Object[]{central,      "BAG",  "CLE-DYN",   "Clé dynamométrique 1/4",            50, 450.0})
            );

            try {
                stockItemRepository.saveAll(newItems);
                log.info("✅ {} articles seedés — Nomenclature LOC-{{SITE}}-{{}}-{{HEX8}} appliquée.", newItems.size());
            } catch (Exception e) {
                log.info("⚠️ Erreur lors du seeding des articles : {}", e.getMessage());
            }
        }

        // FORCE OUT OF STOCK FOR DEMO (regardless of whether seeding ran or was skipped)
        try {
            jdbcTemplate.execute("UPDATE stock_item SET quantity_available = 0 WHERE item_code IN ('FILT-001', 'HUI-5W40') AND location_code LIKE 'LOC-BAG%'");
            log.info("🔥 FORCE DEMO : Stock de FILT-001 et HUI-5W40 mis à 0 pour le magasin BAG !");
        } catch (Exception e) {
            log.warn("⚠️ Impossible de forcer le stock à 0 : {}", e.getMessage());
        }

        // 10. Budget Pièces de Rechange
        if (budgetPiecesRepository.count() == 0) {
            String currentYear = String.valueOf(java.time.LocalDate.now().getYear());
            log.info("💰 Initialisation du pool Budget Pièces de Rechange (Exercice {})...", currentYear);
            BudgetPieces bpCurrent = new BudgetPieces(currentYear, java.math.BigDecimal.valueOf(1000000.0));
            budgetPiecesRepository.save(bpCurrent);
            log.info("✅ Pool Budget Pièces {} créé avec 1,000,000.0 DZD.", currentYear);
        }

        // ── RISQUE-19 + RISQUE-26 : Seed flux Transfert Inter-Sites ──────────────────────────
        // MAJEUR-03 (corrigé) : findByName() deterministe — findAll().get(0) sans ORDER BY ne l'est pas.

        // 11. Warehouse central (référence) + Agence Casablanca (destination)
        Warehouse central = warehouseRepository.findByName("Magasin Central BAG")
            .orElseGet(() -> {
                Warehouse w = new Warehouse();
                w.setName("Magasin Central BAG");
                w.setLocation("Casablanca - Siège");
                w.setType(WarehouseType.CENTRAL);
                return warehouseRepository.save(w);
            });

        Warehouse agenceCasa = warehouseRepository.findByName("Agence Casablanca")
            .orElseGet(() -> {
                Warehouse w = new Warehouse();
                w.setName("Agence Casablanca");
                w.setLocation("Casablanca - Agence Commerciale");
                w.setType(WarehouseType.REGIONAL);
                Warehouse saved = warehouseRepository.save(w);
                log.info("🏭 Warehouse seedé : Agence Casablanca (id={})", saved.getId());
                return saved;
            });

        // Assigner le magasinier source au warehouse central s'il ne l'est pas encore
        userRepository.findByEmail("magasinier@test.com").ifPresent(mag -> {
            if (mag.getWarehouse() == null) {
                mag.setWarehouse(central);
                userRepository.save(mag);
                log.info("🔗 Magasinier Hassan Rahhali assigné à '{}'", central.getName());
            }
        });

        // 12. MAGASINIER_DEST — garanti d'avoir un warehouse
        if (userRepository.findByRole(Role.MAGASINIER_DEST).isEmpty()) {
            User magDest = new User("Karim Alaoui", "magasinier.dest@test.com",
                encoder.encode("password"), Role.MAGASINIER_DEST);
            warehouseRepository.findByName("Magasin Marrakech BAG").ifPresent(magDest::setWarehouse);
            userRepository.save(magDest);
            log.info("👷 MAGASINIER_DEST Karim Alaoui seedé pour warehouse 'Magasin Marrakech BAG'");
        }

        // 13. Nouveaux Magasiniers Spécifiques
        if (userRepository.findByEmail("magasinier.casa@test.com").isEmpty()) {
            User magCasa = new User("Magasinier Casa", "magasinier.casa@test.com", encoder.encode("password"), Role.MAGASINIER);
            warehouseRepository.findByName("Agence Casablanca").ifPresent(magCasa::setWarehouse);
            userRepository.save(magCasa);
            log.info("👷 MAGASINIER seedé pour 'Agence Casablanca'");
        }
        
        if (userRepository.findByEmail("magasinier.rabat@test.com").isEmpty()) {
            Warehouse magRabat = warehouseRepository.findByName("Magasin Rabat BAG").orElseGet(() -> {
                Warehouse w = new Warehouse(); w.setName("Magasin Rabat BAG"); w.setLocation("Rabat"); w.setType(WarehouseType.REGIONAL); return warehouseRepository.save(w);
            });
            User magUserRabat = new User("Magasinier Rabat", "magasinier.rabat@test.com", encoder.encode("password"), Role.MAGASINIER);
            magUserRabat.setWarehouse(magRabat);
            userRepository.save(magUserRabat);
            log.info("👷 MAGASINIER seedé pour 'Magasin Rabat BAG'");
        }

        if (userRepository.findByEmail("magasinier.tanger@test.com").isEmpty()) {
            User magTangerUser = new User("Magasinier Tanger", "magasinier.tanger@test.com", encoder.encode("password"), Role.MAGASINIER);
            warehouseRepository.findByName("Magasin Tanger BAG").ifPresent(magTangerUser::setWarehouse);
            userRepository.save(magTangerUser);
            log.info("👷 MAGASINIER seedé pour 'Magasin Tanger BAG'");
        }

        if (userRepository.findByEmail("magasinier.marrakech@test.com").isEmpty()) {
            User magMarrakechUser = new User("Magasinier Marrakech", "magasinier.marrakech@test.com", encoder.encode("password"), Role.MAGASINIER);
            warehouseRepository.findByName("Magasin Marrakech BAG").ifPresent(magMarrakechUser::setWarehouse);
            userRepository.save(magMarrakechUser);
            log.info("👷 MAGASINIER seedé pour 'Magasin Marrakech BAG'");
        }


        log.info("Encadrants : M. Abdelhamid Barakate & Mme Ibtissame Saadalh");
    }
}
