package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import com.pfe.gestionsachat.repository.FamilyRepository;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class SubFamilyService {

    private static final Logger log = LoggerFactory.getLogger(SubFamilyService.class);

    @Autowired
    private SubFamilyRepository subFamilyRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Transactional
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

    @Transactional
    @NonNull
    public SubFamily updateSubFamily(@NonNull Integer id, @NonNull SubFamily subFamilyDetails) {
        SubFamily subFamily = getSubFamilyById(id);
        subFamily.setLibelle(subFamilyDetails.getLibelle());
        subFamily.setBudgetInitial(subFamilyDetails.getBudgetInitial());
        java.math.BigDecimal engage = subFamily.getBudgetEngage() != null ? subFamily.getBudgetEngage() : java.math.BigDecimal.ZERO;
        subFamily.setBudgetRestant(subFamilyDetails.getBudgetInitial().subtract(engage));
        SubFamily saved = subFamilyRepository.save(subFamily);

        // PROPAGATION ASCENDANTE : recalcul du budget de la famille parente
        // CORRECTIF : le parent était mis à jour en mémoire mais jamais persisté (save manquant).
        if (saved.getFamily() != null) {
            com.pfe.gestionsachat.model.Family parent = familyRepository.findById(saved.getFamily().getIdFamily())
                    .orElseThrow(() -> new RuntimeException("Famille parente introuvable : " + saved.getFamily().getIdFamily()));
            // Re-lecture des sibling APRÈS le save pour avoir les valeurs à jour
            List<SubFamily> siblings = subFamilyRepository.findByFamilyId(parent.getIdFamily());
            java.math.BigDecimal totalInitial = siblings.stream()
                    .map(sf -> sf.getBudgetInitial() != null ? sf.getBudgetInitial() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal totalEngage = siblings.stream()
                    .map(sf -> sf.getBudgetEngage() != null ? sf.getBudgetEngage() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            parent.setBudgetInitial(totalInitial);
            parent.setBudgetEngage(totalEngage);
            parent.setBudgetRestant(totalInitial.subtract(totalEngage));
            familyRepository.save(parent); // FIX CRITIQUE : persiste la famille parente
            log.debug("Cascade budget : Famille id={} recalculée → initial={}, engage={}, restant={}",
                    parent.getIdFamily(), totalInitial, totalEngage, parent.getBudgetRestant());
        }
        return saved;
    }

    @Transactional
    public void deleteSubFamily(@NonNull Integer id) {
        subFamilyRepository.deleteById(id);
    }
}