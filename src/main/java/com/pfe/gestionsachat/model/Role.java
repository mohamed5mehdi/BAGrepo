package com.pfe.gestionsachat.model;

public enum Role {
    EMPLOYE,
    MANAGER_N1,
    MANAGER_N2,
    TECHNICIEN,
    ACHETEUR,
    /** Responsable Service Achat — BAG ERP : approuve le PO */
    RESP_ACHAT,
    /** Magasinier — BAG ERP : reçoit les marchandises et génère le GRN */
    MAGASINIER,
    /** Magasinier Destination — BAG ERP : valide la réception d'un transfert inter-sites (LTI) */
    MAGASINIER_DEST,
    /** Comptable — BAG ERP : valide le GRC (costing financier) */
    COMPTABLE,
    AMG,
    DAF,
    DG,
    ADMINISTRATEUR
}

