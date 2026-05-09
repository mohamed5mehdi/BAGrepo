package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class LogisticsFlowTest {

    @Autowired private GrnService grnService;
    @Autowired private GrcService grcService;
    @Autowired private MatchingService matchingService;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private GrnHeaderRepository grnRepository;
    @Autowired private GrcHeaderRepository grcRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;

    private Warehouse warehouse;
    private StockItem stockItem;
    private PurchaseOrder po;

    @BeforeEach
    void setup() {
        warehouse = new Warehouse();
        warehouse.setName("Main Depot");
        warehouse.setType(WarehouseType.CENTRAL);
        warehouse = warehouseRepository.save(warehouse);

        stockItem = new StockItem();
        stockItem.setItemCode("PART-123");
        stockItem.setItemName("Plaquettes de frein");
        stockItem.setQuantityAvailable(50);
        stockItem.setReorderPoint(20);
        stockItem.setWarehouse(warehouse);
        stockItem = stockItemRepository.save(java.util.Objects.requireNonNull(stockItem));

        Supplier supplier = new Supplier("BAG Supplier", "Contact", "Adresse", "LOGISTIQUE", 5, 2);
        supplier = supplierRepository.save(supplier);

        po = new PurchaseOrder();
        po.setStatut("OPEN");
        po.setMontantTotal(new java.math.BigDecimal("1000.00"));
        po = purchaseOrderRepository.save(po);
    }

    @Test
    void testFlux1AndFlux2_GrnGrcInvoice() {
        // --- ETAPE 1: Reception Physique (GRN) ---
        GrnHeader grn = new GrnHeader();
        grn.setPurchaseOrder(po);
        grn.setReceiptDate(LocalDate.now());
        grn.setStatus(GrnStatus.DRAFT);
        grn = grnRepository.save(grn);

        GrnDetails detail = new GrnDetails();
        detail.setGrnHeader(grn);
        detail.setItemCode("PART-123");
        detail.setOrderedQuantity(10);
        detail.setReceivedQuantity(10);
        detail.setAcceptedQuantity(8);
        detail.setRejectedQuantity(2);
        // On marque 8 acceptées, 2 rejetées -> Le statut PENDING passera à APPROVED pour les acceptées, REJECTED pour les rejetées.
        // Dans notre logique simplifiée, on va mettre APPROVED et supposer que la quantité est 8.
        // On pourrait faire deux lignes (une pour APPROVED, une pour REJECTED). Faisons cela:
        
        GrnDetails detailApproved = new GrnDetails();
        detailApproved.setGrnHeader(grn);
        detailApproved.setItemCode("PART-123");
        detailApproved.setAcceptedQuantity(8);
        detailApproved.setQualityStatus(QualityStatus.APPROVED);

        GrnDetails detailRejected = new GrnDetails();
        detailRejected.setGrnHeader(grn);
        detailRejected.setItemCode("PART-123");
        detailRejected.setAcceptedQuantity(0); // Pas accepté
        detailRejected.setRejectedQuantity(2);
        detailRejected.setQualityStatus(QualityStatus.REJECTED);

        grn.setDetails(new java.util.ArrayList<>(List.of(detailApproved, detailRejected)));
        grn = grnRepository.save(grn);

        // Validation GRN -> Mouvements de stock
        GrnHeader validatedGrn = grnService.validateGrn(java.util.Objects.requireNonNull(grn.getId()));
        assertEquals(GrnStatus.VALIDATED, validatedGrn.getStatus());

        // Verifier le stock IN pour les 8 pièces
        StockItem updatedStock = stockItemRepository.findByItemCode("PART-123").get(0);
        assertEquals(58, updatedStock.getQuantityAvailable()); // 50 + 8

        // --- ETAPE 2: GRC (Costing) ---
        GrcHeader grc = new GrcHeader();
        grc.setGrnHeader(validatedGrn);
        grc.setStatus(GrcStatus.DRAFT);
        grc = grcRepository.save(grc);

        GrcDetails grcDetail = new GrcDetails();
        grcDetail.setGrcHeader(grc);
        // Utiliser le detail déjà présent dans le GRN validé pour éviter l'erreur de transient
        GrnDetails managedDetail = validatedGrn.getDetails().stream()
                .filter(d -> d.getQualityStatus() == QualityStatus.APPROVED)
                .findFirst().orElseThrow();
        grcDetail.setGrnDetail(managedDetail);
        grcDetail.setAcceptedQuantity(8);
        grcDetail.setItemCode("PART-123"); // Important pour GrcService
        grcDetail.setUnitCost(100.0);
        grcDetail.setTaxRate(20.0);
        grc.setDetails(new java.util.ArrayList<>(List.of(grcDetail)));
        grc = grcService.createGrc(grc);

        // Validation GRC -> Calcul totalCost (incluant 20% TVA par défaut si non spécifié, ici on le spécifie)
        GrcHeader validatedGrc = grcService.validateGrc(java.util.Objects.requireNonNull(grc.getId()));
        assertEquals(GrcStatus.VALIDATED, validatedGrc.getStatus());
        // 8 items * 100 * 1.20 = 960.0
        assertEquals(java.math.BigDecimal.valueOf(960.0).setScale(2), validatedGrc.getTotalAmount().setScale(2));

        // --- ETAPE 3: Facturation et Matching ---
        Invoice invoice = new Invoice();
        invoice.setGrnHeader(validatedGrn);
        invoice.setPurchaseOrder(po);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setMontantHT(java.math.BigDecimal.valueOf(800.0));
        invoice.setMontantTTC(java.math.BigDecimal.valueOf(960.0));
        invoice.setStatus(InvoiceStatus.RECEIVED);
        invoice = invoiceRepository.save(invoice);

        // Adjust PO amount to match GRC (TTC) for the test
        po.setMontantTotal(java.math.BigDecimal.valueOf(960.0));
        purchaseOrderRepository.save(java.util.Objects.requireNonNull(po));

        // 3-Way Match -> MATCHED
        Invoice matchedInvoice = matchingService.matchInvoice(java.util.Objects.requireNonNull(invoice.getId()));
        assertEquals(InvoiceStatus.MATCHED, matchedInvoice.getStatus());

        // DAF/Finance Approval -> APPROVED
        Invoice approvedInvoice = matchingService.approveInvoice(invoice.getId());
        assertEquals(InvoiceStatus.APPROVED, approvedInvoice.getStatus());

        System.out.println("TEST SUCCESS: Flux 1 et Flux 2 validés étape par étape.");
    }
}
