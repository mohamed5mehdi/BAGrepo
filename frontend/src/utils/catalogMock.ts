export interface CatalogEntry {
    fournisseurId: number;
    fournisseurNom: string;
    prixRef: number;
    delaiRef: number;
    conditionsRef: string;
}

export const MOCK_CATALOG: Record<string, CatalogEntry[]> = {
    'PC Portables & Stations': [
        { fournisseurId: 1, fournisseurNom: 'Alpha IT Solutions', prixRef: 27500, delaiRef: 5, conditionsRef: 'Garantie 5 ans ProSupport, Housse incluse' },
        { fournisseurId: 2, fournisseurNom: 'Global Office Systems', prixRef: 29000, delaiRef: 10, conditionsRef: 'Garantie standard 2 ans' }
    ],
    'Abonnements Cloud (Azure/AWS)': [
        { fournisseurId: 1, fournisseurNom: 'Alpha IT Solutions', prixRef: 50000, delaiRef: 1, conditionsRef: 'Support 24/7' },
        { fournisseurId: 3, fournisseurNom: 'Elite Services Group', prixRef: 48500, delaiRef: 2, conditionsRef: 'SLA 99.9%' }
    ],
    'Suites Bureautiques (M365)': [
        { fournisseurId: 1, fournisseurNom: 'Alpha IT Solutions', prixRef: 450, delaiRef: 1, conditionsRef: 'Licence Annuelle, Support Tel' },
        { fournisseurId: 2, fournisseurNom: 'Global Office Systems', prixRef: 480, delaiRef: 2, conditionsRef: 'Licence Annuelle' }
    ],
    'Bureaux & Chaises Ergonomiques': [
        { fournisseurId: 2, fournisseurNom: 'Global Office Systems', prixRef: 13500, delaiRef: 15, conditionsRef: 'Montage sur site inclus' },
        { fournisseurId: 3, fournisseurNom: 'Elite Services Group', prixRef: 14200, delaiRef: 10, conditionsRef: 'Livraison express, sans montage' }
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
