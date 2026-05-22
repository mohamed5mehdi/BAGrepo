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
    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private DaDetailsRepository daDetailsRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private DemandeAchatInterneRepository demandeAchatInterneRepository;
    @Autowired private StockItemRepository stockItemRepository;
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
            jdbcTemplate.execute("TRUNCATE TABLE audit_log, status_history, justification, action, budget_transfer, purchase_order, offre_fournisseur, da_details, da_header, demande_achat_interne, demande_ajustement, sub_family, family, warehouse, stock_item, stock_movement, grn_details, grn_header, grc_details, grc_header, credit_note, invoice, transfer_header, transfer_line, users RESTART IDENTITY CASCADE");
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

            userRepository.saveAll(List.of(n1, tech, acheteur, amg, daf, dg, admin, demandeur, magasinier, comptable, respAchat));
        } else {
            demandeur = userRepository.findByEmail("demandeur@test.com").orElse(null);
        }

        log.info("🏗️ Début du seeding des données...");

        // 3. Familles & Sous-familles (Besoins Internes BAG)
        Family famIT     = new Family("Matériel Informatique",  BigDecimal.valueOf(500000.0));
        famIT.setCategorie(CategorieDemande.INFORMATIQUE);

        Family famSoft   = new Family("Licences & Logiciels",   BigDecimal.valueOf(300000.0));
        famSoft.setCategorie(CategorieDemande.INFORMATIQUE);

        Family famBur    = new Family("Bureautique & Mobilier", BigDecimal.valueOf(200000.0));
        famBur.setCategorie(CategorieDemande.BUREAUTIQUE);

        Family famDivers = new Family("Fournitures & Services", BigDecimal.valueOf(100000.0));
        famDivers.setCategorie(CategorieDemande.AUTRE);
        familyRepository.saveAll(List.of(famIT, famSoft, famBur, famDivers));
        log.info("📂 4 Familles créées.");

        SubFamily sfLaptop  = new SubFamily("PC Portables & Stations", BigDecimal.valueOf(250000.0), famIT);
        SubFamily sfPeri    = new SubFamily("Périphériques (Écrans, Claviers)", BigDecimal.valueOf(150000.0), famIT);
        SubFamily sfStock   = new SubFamily("Stockage & Serveurs", BigDecimal.valueOf(100000.0), famIT);
        
        SubFamily sfCloud   = new SubFamily("Abonnements Cloud (Azure/AWS)", BigDecimal.valueOf(150000.0), famSoft);
        SubFamily sfOffice  = new SubFamily("Suites Bureautiques (M365)", BigDecimal.valueOf(100000.0), famSoft);
        SubFamily sfSpec    = new SubFamily("Logiciels Métiers", BigDecimal.valueOf(50000.0), famSoft);
        
        SubFamily sfMobilier = new SubFamily("Bureaux & Chaises Ergonomiques", BigDecimal.valueOf(120000.0), famBur);
        SubFamily sfClim     = new SubFamily("Climatisation & Aménagement", BigDecimal.valueOf(80000.0), famBur);
        
        SubFamily sfFourni   = new SubFamily("Fournitures de bureau", BigDecimal.valueOf(50000.0), famDivers);
        SubFamily sfTraiteur = new SubFamily("Services Traiteur & Réception", BigDecimal.valueOf(50000.0), famDivers);
        
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
        DaHeader da1 = new DaHeader("Renouvellement parc Laptops (Dev Team)", demandeur);
        da1.setStatut(StatutDA.EN_ATTENTE_N1);
        da1 = daHeaderRepository.save(da1);
        daDetailsRepository.save(new DaDetails(da1, sfLaptop, 3, "MacBook Pro M3", BigDecimal.valueOf(28000.0)));
        log.info("📄 DA-1 créée.");

        // DA 2: En attente Technicien
        DaHeader da2 = new DaHeader("Installation Climatisation Salle Réunion", demandeur);
        da2.setStatut(StatutDA.EN_ATTENTE_TECH);
        daHeaderRepository.save(da2);
        daDetailsRepository.save(new DaDetails(da2, sfClim, 1, "Split LG 24000 BTU", BigDecimal.valueOf(8500.0)));

        // DA 3: En attente Acheteur
        DaHeader da3 = new DaHeader("Licences Microsoft 365 Business", demandeur);
        da3.setStatut(StatutDA.EN_ATTENTE_ACHAT);
        daHeaderRepository.save(da3);
        daDetailsRepository.save(new DaDetails(da3, sfOffice, 50, "Abonnement Annuel E3", BigDecimal.valueOf(450.0)));

        // DA 4: VALIDÉE & PO GÉNÉRÉ (Pour tester la logistique)
        if (purchaseOrderRepository.count() == 0) {
            DaHeader da4 = new DaHeader("Fournitures de bureau (Stock)", demandeur);
            da4.setStatut(StatutDA.PO_CREE);
            daHeaderRepository.save(da4);
            
            DaDetails det4 = new DaDetails(da4, sfFourni, 100, "Papier A4 80g", BigDecimal.valueOf(65.0));
            det4.setItemCode("PPR-A4");
            det4.setItemName("Ramette Papier A4");
            det4.setFournisseur(sup2);
            daDetailsRepository.save(det4);

            PurchaseOrder po = new PurchaseOrder();
            po.setDaHeader(da4);
            po.setStatut(POStatus.APPROVED);  // Seedé en APPROVED pour tests logistique (GRN/GRC)
            po.setMontantTotal(BigDecimal.valueOf(6500.0));
            po.setDateCreation(java.time.LocalDate.now());
            po.setFournisseur(sup2);
            PurchaseOrder savedPo = purchaseOrderRepository.save(po);
            savedPo.setPoNumber("PO-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format("%05d", savedPo.getIdPo()));
            purchaseOrderRepository.save(savedPo);
            log.info("📦 PO Seedé pour test logistique : {}", savedPo.getPoNumber());
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

            // 8. Offres Fournisseurs (Pour DA-006 / di3)
            log.info("💰 Création des offres comparatives...");
            offreFournisseurRepository.saveAll(List.of(
                new OffreFournisseur(di3, sup1, BigDecimal.valueOf(14500.0), "Garantie 3 ans incluse, Livraison Express (48h)", 2),
                new OffreFournisseur(di3, sup2, BigDecimal.valueOf(13800.0), "Remise 5% sur volume, Livraison sous 1 semaine", 7),
                new OffreFournisseur(di3, sup3, BigDecimal.valueOf(15200.0), "Service Premium, Installation et configuration sur site", 3)
            ));
        }

        log.info("✅ Données BAG initialisées avec succès !");

        // 9. Seeding des Rayons et Articles (Flux de Pièces)
        if (stockItemRepository.count() < 10) {
            log.info("🛠️ Seeding des Rayons et Pièces de Rechange...");
            Warehouse central = warehouseRepository.findAll().get(0);
            
            StockItem h1 = new StockItem();
            h1.setWarehouse(central); h1.setItemCode("HUI-POMP-001"); h1.setItemName("Huile à pompe hydraulique");
            h1.setCategory(ItemCategory.PIECE_RECHANGE); h1.setLocationCode("RAYON-LUBRIFIANT");
            h1.setQuantityAvailable(10); h1.setUnitCost(BigDecimal.valueOf(150.0));
            
            StockItem r1 = new StockItem();
            r1.setWarehouse(central); r1.setItemCode("ROUE-SEC-045"); r1.setItemName("Roue de secours 17 pouces");
            r1.setCategory(ItemCategory.PIECE_RECHANGE); r1.setLocationCode("RAYON-MECANIQUE");
            r1.setQuantityAvailable(4); r1.setUnitCost(BigDecimal.valueOf(1200.0));
            
            StockItem b1 = new StockItem();
            b1.setWarehouse(central); b1.setItemCode("BAT-V12-70AH"); b1.setItemName("Batterie 12V 70Ah");
            b1.setCategory(ItemCategory.PIECE_RECHANGE); b1.setLocationCode("RAYON-ELECTRIQUE");
            b1.setQuantityAvailable(0); b1.setUnitCost(BigDecimal.valueOf(850.0)); // Hors stock pour tester le flux achat
            
            StockItem c1 = new StockItem();
            c1.setWarehouse(central); c1.setItemCode("CLE-DYN-SMALL"); c1.setItemName("Clé dynamométrique 1/4");
            c1.setCategory(ItemCategory.PIECE_RECHANGE); c1.setLocationCode("RAYON-OUTILLAGE");
            c1.setQuantityAvailable(2); c1.setUnitCost(BigDecimal.valueOf(450.0));
            
            StockItem p1 = new StockItem();
            p1.setWarehouse(central); p1.setItemCode("PNEU-MIC-205"); p1.setItemName("Pneu Michelin 205/55 R16");
            p1.setCategory(ItemCategory.PIECE_RECHANGE); p1.setLocationCode("RAYON-PNEUMATIQUE");
            p1.setQuantityAvailable(8); p1.setUnitCost(BigDecimal.valueOf(950.0));
            
            // Nouveaux magasins pour le transfert inter-sites
            Warehouse magTanger = warehouseRepository.findByName("Magasin Tanger BAG").orElseGet(() -> {
                Warehouse w = new Warehouse(); w.setName("Magasin Tanger BAG"); w.setLocation("Tanger"); w.setType(WarehouseType.REGIONAL); return warehouseRepository.save(w);
            });
            Warehouse magMarrakech = warehouseRepository.findByName("Magasin Marrakech BAG").orElseGet(() -> {
                Warehouse w = new Warehouse(); w.setName("Magasin Marrakech BAG"); w.setLocation("Marrakech"); w.setType(WarehouseType.REGIONAL); return warehouseRepository.save(w);
            });
            Warehouse magAgadir = warehouseRepository.findByName("Magasin Agadir BAG").orElseGet(() -> {
                Warehouse w = new Warehouse(); w.setName("Magasin Agadir BAG"); w.setLocation("Agadir"); w.setType(WarehouseType.REGIONAL); return warehouseRepository.save(w);
            });

            // Pièces pour Tanger
            StockItem t1 = new StockItem();
            t1.setWarehouse(magTanger); t1.setItemCode("FILTRE-HUILE-01"); t1.setItemName("Filtre à huile Premium");
            t1.setCategory(ItemCategory.PIECE_RECHANGE); t1.setLocationCode("RAYON-FILTRATION");
            t1.setQuantityAvailable(15); t1.setUnitCost(BigDecimal.valueOf(45.0));

            // Pièces pour Marrakech
            StockItem m1 = new StockItem();
            m1.setWarehouse(magMarrakech); m1.setItemCode("PLAQUETTE-FREIN"); m1.setItemName("Plaquettes de frein avant");
            m1.setCategory(ItemCategory.PIECE_RECHANGE); m1.setLocationCode("RAYON-FREINAGE");
            m1.setQuantityAvailable(6); m1.setUnitCost(BigDecimal.valueOf(320.0));

            // Pièces pour Agadir
            StockItem a1 = new StockItem();
            a1.setWarehouse(magAgadir); a1.setItemCode("COURROIE-DIST"); a1.setItemName("Courroie de distribution");
            a1.setCategory(ItemCategory.PIECE_RECHANGE); a1.setLocationCode("RAYON-MOTEUR");
            a1.setQuantityAvailable(3); a1.setUnitCost(BigDecimal.valueOf(600.0));

            stockItemRepository.saveAll(List.of(h1, r1, b1, c1, p1, t1, m1, a1));
            log.info("✅ Rayons et articles seedés pour le flux Pièces et Transferts Inter-Sites.");
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


        log.info("Encadrants : M. Abdelhamid Barakate & Mme Ibtissame Saadalh");
    }
}
