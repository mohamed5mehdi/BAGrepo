package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SubFamilyService {

    private static final Logger log = LoggerFactory.getLogger(SubFamilyService.class);

    @Autowired
    private SubFamilyRepository subFamilyRepository;

    public SubFamily createSubFamily(@NonNull SubFamily subFamily) {
        return subFamilyRepository.save(subFamily);
    }

    public List<SubFamily> getAllSubFamilies() {
        return subFamilyRepository.findAll();
    }

    @NonNull
    public SubFamily getSubFamilyById(@NonNull Integer id) {
        return java.util.Objects.requireNonNull(subFamilyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sous-famille non trouvée")));
    }

    public List<SubFamily> getSubFamiliesByFamily(@NonNull Integer familyId) {
        log.debug("Querying sub-families for familyId = {}", familyId);
        return subFamilyRepository.findByFamilyId(familyId);
    }

    public List<SubFamily> getSubFamiliesByFamilyName(String familyName) {
        log.debug("Querying sub-families for familyName = {}", familyName);
        return subFamilyRepository.findByFamilyLibelle(familyName);
    }

    @NonNull
    public SubFamily updateSubFamily(@NonNull Integer id, @NonNull SubFamily subFamilyDetails) {
        SubFamily subFamily = getSubFamilyById(id);
        subFamily.setLibelle(subFamilyDetails.getLibelle());
        subFamily.setBudgetInitial(subFamilyDetails.getBudgetInitial());
        subFamily.setBudgetRestant(subFamilyDetails.getBudgetRestant());
        return subFamilyRepository.save(subFamily);
    }

    public void deleteSubFamily(@NonNull Integer id) {
        subFamilyRepository.deleteById(id);
    }
}