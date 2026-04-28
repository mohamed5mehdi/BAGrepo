package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.Supplier;
import com.pfe.gestionsachat.repository.SupplierRepository;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    public Supplier createSupplier(@NonNull Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @NonNull
    public Supplier getSupplierById(@NonNull Integer id) {
        return java.util.Objects.requireNonNull(supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé")));
    }

    @NonNull
    public Supplier updateSupplier(@NonNull Integer id, @NonNull Supplier supplierDetails) {
        Supplier supplier = getSupplierById(id);
        supplier.setNom(supplierDetails.getNom());
        supplier.setContact(supplierDetails.getContact());
        supplier.setAdresse(supplierDetails.getAdresse());
        return supplierRepository.save(supplier);
    }

    public void deleteSupplier(@NonNull Integer id) {
        supplierRepository.deleteById(id);
    }
}