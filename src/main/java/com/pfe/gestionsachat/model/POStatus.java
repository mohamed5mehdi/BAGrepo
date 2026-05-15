package com.pfe.gestionsachat.model;

/**
 * Statuts du Bon de Commande (PO) — alignés sur le flux BAG ERP.
 * Machine à états stricte : toute transition hors du chemin défini lève une exception.
 *
 * DRAFT → PENDING_APPROVAL → APPROVED → (SHORT_CLOSED)
 *                          ↘ REJECTED
 */
public enum POStatus {
    /** PO créé mais non soumis à approbation */
    DRAFT,
    /** Soumis au Responsable Service Achat — en attente de décision */
    PENDING_APPROVAL,
    /** Approuvé — le Magasinier peut créer un GRN */
    APPROVED,
    /** Statut hérité de la version précédente (synonyme de APPROVED) */
    VALIDEE,
    /** Refusé par le Responsable Service Achat */
    REJECTED,
    /** Clôture manuelle forcée avant réception complète (Shipped > 0) */
    SHORT_CLOSED
}
