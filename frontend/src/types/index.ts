// ── Domain enums ────────────────────────────────────────────
export type Role =
  | 'EMPLOYE'
  | 'MANAGER_N1'
  | 'TECHNICIEN'
  | 'ACHETEUR'
  | 'RESP_ACHAT'       // Responsable Service Achat — BAG ERP
  | 'MAGASINIER'       // Magasinier — BAG ERP
  | 'COMPTABLE'        // Comptable Service Comptabilité — BAG ERP
  | 'AMG'
  | 'DAF'
  | 'DG'
  | 'ADMINISTRATEUR';

export type StatutDA =
  // Flux Interne (StatutDemande)
  | 'BROUILLON' | 'SOUMISE' | 'VALIDE_N1' | 'VALIDE_TECH' | 'VALIDE_AMG' | 'VALIDE_DAF' | 'VALIDE_DG'
  | 'EN_TRAITEMENT' | 'DISPONIBLE_STOCK' | 'APPROUVEE'
  // Flux Classique (StatutDA)
  | 'EN_ATTENTE_N1' | 'EN_ATTENTE_TECH' | 'EN_ATTENTE_ACHAT' | 'EN_ATTENTE_AMG' | 'EN_ATTENTE_DAF' | 'EN_ATTENTE_DG'
  | 'VALIDEE' | 'PO_CREE' | 'REJETEE' | 'EN_LIVRAISON' | 'AFFECTEE';

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
  isPieceRechange?: boolean;
  itemCode?: string;
  isAvailableInStock?: boolean;
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
  demandeInterne?: DemandeAchatInterne;
  fournisseur?: Supplier;
  date_creation: string;
  /** Statut typé BAG ERP — remplace l'ancien string libre */
  statut: POStatus;
  montant_total: number;
  /** Numéro de référence BAG ERP (PO-YYYYMM-XXXXX) */
  poNumber?: string;
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
/** BAG ERP PO statuses — machine à états stricte */
export type POStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'SHORT_CLOSED';
/** BAG ERP GRN statuses — Magasinier, pas d'approbation hiérarchique */
export type GrnStatus = 'DRAFT' | 'PENDING' | 'ENTRY_COMPLETED';
export type QualityStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'QUARANTINE';
/** BAG ERP GRC statuses — Comptable */
export type GrcStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'VALIDATED' | 'POSTED';
export type InvoiceStatus = 'RECEIVED' | 'MATCHED' | 'APPROVED' | 'PAID' | 'REJECTED';
export type CreditNoteStatus = 'PENDING' | 'ISSUED' | 'RECONCILED';
export type WarehouseType = 'CENTRAL' | 'REGIONAL' | 'LOCAL';
export type MovementType = 'IN_RECEIPT' | 'OUT_RETURN' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'CONSUMPTION' | 'AFFECTATION';
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
  /** Numéro de référence BAG ERP — partagé avec le GRC associé (même numéro) */
  grnNumber?: string;
  details?: GrnDetails[];
}

export interface GrnDetails {
  id: number;
  grnHeader?: GrnHeader;
  itemCode: string;
  itemName: string;
  orderedQuantity: number;
  /** Solde restant à recevoir (BAG ERP Shipped Qty) — PO clôturé quand = 0 */
  shippedQuantity: number;
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
  mainAccount?: string;
  subAccount?: string;
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
  /** Code emplacement logique virtuel (BAG ERP — auto-généré, unique par article) */
  locationCode?: string;
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
  token?: string;
}

// ── API Responses ─────────────────────────────────────────────
export interface BudgetCheckResult {
  suffisant: boolean;
  montantRequis: number;
  budgetActuel: number;
  message: string;
}

export interface ApiError {
  timestamp: string;
  message: string;
  status: number;
}

// ── Chatbot Entities ──────────────────────────────────────────
export interface SlotState {
  designation?: string | null;
  quantite?: number | null;
  justification?: string | null;
  urgence?: UrgenceDemande | null;
  familyId?: number | null;
  subFamilyId?: number | null;
  familyLibelle?: string | null;
  subFamilyLibelle?: string | null;
}

export interface ChatResponse {
  sessionId: string;
  botMessage: string;
  slots: SlotState;
  complet: boolean;
  confirmed: boolean;
  daCreeeId?: number | null;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: 'USER' | 'BOT';
  content: string;
  dateEnvoi: string;
  slotsSnapshot?: string | null;
}


