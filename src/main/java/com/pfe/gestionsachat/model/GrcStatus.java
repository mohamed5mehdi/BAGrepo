package com.pfe.gestionsachat.model;

/**
 * Statuts du GRC — alignés sur le flux BAG ERP.
 * PENDING_APPROVAL : GRC créé, en attente de validation financière (Comptable).
 * POSTED           : GRC validé — rapprochement PO/GRN/GRC complété, facture générée.
 * APPROVED         : Second visa (Responsable Achat) — circuit complet clôturé.
 */
public enum GrcStatus {
    DRAFT,
    PENDING_APPROVAL,
    POSTED,
    APPROVED
}

