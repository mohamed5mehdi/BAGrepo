// ── Domain enums ────────────────────────────────────────────
export type Role =
  | 'EMPLOYE'
  | 'MANAGER_N1'
  | 'TECHNICIEN'
  | 'ACHETEUR'
  | 'AMG'
  | 'DAF'
  | 'DG'
  | 'ADMINISTRATEUR';

export type StatutDA =
  | 'BROUILLON' | 'SOUMISE'
  | 'EN_ATTENTE_N1' | 'VALIDEE_N1'
  | 'EN_ATTENTE_TECH' | 'VALIDEE_TECH'
  | 'EN_ATTENTE_ACHAT' | 'EN_TRAITEMENT' | 'A_COMMANDER'
  | 'EN_ATTENTE_AMG' | 'EN_VALIDATION_AMG'
  | 'EN_ATTENTE_DAF' | 'EN_VALIDATION_DAF' | 'AJUSTEMENT_DAF'
  | 'EN_ATTENTE_DG' | 'EN_VALIDATION_DG' | 'AJUSTEMENT_DG'
  | 'VALIDEE' | 'APPROUVEE' | 'PO_CREE' | 'REJETEE'
  | 'EN_LIVRAISON' | 'AFFECTEE';

export type ValidationDecision = 'ACCEPTE' | 'REJETE';

// ── Entities ────────────────────────────────────────────────
export type CategorieDemande = 'INFORMATIQUE' | 'BUREAUTIQUE' | 'MOBILIER' | 'CONSOMMABLE' | 'AUTRE';
export type UrgenceDemande = 'NORMALE' | 'URGENTE' | 'CRITIQUE';
export type StatutDemande = StatutDA;

export interface DemandeAchatInterne {
  id: number;
  oid_da?: number; // Alias pour compatibilité
  demandeur?: User;
  departement?: string;
  categorie: CategorieDemande;
  designation: string;
  objet?: string; // Alias pour compatibilité
  quantite: number;
  justification?: string;
  urgence: UrgenceDemande;
  urgencyLevel?: string; // Alias pour compatibilité
  budgetFamille?: Family;
  budgetSousFamille?: SubFamily;
  statut: StatutDA;
  typeAjustement?: 'SOUS_FAMILLE' | 'FAMILLE';
  montantEstime?: number;
  prixUnitaire?: number;
  fournisseur?: Supplier;
  dateCreation?: string;
  dateValidation?: string;
  commentaireRejet?: string;
}

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
  idFamily?: number; // Alias backend
  name: string;
  libelle?: string; // Alias backend
  budget_initial: number;
  budgetTotal?: number; // Alias DtoFinancier
  budget_restant: number;
  budgetRestant?: number; // Alias DtoFinancier
  budget_disponible?: number;
  budgetEngage?: number;
}

export interface SubFamily {
  id: number;
  oidSub?: number; // Alias backend
  id_family?: number; // Alias backend
  name: string;
  libelle?: string; // Alias backend
  budget_initial: number;
  budget_restant: number;
  budget_disponible?: number;
  family?: Family;
  budgetEngage?: number;
}

export interface Supplier {
  oidSupplier: number;
  nom: string;
  contact: string;
  adresse: string;
  ice?: string;
  email?: string;
  phone?: string;
  sector?: string;
  rating?: number;
  averageLeadTime?: number;
  isCertified?: boolean;
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

export interface DaHeader extends DemandeAchatInterne {
  oid_da: number;
  objet: string;
  details?: DaDetails[];
}

export interface PurchaseOrder {
  id_po: number;
  daHeader?: DaHeader;
  fournisseur?: Supplier;
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

// ── Logistics Enums ───────────────────────────────────────────
export type GrnStatus = 'DRAFT' | 'VALIDATED' | 'PARTIAL';
export type QualityStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'QUARANTINE';
export type GrcStatus = 'DRAFT' | 'VALIDATED' | 'POSTED';
export type InvoiceStatus = 'RECEIVED' | 'MATCHED' | 'APPROVED' | 'PAID' | 'REJECTED';
export type CreditNoteStatus = 'PENDING' | 'ISSUED' | 'RECONCILED';
export type WarehouseType = 'CENTRAL' | 'REGIONAL' | 'LOCAL';
export type MovementType = 'IN_RECEIPT' | 'OUT_RETURN' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'CONSUMPTION';
export type TransferStatus = 'REQUESTED' | 'IN_TRANSIT' | 'DELIVERED';

export interface SupplierOffer {
  id: number;
  fournisseur: Supplier;
  prixPropose: number;
  conditions: string;
  delaiLivraisonOffert?: number;
}

// ── Logistics Entities ─────────────────────────────────────────
export interface GrnHeader {
  id: number;
  purchaseOrder?: PurchaseOrder;
  supplier?: Supplier;
  deliveryNoteNumber: string;
  receiptDate: string;
  receivedBy?: User;
  parentGrn?: GrnHeader;
  status: GrnStatus;
  details?: GrnDetails[];
}

export interface GrnDetails {
  id: number;
  grnHeader?: GrnHeader;
  itemCode: string;
  itemName: string;
  orderedQuantity: number;
  receivedQuantity: number;
  acceptedQuantity: number;
  rejectedQuantity: number;
  qualityStatus: QualityStatus;
  notes?: string;
}

export interface GrcHeader {
  id: number;
  grnHeader?: GrnHeader;
  costingDate: string;
  processedBy?: User;
  status: GrcStatus;
  totalAmount: number;
  devise: string;
  details?: GrcDetails[];
}

export interface GrcDetails {
  id: number;
  grcHeader?: GrcHeader;
  grnDetail?: GrnDetails;
  itemCode: string;
  acceptedQuantity: number;
  unitCost: number;
  totalCost: number;
  taxRate?: number;
  montantTTC: number;
}

export interface Invoice {
  id: number;
  purchaseOrder?: PurchaseOrder;
  grnHeader?: GrnHeader;
  invoiceNumber: string;
  invoiceDate: string;
  montantHT: number;
  montantTTC: number;
  status: InvoiceStatus;
}

export interface CreditNote {
  id: number;
  grnHeader?: GrnHeader;
  creditNoteNumber: string;
  creditNoteDate: string;
  montant: number;
  status: CreditNoteStatus;
}

export interface Warehouse {
  id: number;
  name: string;
  location: string;
  type: WarehouseType;
}

export interface StockItem {
  id: number;
  warehouse?: Warehouse;
  itemCode: string;
  itemName: string;
  quantityAvailable: number;
  quantityReserved: number;
  unitCost?: number;
  minStock?: number;
  reorderPoint?: number;
}

export interface StockMovement {
  id: number;
  stockItem?: StockItem;
  movementType: MovementType;
  quantity: number;
  movementDate: string;
  referenceDocument: string;
}

export interface TransferRequest {
  id: number;
  sourceWarehouse?: Warehouse;
  destinationWarehouse?: Warehouse;
  itemCode: string;
  quantity: number;
  status: TransferStatus;
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

