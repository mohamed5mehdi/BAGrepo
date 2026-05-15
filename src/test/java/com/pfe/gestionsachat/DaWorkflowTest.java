package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.AchatWorkflowOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DaWorkflowTest {

    @Autowired private AchatWorkflowOrchestrator orchestrator;
    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private UserRepository userRepository;

    private User n1;

    @BeforeEach
    void setup() {
        n1 = userRepository.findByEmail("n1@test.com").orElseThrow();
    }

    @Test
    void testWorkflowRejection() {
        DaHeader da = new DaHeader();
        da.setStatut(StatutDA.EN_ATTENTE_N1);
        da = daHeaderRepository.save(da);

        DaHeader rejected = orchestrator.processValidation(da.getOidDa(), n1.getOidUser(), ValidationDecision.REJETE, "Pas de budget");
        
        assertEquals(StatutDA.REJETEE, rejected.getStatut(), "La DA devrait être rejetée");
    }
}
