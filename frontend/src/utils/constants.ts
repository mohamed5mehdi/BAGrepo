import type { StatutDA, Role } from '../types';

export const STATUS_LABELS: Record<StatutDA, string> = {
  BROUILLON: 'Brouillon',
  SOUMISE: 'Soumise',
  VALIDE_N1: 'Validée N+1',
  VALIDE_TECH: 'Validée Technique',
  VALIDE_AMG: 'Validée AMG',
  VALIDE_DAF: 'Validée DAF',
  VALIDE_DG: 'Validée DG',
  EN_TRAITEMENT: 'En traitement / Valorisation',
  APPROUVEE: 'Approuvée (Interne)',
  VALIDEE: 'Validée (Classique)',
  EN_ATTENTE_N1: 'Attente N+1 (Classique)',
  EN_ATTENTE_TECH: 'Attente Tech (Classique)',
  EN_ATTENTE_ACHAT: 'Attente Achat (Classique)',
  EN_ATTENTE_AMG: 'Attente AMG (Classique)',
  EN_ATTENTE_DAF: 'Attente DAF (Classique)',
  EN_ATTENTE_DG: 'Attente DG (Classique)',
  PO_CREE: 'Bon de Commande Créé',
  REJETEE: 'Rejetée',
  EN_LIVRAISON: 'En livraison',
  AFFECTEE: 'Affectée',
  DISPONIBLE_STOCK: 'Disponible en Stock',
  VALIDE_ACHETEUR: 'Traitée par l\'Acheteur',
};

export const STATUS_COLORS: Record<StatutDA, string> = {
  BROUILLON: 'bg-slate-100 text-slate-600',
  SOUMISE: 'bg-blue-50 text-blue-600',
  VALIDE_N1: 'bg-yellow-100 text-yellow-800',
  VALIDE_TECH: 'bg-cyan-100 text-cyan-800',
  VALIDE_AMG: 'bg-orange-100 text-orange-800',
  VALIDE_DAF: 'bg-purple-100 text-purple-800',
  VALIDE_DG: 'bg-slate-100 text-slate-800',
  EN_TRAITEMENT: 'bg-indigo-100 text-indigo-800',
  APPROUVEE: 'bg-emerald-100 text-emerald-800',
  VALIDEE: 'bg-green-100 text-green-800',
  EN_ATTENTE_N1: 'bg-yellow-50 text-yellow-700',
  EN_ATTENTE_TECH: 'bg-cyan-50 text-cyan-700',
  EN_ATTENTE_ACHAT: 'bg-indigo-50 text-indigo-700',
  EN_ATTENTE_AMG: 'bg-orange-50 text-orange-700',
  EN_ATTENTE_DAF: 'bg-purple-50 text-purple-700',
  EN_ATTENTE_DG: 'bg-pink-50 text-pink-700',
  PO_CREE: 'bg-teal-100 text-teal-800',
  REJETEE: 'bg-red-100 text-red-800',
  EN_LIVRAISON: 'bg-blue-100 text-blue-800',
  AFFECTEE: 'bg-slate-200 text-slate-800',
  DISPONIBLE_STOCK: 'bg-emerald-100 text-emerald-800 border border-emerald-200',
  VALIDE_ACHETEUR: 'bg-indigo-100 text-indigo-700',
};

export const ROLE_LABELS: Record<Role, string> = {
  EMPLOYE:              'Demandeur',
  MANAGER_N1:           'Manager N+1',
  TECHNICIEN:           'Technicien',
  ACHETEUR:             'Acheteur',
  ACHETEUR_INFORMATIQUE:'Acheteur Informatique',
  ACHETEUR_BUREAUTIQUE: 'Acheteur Bureautique',
  ACHETEUR_MOBILIER:    'Acheteur Mobilier',
  ACHETEUR_CONSOMMABLE: 'Acheteur Consommable',
  ACHETEUR_AUTRE:       'Acheteur Autre',
  AMG:                  'AMG',
  DAF:                  'DAF',
  DG:                   'Direction Générale',
  ADMINISTRATEUR:       'Administrateur',
  MAGASINIER:           'Magasinier Principal',
  MAGASINIER_DEST:      'Magasinier Destination',
  MAGASINIER_CASA:      'Magasinier Casablanca',
  MAGASINIER_RABAT:     'Magasinier Rabat',
  MAGASINIER_TANGER:    'Magasinier Tanger',
  MAGASINIER_MARRAKECH: 'Magasinier Marrakech',
  COMPTABLE:            'Comptabilité / Finance',
  RESP_ACHAT:           'Responsable Achat',
};

export const ROLE_COLORS: Record<Role, string> = {
  EMPLOYE:              'from-sky-500 to-blue-600',
  MANAGER_N1:           'from-violet-500 to-purple-600',
  TECHNICIEN:           'from-cyan-500 to-teal-600',
  ACHETEUR:             'from-indigo-500 to-blue-700',
  ACHETEUR_INFORMATIQUE:'from-indigo-500 to-blue-700',
  ACHETEUR_BUREAUTIQUE: 'from-indigo-500 to-blue-700',
  ACHETEUR_MOBILIER:    'from-indigo-500 to-blue-700',
  ACHETEUR_CONSOMMABLE: 'from-indigo-500 to-blue-700',
  ACHETEUR_AUTRE:       'from-indigo-500 to-blue-700',
  AMG:                  'from-orange-500 to-amber-600',
  DAF:                  'from-fuchsia-500 to-purple-700',
  DG:                   'from-rose-500 to-red-700',
  ADMINISTRATEUR:       'from-slate-800 to-black',
  MAGASINIER:           'from-emerald-500 to-teal-700',
  MAGASINIER_DEST:      'from-emerald-600 to-green-800',
  MAGASINIER_CASA:      'from-blue-600 to-indigo-700',
  MAGASINIER_RABAT:     'from-amber-500 to-orange-600',
  MAGASINIER_TANGER:    'from-cyan-500 to-sky-700',
  MAGASINIER_MARRAKECH: 'from-rose-500 to-pink-700',
  COMPTABLE:            'from-amber-500 to-yellow-600',
  RESP_ACHAT:           'from-indigo-600 to-blue-800',
};

export function getDaTotal(da: { details?: { quantite: number; prix_unitaire: number }[] }): number {
  if (!da.details) return 0;
  return da.details.reduce((sum, d) => sum + (d.quantite ?? 0) * (d.prix_unitaire ?? 0), 0);
}

export function formatCurrency(n: number): string {
  return n.toLocaleString('fr-FR', { style: 'currency', currency: 'MAD' }).replace('MAD', 'DHS');
}

export function formatDA(id: number): string {
  return `DA-${String(id).padStart(3, '0')}`;
}

// Workflow steps for stepper
export const WORKFLOW_STEPS: { statut: StatutDA; label: string; icon: string }[] = [
  { statut: 'VALIDE_N1',    label: 'N+1',      icon: '👤' },
  { statut: 'EN_TRAITEMENT', label: 'Acheteur',  icon: '🛒' },
  { statut: 'VALIDE_TECH',  label: 'Technique', icon: '🔧' },
  { statut: 'VALIDE_AMG',   label: 'AMG',       icon: '📋' },
  { statut: 'VALIDE_DAF',   label: 'DAF',       icon: '💰' },
  { statut: 'VALIDE_DG',    label: 'DG',        icon: '🏢' },
  { statut: 'APPROUVEE',    label: 'Approuvée',   icon: '✅' },
  { statut: 'PO_CREE',      label: 'PO Créé',   icon: '📜' },
];

const STEP_ORDER = WORKFLOW_STEPS.map(s => s.statut);

export function getStepIndex(statut: StatutDA): number {
  if (statut === 'REJETEE') return -1;
  return STEP_ORDER.indexOf(statut);
}
