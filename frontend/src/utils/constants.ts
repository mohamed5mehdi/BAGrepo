import type { StatutDA, Role } from '../types';

export const STATUS_LABELS: Record<StatutDA, string> = {
  EN_ATTENTE_N1:    'En attente N+1',
  EN_ATTENTE_TECH:  'En attente Technicien',
  EN_ATTENTE_ACHAT: 'En traitement Achat',
  EN_ATTENTE_AMG:   'En attente AMG',
  EN_ATTENTE_DAF:   'En attente DAF',
  EN_ATTENTE_DG:    'En attente DG',
  VALIDEE:          'Validée',
  PO_CREE:          'Bon de commande créé',
  REJETEE:          'Rejetée',
};

export const STATUS_COLORS: Record<StatutDA, string> = {
  EN_ATTENTE_N1:    'bg-yellow-100 text-yellow-800',
  EN_ATTENTE_TECH:  'bg-blue-100 text-blue-800',
  EN_ATTENTE_ACHAT: 'bg-indigo-100 text-indigo-800',
  EN_ATTENTE_AMG:   'bg-orange-100 text-orange-800',
  EN_ATTENTE_DAF:   'bg-purple-100 text-purple-800',
  EN_ATTENTE_DG:    'bg-pink-100 text-pink-800',
  VALIDEE:          'bg-green-100 text-green-800',
  PO_CREE:          'bg-emerald-100 text-emerald-800',
  REJETEE:          'bg-red-100 text-red-800',
};

export const ROLE_LABELS: Record<Role, string> = {
  ROLE_DEMANDEUR:  'Demandeur',
  ROLE_N1:         'Manager N+1',
  ROLE_TECHNICIEN: 'Technicien',
  ROLE_ACHETEUR:   'Acheteur',
  ROLE_AMG:        'AMG',
  ROLE_DAF:        'DAF',
  ROLE_DG:         'Direction Générale',
};

export const ROLE_COLORS: Record<Role, string> = {
  ROLE_DEMANDEUR:  'from-sky-500 to-blue-600',
  ROLE_N1:         'from-violet-500 to-purple-600',
  ROLE_TECHNICIEN: 'from-cyan-500 to-teal-600',
  ROLE_ACHETEUR:   'from-indigo-500 to-blue-700',
  ROLE_AMG:        'from-orange-500 to-amber-600',
  ROLE_DAF:        'from-fuchsia-500 to-purple-700',
  ROLE_DG:         'from-rose-500 to-red-700',
};

export function getDaTotal(da: { details?: { quantite: number; prix_unitaire: number }[] }): number {
  if (!da.details) return 0;
  return da.details.reduce((sum, d) => sum + (d.quantite ?? 0) * (d.prix_unitaire ?? 0), 0);
}

export function formatCurrency(n: number): string {
  return n.toLocaleString('fr-FR', { style: 'currency', currency: 'EUR' });
}

export function formatDA(id: number): string {
  return `DA-${String(id).padStart(3, '0')}`;
}

// Workflow steps for stepper
export const WORKFLOW_STEPS: { statut: StatutDA; label: string; icon: string }[] = [
  { statut: 'EN_ATTENTE_N1',    label: 'N+1',      icon: '👤' },
  { statut: 'EN_ATTENTE_TECH',  label: 'Technicien', icon: '🔧' },
  { statut: 'EN_ATTENTE_ACHAT', label: 'Acheteur',  icon: '🛒' },
  { statut: 'EN_ATTENTE_AMG',   label: 'AMG',       icon: '📋' },
  { statut: 'EN_ATTENTE_DAF',   label: 'DAF',       icon: '💰' },
  { statut: 'EN_ATTENTE_DG',    label: 'DG',        icon: '🏢' },
  { statut: 'VALIDEE',          label: 'Validée',   icon: '✅' },
  { statut: 'PO_CREE',          label: 'PO Créé',   icon: '📜' },
];

const STEP_ORDER = WORKFLOW_STEPS.map(s => s.statut);

export function getStepIndex(statut: StatutDA): number {
  if (statut === 'REJETEE') return -1;
  return STEP_ORDER.indexOf(statut);
}
