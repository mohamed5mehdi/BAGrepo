package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.DaHeader;
import com.pfe.gestionsachat.model.DaDetails;
import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.model.Warehouse;
import com.pfe.gestionsachat.model.WarehouseType;
import com.pfe.gestionsachat.model.StockItem;
import com.pfe.gestionsachat.model.Supplier;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.StatutDA;
import com.pfe.gestionsachat.model.ValidationDecision;
import com.pfe.gestionsachat.model.PurchaseOrder;
import com.pfe.gestionsachat.model.GrnHeader;
import com.pfe.gestionsachat.model.GrnDetails;
import com.pfe.gestionsachat.model.GrnStatus;
import com.pfe.gestionsachat.model.QualityStatus;
import com.pfe.gestionsachat.model.GrcHeader;
import com.pfe.gestionsachat.model.GrcDetails;
import com.pfe.gestionsachat.model.GrcStatus;
import com.pfe.gestionsachat.model.Invoice;
import com.pfe.gestionsachat.model.InvoiceStatus;
import com.pfe.gestionsachat.repository.DaHeaderRepository;
import com.pfe.gestionsachat.repository.DaDetailsRepository;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import com.pfe.gestionsachat.repository.FamilyRepository;
import com.pfe.gestionsachat.repository.WarehouseRepository;
import com.pfe.gestionsachat.repository.StockItemRepository;
import com.pfe.gestionsachat.repository.InvoiceRepository;
import com.pfe.gestionsachat.repository.SupplierRepository;
import com.pfe.gestionsachat.service.AchatWorkflowOrchestrator;
import com.pfe.gestionsachat.service.GrnService;
import com.pfe.gestionsachat.service.GrcService;
import com.pfe.gestionsachat.service.MatchingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EndToEndProcurementTest {

    @Autowired private AchatWorkflowOrchestrator orchestrator;
    @Autowired private GrnService grnService;
    @Autowired private GrcService grcService;
    @Autowired private MatchingService matchingService;
    @Autowired private com.pfe.gestionsachat.service.PurchaseOrderService purchaseOrderService;

    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private DaDetailsRepository daDetailsRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private SupplierRepository supplierRepository;

    @Test
    void testFullCycle_DaToInvoice() {
        System.out.println("🚀 Démarrage du test End-to-End : de la DA à la Facture");

        // 1. Initialisation des données de base
        Family family = familyRepository.save(new Family("Pièces Moteur", BigDecimal.valueOf(50000.0)));
        SubFamily subFamily = subFamilyRepository.save(new SubFamily("Pistons", BigDecimal.valueOf(20000.0), family));
        
        userRepository.findAll().forEach(u -> {
            u.setWarehouse(null);
            userRepository.save(u);
        });
        stockItemRepository.deleteAll();
        warehouseRepository.deleteAll();

        Warehouse warehouse = new Warehouse();
        warehouse.setName("Entrepôt Central BAG");
        warehouse.setLocation("Casablanca");
        warehouse.setType(WarehouseType.CENTRAL);
        warehouse = warehouseRepository.save(warehouse);

        StockItem stockItem = new StockItem();
        stockItem.setWarehouse(warehouse);
        stockItem.setItemCode("PIST-001");
        stockItem.setItemName("Piston Kit");
        stockItem.setQuantityAvailable(10);
        stockItem = stockItemRepository.save(stockItem);

        Supplier supplier = supplierRepository.save(new Supplier("Youssef Idrissi Parts", "Youssef", "Casa", "MECANIQUE", 4, 3));

        User n1 = userRepository.findByEmail("n1@test.com").orElseThrow();
        User tech = userRepository.findByEmail("tech@test.com").orElseThrow();
        User acheteur = userRepository.findByEmail("acheteur@test.com").orElseThrow();
        User amg = userRepository.findByEmail("amg@test.com").orElseThrow();
        User dg = userRepository.findByEmail("dg@test.com").orElseThrow();
        User demandeur = userRepository.findByEmail("demandeur@test.com").orElseThrow();

        // 2. Création de la Demande d'Achat (DA)
        DaHeader da = new DaHeader("Besoin Urgent Pistons", demandeur);
        da.setStatut(StatutDA.EN_ATTENTE_N1);
        da = daHeaderRepository.save(da);

        DaDetails daDetail = new DaDetails(da, subFamily, 10, "Pistons", BigDecimal.valueOf(100.0));
        daDetail.setItemCode("PIST-001");
        daDetail.setFournisseur(supplier);
        daDetailsRepository.save(daDetail);
        da.setDetails(new ArrayList<>(List.of(daDetail)));

        Integer daId = Objects.requireNonNull(da.getOidDa());
        // 3. Workflow de Validation complet : N1 -> TECH -> Budget(AMG) -> DAF -> DG -> VALIDEE
        orchestrator.processValidation(daId, Objects.requireNonNull(n1.getOidUser()), ValidationDecision.ACCEPTE, "Approuvé N1");
        assertEquals(StatutDA.EN_ATTENTE_TECH, daHeaderRepository.findById(daId).orElseThrow().getStatut());

        orchestrator.processValidation(daId, Objects.requireNonNull(tech.getOidUser()), ValidationDecision.ACCEPTE, "Approuvé Tech");
        assertEquals(StatutDA.EN_ATTENTE_ACHAT, daHeaderRepository.findById(daId).orElseThrow().getStatut());

        orchestrator.verifierBudget(daId, Objects.requireNonNull(acheteur.getOidUser()));
        assertEquals(StatutDA.EN_ATTENTE_AMG, daHeaderRepository.findById(daId).orElseThrow().getStatut());

        orchestrator.processValidation(daId, Objects.requireNonNull(amg.getOidUser()), ValidationDecision.ACCEPTE, "Approuvé AMG");
        assertEquals(StatutDA.EN_ATTENTE_DAF, daHeaderRepository.findById(daId).orElseThrow().getStatut());

        User daf = userRepository.findByEmail("daf@test.com").orElseThrow();
        orchestrator.processValidation(daId, Objects.requireNonNull(daf.getOidUser()), ValidationDecision.ACCEPTE, "Approuvé DAF");
        assertEquals(StatutDA.EN_ATTENTE_DG, daHeaderRepository.findById(daId).orElseThrow().getStatut());

        orchestrator.processValidation(daId, Objects.requireNonNull(dg.getOidUser()), ValidationDecision.ACCEPTE, "Approuvé DG (Superviseur Abdelhamid)");
        assertEquals(StatutDA.VALIDEE, daHeaderRepository.findById(daId).orElseThrow().getStatut());
        System.out.println("✅ Étape 1 : DA validée par le circuit complet.");

        // 4. Génération du Bon de Commande (PO)
        PurchaseOrder po = orchestrator.manualCreatePO(daId, Objects.requireNonNull(acheteur.getOidUser()));
        assertNotNull(po);
        // po = purchaseOrderService.submitForApproval(po.getIdPo(), acheteur);
        // po = purchaseOrderService.approvePO(po.getIdPo(), daf, "OK PO");
        assertTrue(new BigDecimal("1200.00").compareTo(po.getMontantTotal()) == 0);
        assertEquals(StatutDA.PO_CREE, daHeaderRepository.findById(daId).orElseThrow().getStatut());
        System.out.println("✅ Étape 2 : PO généré avec succès (Montant TTC: 1200.00).");

        // 5. Réception Physique (GRN)
        GrnHeader grn = new GrnHeader();
        grn.setPurchaseOrder(po);
        grn.setSupplier(supplier);
        grn.setDeliveryNoteNumber("BL-2024-001");
        grn.setReceiptDate(java.time.LocalDate.now());
        grn.setStatus(GrnStatus.PENDING);

        GrnDetails grnDetail = new GrnDetails();
        grnDetail.setGrnHeader(grn);
        grnDetail.setItemCode("PIST-001");
        grnDetail.setItemName("Pistons");
        grnDetail.setOrderedQuantity(10);
        grnDetail.setReceivedQuantity(10);
        grnDetail.setAcceptedQuantity(10);
        grnDetail.setQualityStatus(QualityStatus.APPROVED);
        grn.setDetails(new ArrayList<>(List.of(grnDetail)));

        grn = grnService.createGrn(grn);
        GrnHeader validatedGrn = grnService.validateGrn(Objects.requireNonNull(grn.getId()));

        assertEquals(GrnStatus.ENTRY_COMPLETED, validatedGrn.getStatus());
        assertEquals(20, stockItemRepository.findByItemCode("PIST-001").get(0).getQuantityAvailable()); // 10 initial + 10 reçus
        System.out.println("✅ Étape 3 : Réception physique (GRN) effectuée et stock mis à jour (Total: 20).");

        // 6. Valorisation Financière (GRC)
        GrcHeader grc = new GrcHeader();
        grc.setGrnHeader(validatedGrn);
        grc.setStatus(GrcStatus.PENDING_APPROVAL);
        grc.setCostingDate(java.time.LocalDate.now());

        GrcDetails grcDetail = new GrcDetails();
        grcDetail.setGrcHeader(grc);
        grcDetail.setGrnDetail(validatedGrn.getDetails().get(0));
        grcDetail.setItemCode("PIST-001");
        grcDetail.setAcceptedQuantity(10);
        grcDetail.setUnitCost(java.math.BigDecimal.valueOf(100.0));
        grcDetail.setTaxRate(java.math.BigDecimal.valueOf(20.0)); // 20% tax to make total 1200.0
        grc.setDetails(new ArrayList<>(List.of(grcDetail)));

        grc = grcService.createGrc(grc);
        GrcHeader validatedGrc = grcService.validateGrc(Objects.requireNonNull(grc.getId()));

        assertEquals(GrcStatus.POSTED, validatedGrc.getStatus());
        assertEquals(java.math.BigDecimal.valueOf(1200.0).setScale(2), validatedGrc.getTotalAmount().setScale(2));
        System.out.println("✅ Étape 4 : Valorisation financière (GRC) terminée.");

        // 7. Facturation et 3-Way Matching
        Invoice invoice = new Invoice();
        invoice.setPurchaseOrder(po);
        invoice.setGrnHeader(validatedGrn);
        invoice.setInvoiceNumber("FACT-999");
        invoice.setInvoiceDate(java.time.LocalDate.now());
        invoice.setMontantHT(java.math.BigDecimal.valueOf(1000.0));
        invoice.setMontantTTC(java.math.BigDecimal.valueOf(1200.0));
        invoice.setStatus(InvoiceStatus.RECEIVED);
        invoice = invoiceRepository.save(invoice);

        Invoice matchedInvoice = matchingService.matchInvoice(Objects.requireNonNull(invoice.getId()));
        assertEquals(InvoiceStatus.MATCHED, matchedInvoice.getStatus());

        Invoice approvedInvoice = matchingService.approveInvoice(invoice.getId());
        assertEquals(InvoiceStatus.APPROVED, approvedInvoice.getStatus());
        System.out.println("✅ Étape 5 : Facture reçue, matchée et approuvée via le circuit financier complet.");

        System.out.println("🏁 FIN DU TEST : Flux complet BAG validé de A à Z !");
    }
}
