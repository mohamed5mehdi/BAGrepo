package com.pfe.gestionsachat.model;

/**
 * Enumération des rôles de l'application (RBAC).
 * Les méthodes utilitaires centralisent la logique d'autorisation.
 */
public enum Role {
    EMPLOYE,
    MANAGER_N1,
    MANAGER_N2,
    TECHNICIEN,
    ACHETEUR,
    ACHETEUR_INFORMATIQUE,
    ACHETEUR_BUREAUTIQUE,
    ACHETEUR_MOBILIER,
    ACHETEUR_CONSOMMABLE,
    ACHETEUR_AUTRE,
    /** Responsable Service Achat — BAG ERP : approuve le PO */
    RESP_ACHAT,
    /** Magasinier générique — BAG ERP : reçoit les marchandises et génère le GRN */
    MAGASINIER,
    /** Magasinier Destination — BAG ERP : valide la réception d'un transfert inter-sites (LTI) */
    MAGASINIER_DEST,
    /** Comptable — BAG ERP : valide le GRC (costing financier) */
    COMPTABLE,
    AMG,
    DAF,
    DG,
    ADMINISTRATEUR;

    /**
     * BUG-15 FIX : méthode utilitaire centralisée — vérifie si le rôle est un magasinier
     * (générique ou géographique).
     */
    public boolean isMagasinier() {
        return this == MAGASINIER
            || this == MAGASINIER_DEST;
    }

    /**
     * Vérifie si le rôle est un rôle d'Acheteur (générique ou spécialisé par famille).
     */
    public boolean isAcheteur() {
        return this == ACHETEUR
            || this == ACHETEUR_INFORMATIQUE
            || this == ACHETEUR_BUREAUTIQUE
            || this == ACHETEUR_MOBILIER
            || this == ACHETEUR_CONSOMMABLE
            || this == ACHETEUR_AUTRE;
    }

    /**
     * Retourne la catégorie assignée à un acheteur spécifique.
     * Retourne null pour le super ACHETEUR ou les autres rôles.
     */
    public CategorieDemande getCategorieAssignee() {
        return switch (this) {
            case ACHETEUR_INFORMATIQUE -> CategorieDemande.INFORMATIQUE;
            case ACHETEUR_BUREAUTIQUE -> CategorieDemande.BUREAUTIQUE;
            case ACHETEUR_MOBILIER -> CategorieDemande.MOBILIER;
            case ACHETEUR_CONSOMMABLE -> CategorieDemande.CONSOMMABLE;
            case ACHETEUR_AUTRE -> CategorieDemande.AUTRE;
            default -> null;
        };
    }
}
