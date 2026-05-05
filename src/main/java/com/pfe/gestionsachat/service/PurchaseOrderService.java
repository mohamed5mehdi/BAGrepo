package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public PurchaseOrder getPurchaseOrderById(Integer id) {
        return purchaseOrderRepository.findById(id).orElseThrow(() -> new RuntimeException("PO non trouvé"));
    }

    public PurchaseOrder getPurchaseOrderByDa(Integer oidDa) {
        return purchaseOrderRepository.findByDaHeader_OidDa(oidDa);
    }

    public List<PurchaseOrder> getPurchaseOrdersByStatus(String statut) {
        return purchaseOrderRepository.findByStatut(statut);
    }

    @Transactional
    public PurchaseOrder generateFromInternal(DemandeAchatInterne demande) {
        BigDecimal montantHt = demande.getMontantEstime();
        BigDecimal tva = montantHt.multiply(new BigDecimal("0.20"));
        BigDecimal montantTtc = montantHt.add(tva).setScale(2, RoundingMode.HALF_UP);

        PurchaseOrder po = new PurchaseOrder();
        po.setDemandeInterne(demande);
        po.setFournisseur(demande.getFournisseur());
        po.setStatut("VALIDEE");
        po.setMontantTotal(montantTtc);
        po.setDateCreation(LocalDate.now());

        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder generateFromClassic(DaHeader da) {
        if (da.getDetails() == null || da.getDetails().isEmpty()) {
            throw new IllegalArgumentException("Impossible de créer un PO : la DA [" + da.getOidDa() + "] n'a aucun détail.");
        }

        BigDecimal totalHt = da.getDetails().stream()
                .filter(d -> d.getPrixUnitaire() != null && d.getQuantite() != null)
                .map(d -> d.getPrixUnitaire().multiply(BigDecimal.valueOf(d.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montantTtc = totalHt.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.HALF_UP);

        Supplier fournisseur = da.getDetails().stream()
                .map(DaDetails::getFournisseur)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);

        PurchaseOrder po = new PurchaseOrder();
        po.setDaHeader(da);
        po.setFournisseur(fournisseur);
        po.setStatut("VALIDEE");
        po.setMontantTotal(montantTtc);
        po.setDateCreation(LocalDate.now());

        return purchaseOrderRepository.save(po);
    }
}