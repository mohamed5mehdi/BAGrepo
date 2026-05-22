package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.GrcService;
import com.pfe.gestionsachat.service.GrnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GrcNumberSyncTest {

    @Autowired private GrcService grcService;
    @Autowired private GrnService grnService;
    @Autowired private PurchaseOrderRepository poRepository;
    @Autowired private DemandeAchatInterneRepository demandeInterneRepository;
    @Autowired private GrnHeaderRepository grnRepository;

    private GrnHeader grn;

    @BeforeEach
    void setup() {
        DemandeAchatInterne demandeInterne = new DemandeAchatInterne();
        demandeInterne.setQuantite(10);
        demandeInterne = demandeInterneRepository.save(demandeInterne);

        PurchaseOrder po = new PurchaseOrder();
        po.setDemandeInterne(demandeInterne);
        po.setStatut(POStatus.APPROVED);
        po = poRepository.save(po);

        GrnHeader newGrn = new GrnHeader();
        newGrn.setPurchaseOrder(po);
        
        GrnDetails grnDetail = new GrnDetails();
        grnDetail.setItemCode("TEST-SYNC");
        grnDetail.setReceivedQuantity(10);
        grnDetail.setAcceptedQuantity(10);
        newGrn.setDetails(new java.util.ArrayList<>(List.of(grnDetail)));

        grn = grnService.createGrn(newGrn);
        grn.setStatus(GrnStatus.ENTRY_COMPLETED);
        grn.setGrnNumber("GRN-202605-00123");
        grn = grnRepository.save(grn);
    }

    @Test
    void testGrcNumberSyncsWithGrnNumber() {
        GrcHeader grc = new GrcHeader();
        grc.setGrnHeader(grn);
        
        GrcDetails detail = new GrcDetails();
        detail.setGrnDetail(grn.getDetails().get(0));
        detail.setItemCode("TEST-SYNC");
        detail.setAcceptedQuantity(10);
        detail.setUnitCost(java.math.BigDecimal.valueOf(10.0));
        grc.setDetails(new java.util.ArrayList<>(List.of(detail)));

        GrcHeader savedGrc = grcService.createGrc(grc);
        
        assertEquals("GRN-202605-00123", savedGrc.getGrcNumber());
    }

    @Test
    void testGrcCreationWithNoGrnNumberGeneratesIt() {
        grn.setGrnNumber(null);
        grn = grnRepository.save(grn);

        GrcHeader grc = new GrcHeader();
        grc.setGrnHeader(grn);
        
        GrcDetails detail = new GrcDetails();
        detail.setGrnDetail(grn.getDetails().get(0));
        detail.setItemCode("TEST-SYNC");
        detail.setAcceptedQuantity(10);
        detail.setUnitCost(java.math.BigDecimal.valueOf(10.0));
        grc.setDetails(new java.util.ArrayList<>(List.of(detail)));

        GrcHeader savedGrc = grcService.createGrc(grc);
        
        assertNotNull(savedGrc.getGrcNumber());
        assertTrue(savedGrc.getGrcNumber().startsWith("GRN-"));
        assertEquals(savedGrc.getGrcNumber(), grn.getGrnNumber());
    }
}
