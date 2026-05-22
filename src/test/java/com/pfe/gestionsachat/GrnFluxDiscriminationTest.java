package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.GrnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GrnFluxDiscriminationTest {

    @Autowired private GrnService grnService;
    @Autowired private PurchaseOrderRepository poRepository;
    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private DemandeAchatInterneRepository demandeInterneRepository;
    @Autowired private DaDetailsRepository daDetailsRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private UserRepository userRepository;

    private PurchaseOrder poClassic;
    private PurchaseOrder poInterne;

    @BeforeEach
    void setup() {
        userRepository.findAll().forEach(u -> {
            u.setWarehouse(null);
            userRepository.save(u);
        });
        stockItemRepository.deleteAll();
        warehouseRepository.deleteAll();

        Warehouse warehouse = new Warehouse();
        warehouse.setName("Main Depot");
        warehouse.setType(WarehouseType.CENTRAL);
        warehouseRepository.save(warehouse);

        // Flux 1 Setup
        DaHeader da = new DaHeader();
        da = daHeaderRepository.save(da);
        
        DaDetails detail = new DaDetails();
        detail.setDaHeader(da);
        detail.setItemCode("VALID-123");
        detail.setQuantite(10);
        daDetailsRepository.save(detail);
        da.setDetails(new ArrayList<>(List.of(detail)));
        daHeaderRepository.save(da);

        poClassic = new PurchaseOrder();
        poClassic.setDaHeader(da);
        poClassic.setStatut(POStatus.APPROVED);
        poClassic = poRepository.save(poClassic);

        // Flux 2 Setup
        DemandeAchatInterne demandeInterne = new DemandeAchatInterne();
        demandeInterne.setQuantite(20);
        demandeInterne = demandeInterneRepository.save(demandeInterne);

        poInterne = new PurchaseOrder();
        poInterne.setDemandeInterne(demandeInterne);
        poInterne.setStatut(POStatus.APPROVED);
        poInterne = poRepository.save(poInterne);
    }

    @Test
    void testFlux1_ItemCodeMismatch_ThrowsException() {
        GrnHeader grn = new GrnHeader();
        grn.setPurchaseOrder(poClassic);
        
        GrnDetails grnDetail = new GrnDetails();
        grnDetail.setItemCode("INVALID-456"); // Mismatch
        grnDetail.setReceivedQuantity(5);
        grn.setDetails(new ArrayList<>(List.of(grnDetail)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            grnService.createGrn(grn);
        });
        assertTrue(ex.getMessage().contains("introuvable dans le PO"));
    }

    @Test
    void testFlux2_ItemCodeLibre_CreatesStockItem() {
        GrnHeader grn = new GrnHeader();
        grn.setPurchaseOrder(poInterne);
        
        GrnDetails grnDetail = new GrnDetails();
        grnDetail.setItemCode("NEW-ITEM-999");
        grnDetail.setReceivedQuantity(5);
        grnDetail.setAcceptedQuantity(5);
        grnDetail.setQualityStatus(QualityStatus.APPROVED);
        grn.setDetails(new ArrayList<>(List.of(grnDetail)));

        GrnHeader savedGrn = grnService.createGrn(grn);
        assertNotNull(savedGrn);

        // Simulate completeGrnEntry which adds stock
        grnService.completeGrnEntry(savedGrn.getId(), null);

        List<StockItem> items = stockItemRepository.findByItemCode("NEW-ITEM-999");
        assertEquals(1, items.size());
        assertEquals(ItemCategory.PIECE_RECHANGE, items.get(0).getCategory());
        assertEquals(5, items.get(0).getQuantityAvailable());
    }
}
