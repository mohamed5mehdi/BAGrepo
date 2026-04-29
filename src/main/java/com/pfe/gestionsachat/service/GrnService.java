package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class GrnService {
    @Autowired
    private GrnHeaderRepository grnRepository;
    @Autowired
    private WarehouseService warehouseService;

    @Transactional
    public GrnHeader createGrn(GrnHeader grn) {
        grn.setStatus(GrnStatus.DRAFT);
        if (grn.getDetails() != null) {
            grn.getDetails().forEach(d -> d.setGrnHeader(grn));
        }
        return grnRepository.save(grn);
    }

    @Transactional
    public GrnHeader validateGrn(@org.springframework.lang.NonNull Long grnId) {
        GrnHeader grn = grnRepository.findById(grnId).orElseThrow();
        grn.setStatus(GrnStatus.VALIDATED);

        for (GrnDetails detail : grn.getDetails()) {
            if (detail.getQualityStatus() == QualityStatus.APPROVED) {
                // Flux 1 : IN (Physique) - On ajoute uniquement ce qui est accepté
                warehouseService.addStock(detail.getItemCode(), detail.getAcceptedQuantity(), "GRN-" + grn.getId());
            } 
            // Les pièces rejetées ne sont pas ajoutées au stock 'Available', donc pas besoin de removeStock.
            // On pourrait ajouter un mouvement 'REJECTED' sans impact sur la quantité available si besoin.
        }
        return grnRepository.save(grn);
    }
}
