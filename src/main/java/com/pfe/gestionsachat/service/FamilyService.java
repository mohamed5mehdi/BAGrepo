package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.repository.FamilyRepository;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FamilyService {

    @Autowired
    private FamilyRepository familyRepository;

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

    @NonNull
    public Family updateFamily(@NonNull Integer id, @NonNull Family familyDetails) {
        Family family = getFamilyById(id);
        family.setLibelle(familyDetails.getLibelle());
        family.setBudgetInitial(familyDetails.getBudgetInitial());
        family.setBudgetRestant(familyDetails.getBudgetRestant());
        return familyRepository.save(family);
    }

    public void deleteFamily(@NonNull Integer id) {
        familyRepository.deleteById(id);
    }
}