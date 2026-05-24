package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/purchase-orders", produces = "application/json")
public class AcheteurAutomationController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/{id}/auto-grn-grc")
    public ResponseEntity<Void> autoGenerateGrnGrc(@PathVariable @NonNull Integer id, @RequestParam @NonNull Integer userId) {
        User acheteur = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        purchaseOrderService.autoGenerateGrnGrc(id, acheteur);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending-grn-internal")
    public ResponseEntity<java.util.List<com.pfe.gestionsachat.model.PurchaseOrder>> getPendingInternalPOs() {
        return ResponseEntity.ok(purchaseOrderService.getPendingInternalPOsForAutomation());
    }
}
