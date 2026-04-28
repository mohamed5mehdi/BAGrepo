package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.DaHeaderRepository;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class DaHeaderService {

    @Autowired
    private DaHeaderRepository daHeaderRepository;

    public DaHeader createPurchaseRequest(@NonNull DaHeader request) {
        // Forcer le statut initial selon le diagramme
        request.setStatut(StatutDA.EN_ATTENTE_N1);
        
        // S'assurer que la date est renseignée
        if (request.getDateCreation() == null) {
            request.setDateCreation(java.time.LocalDate.now());
        }

        // Liaison bidirectionnelle pour les détails
        if (request.getDetails() != null) {
            for (DaDetails detail : request.getDetails()) {
                detail.setDaHeader(request);
            }
        }
        
        return daHeaderRepository.save(request);
    }

    public DaHeader submitPurchaseRequest(@NonNull Integer id) {
        DaHeader request = getPurchaseRequestById(id);
        request.soumettre();
        return daHeaderRepository.save(request);
    }

    public List<DaHeader> getAllPurchaseRequests() {
        return daHeaderRepository.findAll();
    }

    @NonNull
    public DaHeader getPurchaseRequestById(Integer id) {
        if (id == null) throw new IllegalArgumentException("ID cannot be null");
        return java.util.Objects.requireNonNull(daHeaderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande d'achat non trouvée avec ID: " + id)));
    }

    public List<DaHeader> getPurchaseRequestsByStatus(StatutDA statut) {
        return daHeaderRepository.findByStatut(statut);
    }

    public List<DaHeader> getPurchaseRequestsByDemandeur(@NonNull Integer oidUser) {
        return daHeaderRepository.findByDemandeur_OidUser(oidUser);
    }

    public DaHeader updatePurchaseRequest(@NonNull Integer id, @NonNull DaHeader requestDetails) {
        DaHeader request = getPurchaseRequestById(id);
        request.setObjet(requestDetails.getObjet());
        if (requestDetails.getDetails() != null) {
            // S'assurer de la liaison bidirectionnelle pour chaque détail
            for (DaDetails detail : requestDetails.getDetails()) {
                detail.setDaHeader(request);
            }
            request.getDetails().clear();
            request.getDetails().addAll(requestDetails.getDetails());
        }
        return daHeaderRepository.save(request);
    }

    public void deletePurchaseRequest(@NonNull Integer id) {
        DaHeader request = getPurchaseRequestById(id);
        daHeaderRepository.delete(request);
    }

    public List<DaHeader> searchPurchaseRequests(String keyword) {
        return daHeaderRepository.searchByKeyword(keyword);
    }

    public long countByStatut(StatutDA statut) {
        return daHeaderRepository.countByStatut(statut);
    }
}