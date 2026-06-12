package com.pfe.gestionsachat.model;

/**
 * Statuts du Bon de Commande (PO) — alignés sur le flux BAG ERP.
 * Machine à états stricte : toute transition hors du chemin défini lève une exception.
 *
 * DRAFT → PENDING_APPROVAL → APPROVED → (SHORT_CLOSED)
 *                          ↘ REJECTED
 *
 * BUG-13 FIX : VALIDEE est conservé pour la compatibilité ascendante avec les données historiques en base.
 * Il est marqué @Deprecated — tout nouveau code doit utiliser APPROVED exclusivement.
 * Migration DDL requise : UPDATE purchase_order SET statut = 'APPROVED' WHERE statut = 'VALIDEE';
 * Après migration : supprimer VALIDEE de cet enum.
 */
public enum POStatus {
    /** PO créé mais non soumis à approbation */
    DRAFT,
    /** Soumis au Responsable Service Achat — en attente de décision */
    PENDING_APPROVAL,
    /** Approuvé — le Magasinier peut créer un GRN */
    APPROVED,
    /**
     * BUG-13 FIX : statut hérité de l'ancienne version — synonyme exact de APPROVED.
     * @deprecated Utiliser APPROVED. Migration : UPDATE purchase_order SET statut = 'APPROVED' WHERE statut = 'VALIDEE';
     *             Tout service vérifiant l'approbation DOIT inclure les deux valeurs :
     *             po.getStatut() == APPROVED || po.getStatut() == VALIDEE
     *             jusqu'à la complétion de la migration DDL.
     */
    @Deprecated(since = "BUG-13-FIX", forRemoval = true)
    VALIDEE,
    /** Refusé par le Responsable Service Achat */
    REJECTED,
    /** Clôture manuelle forcée avant réception complète (Shipped > 0) */
    SHORT_CLOSED;

    /**
     * BUG-13 FIX : méthode utilitaire — vérifie si le PO est dans un état "approuvé"
     * en tenant compte des deux valeurs APPROVED et VALIDEE (ancien code).
     * À utiliser dans TOUS les services jusqu'à la migration DDL complète.
     */
    public boolean isApproved() {
        return this == APPROVED || this == VALIDEE;
    }
}
