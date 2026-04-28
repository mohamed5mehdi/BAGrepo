package com.pfe.gestionsachat.config;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private DaDetailsRepository daDetailsRepository;
    @Autowired private ActionRepository actionRepository;
    @Autowired private BudgetTransferRepository budgetTransferRepository;
    @Autowired private BCryptPasswordEncoder encoder;

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Override
    @SuppressWarnings("null")
    public void run(String... args) throws Exception {
        // On récupère ou on crée le demandeur
        User demandeur;
        if (userRepository.count() == 0) {
            log.info("⏳ Création des utilisateurs...");
            User n1       = new User("Manager N+1",       "n1@test.com",       encoder.encode("password"), Role.ROLE_N1);
            User tech     = new User("Expert Technicien",  "tech@test.com",     encoder.encode("password"), Role.ROLE_TECHNICIEN);
            User acheteur = new User("Acheteur Principal", "acheteur@test.com", encoder.encode("password"), Role.ROLE_ACHETEUR);
            User amg      = new User("Responsable AMG",    "amg@test.com",      encoder.encode("password"), Role.ROLE_AMG);
            User daf      = new User("Directeur DAF",      "daf@test.com",      encoder.encode("password"), Role.ROLE_DAF);
            User dg       = new User("Directeur Général",  "dg@test.com",       encoder.encode("password"), Role.ROLE_DG);
            User admin    = new User("Administrateur",     "admin@test.com",    encoder.encode("password"), Role.ROLE_ADMIN);
            
            demandeur = new User("Demandeur Test", "demandeur@test.com", encoder.encode("password"), Role.ROLE_DEMANDEUR);
            demandeur.setService("IT");
            demandeur.setN1(n1);
            
            userRepository.saveAll(List.of(n1, tech, acheteur, amg, daf, dg, admin, demandeur));
        } else {
            demandeur = userRepository.findByEmail("demandeur@test.com").orElse(null);
        }

        // Si on a encore les vieux noms "Budget IT", on vide et on recrée
        if (familyRepository.count() > 0 && familyRepository.findAll().get(0).getLibelle().contains("Budget")) {
            log.info("Sweep : Nettoyage des anciennes catégories...");
            actionRepository.deleteAll();
            budgetTransferRepository.deleteAll();
            daDetailsRepository.deleteAll();
            daHeaderRepository.deleteAll();
            subFamilyRepository.deleteAll();
            familyRepository.deleteAll();
        }

        if (familyRepository.count() > 0) return;

        // 3. Familles & Sous-familles (Noms fonctionnels pour le demandeur)
        Family famIT  = new Family("Informatique",  BigDecimal.valueOf(15000.0));
        Family famSG  = new Family("Services Généraux",  BigDecimal.valueOf(8000.0));
        Family famRH  = new Family("Ressources Humaines",  BigDecimal.valueOf(5000.0));
        familyRepository.saveAll(List.of(famIT, famSG, famRH));

        SubFamily sfHardware = new SubFamily("Matériel (PC, Écrans)", BigDecimal.valueOf(5000.0), famIT);
        SubFamily sfSoftware = new SubFamily("Logiciels & Licences", BigDecimal.valueOf(5000.0), famIT);
        SubFamily sfInfra    = new SubFamily("Réseau & Infrastructure", BigDecimal.valueOf(5000.0), famIT);
        
        SubFamily sfFourn    = new SubFamily("Fournitures de bureau", BigDecimal.valueOf(4000.0), famSG);
        SubFamily sfMobilier = new SubFamily("Mobilier & Aménagement", BigDecimal.valueOf(4000.0), famSG);
        
        SubFamily sfForm     = new SubFamily("Formation", BigDecimal.valueOf(2500.0), famRH);
        SubFamily sfRecrut   = new SubFamily("Recrutement", BigDecimal.valueOf(2500.0), famRH);
        
        subFamilyRepository.saveAll(List.of(
            sfHardware, sfSoftware, sfInfra, 
            sfFourn, sfMobilier, 
            sfForm, sfRecrut
        ));

        // 4. Fournisseurs
        Supplier sup1 = new Supplier("IT Pro B2B", "Ali Chérif", "Paris, France");
        Supplier sup2 = new Supplier("TechWorld SA", "Marie Dupont", "Lyon, France");
        Supplier sup3 = new Supplier("Bureau Express", "Jean Marc", "Marseille, France");
        supplierRepository.saveAll(List.of(sup1, sup2, sup3));

        // 5. Demandes d'achat (Différents statuts pour tester les dashboards)
        
        // DA 1: En attente N1
        DaHeader da1 = new DaHeader("Mise à niveau switches réseau", demandeur);
        da1.setStatut(StatutDA.EN_ATTENTE_N1);
        daHeaderRepository.save(da1);
        daDetailsRepository.save(new DaDetails(da1, sfHardware, 2, "Switch Cisco", BigDecimal.valueOf(600.0)));

        // DA 2: En attente Technicien
        DaHeader da2 = new DaHeader("Licences Windows 11 Pro", demandeur);
        da2.setStatut(StatutDA.EN_ATTENTE_TECH);
        daHeaderRepository.save(da2);
        daDetailsRepository.save(new DaDetails(da2, sfSoftware, 10, "License Win11", BigDecimal.valueOf(150.0)));

        // DA 3: En attente Acheteur (C'est celle-ci que vous verrez à traiter)
        DaHeader da3 = new DaHeader("Renouvellement PC Portables", demandeur);
        da3.setStatut(StatutDA.EN_ATTENTE_ACHAT);
        daHeaderRepository.save(da3);
        daDetailsRepository.save(new DaDetails(da3, sfHardware, 5, "Laptop Dell Latitude", BigDecimal.valueOf(1200.0)));

        // DA 4: En attente AMG
        DaHeader da4 = new DaHeader("Achat Serveur NAS", demandeur);
        da4.setStatut(StatutDA.EN_ATTENTE_AMG);
        daHeaderRepository.save(da4);
        daDetailsRepository.save(new DaDetails(da4, sfHardware, 1, "NAS Synology", BigDecimal.valueOf(2500.0)));

        log.info("✅ Données initialisées avec succès !");
    }
}
