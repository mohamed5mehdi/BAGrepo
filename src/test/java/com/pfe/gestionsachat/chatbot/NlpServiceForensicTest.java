package com.pfe.gestionsachat.chatbot;

import com.pfe.gestionsachat.chatbot.model.SlotState;
import com.pfe.gestionsachat.chatbot.service.NlpService;
import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.model.UrgenceDemande;
import com.pfe.gestionsachat.repository.FamilyRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class NlpServiceForensicTest {

    @Mock
    private FamilyRepository familyRepository;
    
    @Mock
    private SubFamilyRepository subFamilyRepository;

    @InjectMocks
    private NlpService nlpService;

    private void printResult(int id, String msg, SlotState res) {
        System.out.println("=== PHRASE " + id + " ===");
        System.out.println("Message: " + msg);
        System.out.println("- designation extraite: " + (res.getDesignation() != null ? res.getDesignation() : "NULL"));
        System.out.println("- quantite extraite: " + (res.getQuantite() != null ? res.getQuantite() : "NULL"));
        System.out.println("- urgence extraite: " + (res.getUrgence() != null ? res.getUrgence() : "NULL"));
        System.out.println("- famille resolue: " + (res.getFamilyLibelle() != null ? res.getFamilyLibelle() : "NULL"));
        System.out.println("- justification extraite: " + (res.getJustification() != null ? res.getJustification() : "NULL"));
        System.out.println("- slot manquant detecte: " + res.getProchainSlotManquant());
        System.out.println("");
    }

    @Test
    public void analysePréalableSemaine2() {
        Family famIT = new Family(); famIT.setIdFamily(1); famIT.setLibelle("Matériel Informatique");
        Family famFourni = new Family(); famFourni.setIdFamily(4); famFourni.setLibelle("Fournitures & Services");
        
        Mockito.when(familyRepository.findAll()).thenReturn(Arrays.asList(famIT, famFourni));
        Mockito.lenient().when(subFamilyRepository.findByFamilyId(Mockito.anyInt())).thenReturn(Collections.emptyList());

        // Phrase 1 : context Designation set
        SlotState s1 = new SlotState(); s1.setDesignation("Ordinateurs");
        printResult(1, "3", nlpService.extractSlots("3", s1));

        // Phrase 2 : empty
        printResult(2, "c'est pas urgent du tout", nlpService.extractSlots("c'est pas urgent du tout", new SlotState()));

        // Phrase 3 : empty
        printResult(3, "bghi 3 stylos", nlpService.extractSlots("bghi 3 stylos", new SlotState()));

        // Phrase 4 : context up to subFamily
        SlotState s4 = new SlotState(); s4.setDesignation("Ordinateur"); s4.setQuantite(1); s4.setFamilyId(1); s4.setFamilyLibelle("Info"); s4.setSubFamilyId(100); s4.setSubFamilyLibelle("SubInfo");
        printResult(4, "pour la salle de réunion", nlpService.extractSlots("pour la salle de réunion", s4));

        // Phrase 5 : empty
        printResult(5, "j'ai besoin d'un vidéoprojecteur pour les formations critiques", nlpService.extractSlots("j'ai besoin d'un vidéoprojecteur pour les formations critiques", new SlotState()));
    }
}
