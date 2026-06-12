package com.pfe.gestionsachat.model;

/**
 * BUG-11 FIX : entité zombie supprimée du modèle JPA.
 *
 * TransferRequest était un doublon fonctionnel complet de TransferHeader + TransferLine.
 * Elle n'était référencée dans aucun service, controller ou repository.
 * La table transfer_request en base doit être supprimée via migration Flyway/Liquibase.
 *
 * Entité canonique de transfert : TransferHeader (avec ses TransferLine).
 *
 * @deprecated Entité morte — utiliser TransferHeader exclusivement.
 *             Migration DDL requise : DROP TABLE IF EXISTS transfer_request;
 */
@Deprecated(since = "BUG-11-FIX", forRemoval = true)
public class TransferRequest {
    // Classe volontairement vide — plus d'annotation @Entity.
    // La table transfer_request sera nettoyée via migration DDL.
    // Aucun repository ou service ne doit référencer cette classe.
}
