package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.repository.FamilyRepository;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FamilyService {

    @Autowired
    private FamilyRepository familyRepository;

    @Transactional
    public Family createFamily(@NonNull Family family) {
        return familyRepository.save(family);
    }

    public List<Family> getAllFamilies() {
        return familyRepository.findAll();
    }

    @NonNull
    public Family getFamilyById(@NonNull Integer id) {
        return java.util.Objects.requireNonNull(familyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Famille non trouvée")));
    }

    @Transactional
    @NonNull
    public Family updateFamily(@NonNull Integer id, @NonNull Family familyDetails) {
        Family family = getFamilyById(id);
        family.setLibelle(familyDetails.getLibelle());
        family.setBudgetInitial(familyDetails.getBudgetInitial());
        java.math.BigDecimal engage = family.getBudgetEngage() != null ? family.getBudgetEngage() : java.math.BigDecimal.ZERO;
        family.setBudgetRestant(familyDetails.getBudgetInitial().subtract(engage));
        return familyRepository.save(family);
    }

    @Transactional
    public void deleteFamily(@NonNull Integer id) {
        familyRepository.deleteById(id);
    }
}