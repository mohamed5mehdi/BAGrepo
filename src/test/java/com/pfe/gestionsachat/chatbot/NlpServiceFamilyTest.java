package com.pfe.gestionsachat.chatbot;

import com.pfe.gestionsachat.chatbot.model.SlotState;
import com.pfe.gestionsachat.chatbot.service.NlpService;
import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.model.UrgenceDemande;
import com.pfe.gestionsachat.model.CategorieDemande;
import com.pfe.gestionsachat.repository.FamilyRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class NlpServiceFamilyTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private SubFamilyRepository subFamilyRepository;

    @InjectMocks
    private NlpService nlpService;

    private Family famIT;
    private Family famSoft;
    private Family famBur;
    private Family famDivers;

    private SubFamily sfLaptop;
    private SubFamily sfPeri;
    private SubFamily sfStock;
    private SubFamily sfCloud;
    private SubFamily sfOffice;
    private SubFamily sfSpec;
    private SubFamily sfMobilier;
    private SubFamily sfClim;
    private SubFamily sfFourni;
    private SubFamily sfTraiteur;

    @BeforeEach
    public void setUp() {
        // Setup Families matching DataInitializer
        famIT = new Family("Matériel Informatique", BigDecimal.valueOf(2000000.0));
        famIT.setIdFamily(1);
        famIT.setCategorie(CategorieDemande.INFORMATIQUE);

        famSoft = new Family("Licences & Logiciels", BigDecimal.valueOf(1200000.0));
        famSoft.setIdFamily(2);
        famSoft.setCategorie(CategorieDemande.INFORMATIQUE);

        famBur = new Family("Bureautique & Mobilier", BigDecimal.valueOf(800000.0));
        famBur.setIdFamily(3);
        famBur.setCategorie(CategorieDemande.BUREAUTIQUE);

        famDivers = new Family("Fournitures & Services", BigDecimal.valueOf(600000.0));
        famDivers.setIdFamily(4);
        famDivers.setCategorie(CategorieDemande.AUTRE);

        // Setup SubFamilies matching DataInitializer
        sfLaptop = new SubFamily("PC Portables & Stations", BigDecimal.valueOf(900000.0), famIT);
        sfLaptop.setOidSub(11);
        sfPeri = new SubFamily("Périphériques (Écrans, Claviers)", BigDecimal.valueOf(600000.0), famIT);
        sfPeri.setOidSub(12);
        sfStock = new SubFamily("Stockage & Serveurs", BigDecimal.valueOf(500000.0), famIT);
        sfStock.setOidSub(13);

        sfCloud = new SubFamily("Abonnements Cloud (Azure/AWS)", BigDecimal.valueOf(500000.0), famSoft);
        sfCloud.setOidSub(21);
        sfOffice = new SubFamily("Suites Bureautiques (M365)", BigDecimal.valueOf(400000.0), famSoft);
        sfOffice.setOidSub(22);
        sfSpec = new SubFamily("Logiciels Métiers", BigDecimal.valueOf(300000.0), famSoft);
        sfSpec.setOidSub(23);

        sfMobilier = new SubFamily("Bureaux & Chaises Ergonomiques", BigDecimal.valueOf(450000.0), famBur);
        sfMobilier.setOidSub(31);
        sfClim = new SubFamily("Climatisation & Aménagement", BigDecimal.valueOf(350000.0), famBur);
        sfClim.setOidSub(32);

        sfFourni = new SubFamily("Fournitures de bureau", BigDecimal.valueOf(300000.0), famDivers);
        sfFourni.setOidSub(41);
        sfTraiteur = new SubFamily("Services Traiteur & Réception", BigDecimal.valueOf(300000.0), famDivers);
        sfTraiteur.setOidSub(42);

        // Lenient stubs to allow isolated testing
        Mockito.lenient().when(familyRepository.findAll()).thenReturn(Arrays.asList(famIT, famSoft, famBur, famDivers));
        Mockito.lenient().when(subFamilyRepository.findByFamilyId(1)).thenReturn(Arrays.asList(sfLaptop, sfPeri, sfStock));
        Mockito.lenient().when(subFamilyRepository.findByFamilyId(2)).thenReturn(Arrays.asList(sfCloud, sfOffice, sfSpec));
        Mockito.lenient().when(subFamilyRepository.findByFamilyId(3)).thenReturn(Arrays.asList(sfMobilier, sfClim));
        Mockito.lenient().when(subFamilyRepository.findByFamilyId(4)).thenReturn(Arrays.asList(sfFourni, sfTraiteur));
    }

    // =========================================================================
    // FAMILLE 1 : MATÉRIEL INFORMATIQUE (ID: 1)
    // =========================================================================

    @Test
    public void testFamilyIT_Scenario1_Nominal() {
        // Prompt simple sans fioritures
        String prompt = "Je veux acheter un ordinateur";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(1, res.getFamilyId());
        assertEquals("Matériel Informatique", res.getFamilyLibelle());
        assertEquals(1, res.getQuantite()); // Par défaut "un" -> 1
        assertEquals("Ordinateur", res.getDesignation());
    }

    @Test
    public void testFamilyIT_Scenario2_QuantiteUrgence() {
        // Prompt contenant quantité et urgence explicite
        String prompt = "Acheter rapidement 5 laptops pour le service informatique";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(1, res.getFamilyId());
        assertEquals("Matériel Informatique", res.getFamilyLibelle());
        assertEquals(5, res.getQuantite());
        assertEquals(UrgenceDemande.URGENTE, res.getUrgence());
    }

    @Test
    public void testFamilyIT_Scenario3_SousFamille() {
        // Prompt contenant des mots-clés d'une sous-famille (Périphériques)
        String prompt = "Besoin d'une nouvelle souris et un écran de rechange";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(1, res.getFamilyId());
        assertEquals(12, res.getSubFamilyId()); // Périphériques (Écrans, Claviers)
        assertEquals("Périphériques (Écrans, Claviers)", res.getSubFamilyLibelle());
    }

    @Test
    public void testFamilyIT_Scenario4_FuzzyMatching() {
        // Test de robustesse / fuzzy matching avec accent correct
        String prompt = "Demande urgente de matériel informatique";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(1, res.getFamilyId());
        assertEquals("Matériel Informatique", res.getFamilyLibelle());
        assertEquals(UrgenceDemande.URGENTE, res.getUrgence());
    }

    // =========================================================================
    // FAMILLE 2 : LICENCES & LOGICIELS (ID: 2)
    // =========================================================================

    @Test
    public void testFamilySoft_Scenario1_Nominal() {
        String prompt = "Je veux renouveler nos licences logicielles";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(2, res.getFamilyId());
        assertEquals("Licences & Logiciels", res.getFamilyLibelle());
        assertNull(res.getUrgence()); // Pas d'urgence spécifiée
    }

    @Test
    public void testFamilySoft_Scenario2_QuantiteUrgence() {
        String prompt = "Commander rapidement 10 abonnements office";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(2, res.getFamilyId());
        assertEquals(10, res.getQuantite());
        assertEquals(UrgenceDemande.URGENTE, res.getUrgence());
    }

    @Test
    public void testFamilySoft_Scenario3_SousFamille() {
        String prompt = "Besoin d'un hébergement cloud sur Azure";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(2, res.getFamilyId());
        assertEquals(21, res.getSubFamilyId()); // Abonnements Cloud (Azure/AWS)
    }

    @Test
    public void testFamilySoft_Scenario4_FuzzyMatching() {
        String prompt = "Acheter licences m365";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(2, res.getFamilyId());
        assertEquals(23, res.getSubFamilyId()); // Suites Bureautiques (M365)
    }

    // =========================================================================
    // FAMILLE 3 : BUREAUTIQUE & MOBILIER (ID: 3)
    // =========================================================================

    @Test
    public void testFamilyBur_Scenario1_Nominal() {
        String prompt = "Acheter un bureau en bois";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(3, res.getFamilyId());
        assertEquals("Bureautique & Mobilier", res.getFamilyLibelle());
        assertEquals(1, res.getQuantite());
    }

    @Test
    public void testFamilyBur_Scenario2_QuantiteUrgence() {
        String prompt = "Acheter 3 climatiseurs clim rapidement";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(3, res.getFamilyId());
        assertEquals(3, res.getQuantite());
        assertEquals(UrgenceDemande.URGENTE, res.getUrgence());
    }

    @Test
    public void testFamilyBur_Scenario3_SousFamille() {
        String prompt = "Commander une chaise ergonomique pour l'open space";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(3, res.getFamilyId());
        assertEquals(31, res.getSubFamilyId()); // Bureaux & Chaises Ergonomiques
    }

    @Test
    public void testFamilyBur_Scenario4_FuzzyMatching() {
        // Fuzzy matching sur mobilier
        String prompt = "Je veux renouveler notre mobilier";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(3, res.getFamilyId());
        assertEquals("Bureautique & Mobilier", res.getFamilyLibelle());
    }

    // =========================================================================
    // FAMILLE 4 : FOURNITURES & SERVICES (ID: 4)
    // =========================================================================

    @Test
    public void testFamilyDivers_Scenario1_Nominal() {
        String prompt = "Acheter du papier d'impression";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(4, res.getFamilyId());
        assertEquals("Fournitures & Services", res.getFamilyLibelle());
    }

    @Test
    public void testFamilyDivers_Scenario2_QuantiteUrgence() {
        String prompt = "Commander 100 stylos (stylo) billes bleus critiques";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(4, res.getFamilyId());
        assertEquals(100, res.getQuantite());
        assertEquals(UrgenceDemande.CRITIQUE, res.getUrgence());
    }

    @Test
    public void testFamilyDivers_Scenario3_SousFamille() {
        String prompt = "Faire appel à un traiteur pour le déjeuner de demain";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(4, res.getFamilyId());
        assertEquals(42, res.getSubFamilyId()); // Services Traiteur & Réception
    }

    @Test
    public void testFamilyDivers_Scenario4_FuzzyMatching() {
        String prompt = "Acheter du cafe et de l'eau";
        SlotState res = nlpService.extractSlots(prompt, new SlotState());

        assertEquals(4, res.getFamilyId());
        assertEquals(42, res.getSubFamilyId()); // café/eau correspondent à Services Traiteur & Réception
    }
}
