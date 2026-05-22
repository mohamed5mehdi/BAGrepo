package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.dto.CreditNoteRequest;
import com.pfe.gestionsachat.dto.ItemQuantityRejection;
import com.pfe.gestionsachat.model.CreditNote;
import com.pfe.gestionsachat.model.CreditNoteStatus;
import com.pfe.gestionsachat.model.DaDetails;
import com.pfe.gestionsachat.model.GrnHeader;
import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.model.MovementType;
import com.pfe.gestionsachat.model.StockItem;
import com.pfe.gestionsachat.model.StockMovement;
import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.repository.CreditNoteRepository;
import com.pfe.gestionsachat.repository.FamilyRepository;
import com.pfe.gestionsachat.repository.GrnHeaderRepository;
import com.pfe.gestionsachat.repository.StockItemRepository;
import com.pfe.gestionsachat.repository.StockMovementRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class CreditNoteService {

    @Autowired private CreditNoteRepository creditNoteRepository;
    @Autowired private GrnHeaderRepository grnHeaderRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private FamilyRepository familyRepository;

    @Transactional
    public CreditNote processCreditNote(CreditNoteRequest request) {
        GrnHeader grn = grnHeaderRepository.findById(request.getGrnHeaderId())
                .orElseThrow(() -> new RuntimeException("GRN introuvable : " + request.getGrnHeaderId()));

        CreditNote creditNote = new CreditNote();
        creditNote.setGrnHeader(grn);
        creditNote.setCreditNoteNumber(request.getCreditNoteNumber());
        creditNote.setCreditNoteDate(java.time.LocalDate.now());
        creditNote.setStatus(CreditNoteStatus.PENDING);

        BigDecimal totalCreditAmount = BigDecimal.ZERO;

        for (ItemQuantityRejection rejection : request.getRejections()) {
            // 1. Mise à jour du stock (Logistique)
            StockItem stockItem = stockItemRepository.findById(rejection.getStockItemId())
                    .orElseThrow(() -> new RuntimeException("Article en stock introuvable : " + rejection.getStockItemId()));

            stockItem.setQuantityAvailable(stockItem.getQuantityAvailable() - rejection.getQuantity());
            stockItemRepository.save(stockItem);

            // 2. Générer un StockMovement de type SORTIE (OUT_RETURN)
            StockMovement movement = new StockMovement();
            movement.setStockItem(stockItem);
            movement.setMovementType(MovementType.OUT_RETURN);
            movement.setQuantity(rejection.getQuantity());
            movement.setMovementDate(java.time.LocalDateTime.now());
            movement.setReferenceDocument("CN-" + request.getCreditNoteNumber());
            stockMovementRepository.save(movement);

            // 3. Réversibilité Budgétaire (Finance)
            // BUG #7 CORRIGÉ : utilise addBudget() qui fait engage-- ET restant++
            // L'ancien code ne faisait que restant++ sans toucher à engage, rompant l'équation.
            SubFamily subFamily = findSubFamilyForItem(grn, stockItem.getItemCode());
            if (subFamily != null) {
                BigDecimal unitPrice = rejection.getUnitPrice();
                BigDecimal amountToCredit = unitPrice.multiply(BigDecimal.valueOf(rejection.getQuantity()));

                // Lock pessimiste sur la sous-famille
                SubFamily lockedSf = subFamilyRepository.findByIdWithLock(subFamily.getOidSub())
                        .orElseThrow(() -> new RuntimeException("Sous-famille introuvable pour lock: " + subFamily.getOidSub()));

                // BUG #7 CORRIGÉ : addBudget() maintient l'invariant initial = engage + restant
                lockedSf.addBudget(amountToCredit);
                subFamilyRepository.save(lockedSf);

                // BUG #7 CORRIGÉ : Lock pessimiste sur la famille (l'ancien code accédait via proxy Lazy sans lock)
                if (lockedSf.getFamily() != null) {
                    Family lockedFamily = familyRepository.findByIdWithLock(
                            lockedSf.getFamily().getIdFamily())
                            .orElseThrow(() -> new RuntimeException(
                                    "Famille introuvable pour lock: " + lockedSf.getFamily().getIdFamily()));
                    lockedFamily.addBudget(amountToCredit);
                    familyRepository.save(lockedFamily);
                }

                totalCreditAmount = totalCreditAmount.add(amountToCredit);
            }
        }

        creditNote.setMontant(totalCreditAmount);
        creditNote.setStatus(CreditNoteStatus.COMPLETED);
        return creditNoteRepository.save(creditNote);
    }

    private SubFamily findSubFamilyForItem(GrnHeader grn, String itemCode) {
        if (grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getDaHeader() != null) {
            return grn.getPurchaseOrder().getDaHeader().getDetails().stream()
                    .filter(d -> itemCode != null && itemCode.equals(d.getItemCode()))
                    .map(DaDetails::getSubFamily)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
