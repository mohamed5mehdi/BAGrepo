package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.PurchaseOrder;
import com.pfe.gestionsachat.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import java.util.List;

@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    @NonNull
    public PurchaseOrder getPurchaseOrderById(@NonNull Integer id) {
        return java.util.Objects.requireNonNull(purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bon de commande non trouvé")));
    }

    public PurchaseOrder getPurchaseOrderByDa(@NonNull Integer oidDa) {
        return purchaseOrderRepository.findByDaHeader_OidDa(oidDa);
    }

    public List<PurchaseOrder> getPurchaseOrdersByStatus(@NonNull String statut) {
        return purchaseOrderRepository.findByStatut(statut);
    }
}