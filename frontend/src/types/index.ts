// ── Domain enums ────────────────────────────────────────────
export type Role =
  | 'ROLE_DEMANDEUR'
  | 'ROLE_N1'
  | 'ROLE_TECHNICIEN'
  | 'ROLE_ACHETEUR'
  | 'ROLE_AMG'
  | 'ROLE_DAF'
  | 'ROLE_DG'
  | 'ROLE_ADMIN';

export type StatutDA =
  | 'EN_ATTENTE_N1'
  | 'EN_ATTENTE_TECH'
  | 'EN_ATTENTE_ACHAT'
  | 'EN_ATTENTE_AMG'
  | 'EN_ATTENTE_DAF'
  | 'EN_ATTENTE_DG'
  | 'VALIDEE'
  | 'PO_CREE'
  | 'REJETEE';

export type ValidationDecision = 'ACCEPTE' | 'REJETE';

// ── Entities ────────────────────────────────────────────────
export interface User {
  oid_user: number;
  nom: string;
  email: string;
  role: Role;
  service?: string;
  actif?: boolean;
  n1?: User | null;
  n1_id?: number | null;
}

export interface Family {
  id: number;
  name: string;
  budget_initial: number;
  budget_restant: number;
}

export interface SubFamily {
  id: number;
  name: string;
  budget_initial: number;
  budget_restant: number;
  family?: Family;
}

export interface Supplier {
  oidSupplier: number;
  nom: string;
  contact: string;
  adresse: string;
}

export interface DaDetails {
  oid_detail?: number;
  subFamily?: SubFamily;
  quantite: number;
  itemCode?: string;
  itemName?: string;
  description?: string;
  justification?: string;
  prix_unitaire: number;
  fournisseur?: Supplier | null;
  totalPrice?: number;
}

export interface DaHeader {
  oid_da: number;
  demandeur?: User;
  dateCreation?: string;
  statut: StatutDA;
  objet: string;
  urgencyLevel?: string;
  justification?: string;
  details?: DaDetails[];
}

export interface PurchaseOrder {
  id_po: number;
  daHeader?: DaHeader;
  date_creation: string;
  statut: string;
  montant_total: number;
}

export interface BudgetTransfer {
  idTransfert: number;
  daHeader?: DaHeader;
  subSource?: SubFamily | null;
  subCible?: SubFamily;
  montant: number;
  dateTransfert: string;
}

// ── Auth ─────────────────────────────────────────────────────
export interface AuthUser {
  userId: number;
  userName: string;
  email: string;
  role: Role;
}

// ── API Responses ─────────────────────────────────────────────
export type BudgetCheckResult = 'SUFFISANT' | 'INSUFFISANT';

export interface ApiError {
  timestamp: string;
  message: string;
  status: number;
}
