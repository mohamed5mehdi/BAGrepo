export interface CatalogEntry {
    fournisseurId: number;
    fournisseurNom: string;
    prixRef: number;
    delaiRef: number;
    conditionsRef: string;
}

export const MOCK_CATALOG: Record<string, CatalogEntry[]> = {
    'PC Portables & Stations': [
        { fournisseurId: 1, fournisseurNom: 'Alpha IT Solutions', prixRef: 11500, delaiRef: 5, conditionsRef: 'Garantie 3 ans ProSupport, Sacoche incluse' },
        { fournisseurId: 2, fournisseurNom: 'Global Office Systems', prixRef: 12000, delaiRef: 10, conditionsRef: 'Garantie standard 2 ans' }
    ],
    'Abonnements Cloud (Azure/AWS)': [
        { fournisseurId: 1, fournisseurNom: 'Alpha IT Solutions', prixRef: 35000, delaiRef: 1, conditionsRef: 'Support 24/7 (Annuel)' },
        { fournisseurId: 3, fournisseurNom: 'Elite Services Group', prixRef: 42000, delaiRef: 2, conditionsRef: 'SLA 99.9% (Annuel)' }
    ],
    'Suites Bureautiques (M365)': [
        { fournisseurId: 1, fournisseurNom: 'Alpha IT Solutions', prixRef: 1800, delaiRef: 1, conditionsRef: 'Business Standard Annuel' },
        { fournisseurId: 2, fournisseurNom: 'Global Office Systems', prixRef: 1950, delaiRef: 2, conditionsRef: 'Business Standard Annuel + Setup' }
    ],
    'Bureaux & Chaises Ergonomiques': [
        { fournisseurId: 2, fournisseurNom: 'Global Office Systems', prixRef: 4500, delaiRef: 15, conditionsRef: 'Poste Complet, Montage sur site' },
        { fournisseurId: 3, fournisseurNom: 'Elite Services Group', prixRef: 5200, delaiRef: 10, conditionsRef: 'Poste Premium Express' }
    ],
    'Climatisation & Aménagement': [
        { fournisseurId: 2, fournisseurNom: 'Global Office Systems', prixRef: 8500, delaiRef: 7, conditionsRef: 'Garantie 2 ans, installation comprise' },
        { fournisseurId: 3, fournisseurNom: 'Elite Services Group', prixRef: 8200, delaiRef: 14, conditionsRef: 'Installation sous-traitée' }
    ],
    'Fournitures de bureau': [
        { fournisseurId: 2, fournisseurNom: 'Global Office Systems', prixRef: 65, delaiRef: 2, conditionsRef: 'Livraison gratuite > 1000 MAD' },
        { fournisseurId: 3, fournisseurNom: 'Elite Services Group', prixRef: 60, delaiRef: 5, conditionsRef: 'Paiement à 30 jours' }
    ],
    'Services Traiteur & Réception': [
        { fournisseurId: 3, fournisseurNom: 'Elite Services Group', prixRef: 15000, delaiRef: 7, conditionsRef: 'Menu Premium, Service inclus' }
    ]
};
