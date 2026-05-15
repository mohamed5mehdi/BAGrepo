package com.pfe.gestionsachat.model;

public enum StatutDemande {
    BROUILLON,
    SOUMISE,
    VALIDE_N1,
    VALIDE_TECH,
    VALIDE_AMG,
    VALIDE_DAF,
    VALIDE_DG,
    EN_TRAITEMENT,
    DISPONIBLE_STOCK,
    REJETEE,
    APPROUVEE,
    PO_CREE,
    EN_LIVRAISON, // Restauré pour compatibilité historique
    AFFECTEE      // Restauré pour compatibilité historique
}
