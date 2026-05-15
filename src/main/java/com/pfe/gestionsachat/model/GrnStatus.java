package com.pfe.gestionsachat.model;

/**
 * Statuts du GRN — alignés sur le flux BAG ERP.
 * PENDING       : GRN créé, marchandises en cours de vérification.
 * ENTRY_COMPLETED : Magasinier a coché "Entry Completed" — stock mis à jour.
 *                   Aucune approbation hiérarchique sur le GRN (règle BAG ERP).
 */
public enum GrnStatus {
    PENDING,
    ENTRY_COMPLETED
}
