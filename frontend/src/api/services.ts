import api from './axios';
import type {
  DaHeader, DaDetails, Family, SubFamily,
  Supplier, PurchaseOrder, User,
  ValidationDecision, BudgetCheckResult,
  GrnHeader, GrcHeader, Invoice, CreditNote,
  Warehouse, StockItem,
} from '../types';

// ── Auth ─────────────────────────────────────────────────────
export const login = (email: string, password: string) =>
  api.post<{ success: boolean; userId: number; userName: string; email: string; role: string; message: string }>(
    '/auth/login',
    null,
    { params: { email, password } }
  );

// ── Users ─────────────────────────────────────────────────────
export const getUsers = () => api.get<User[]>('/users');
export const getUserById = (id: number) => api.get<User>(`/users/${id}`);

// ── DA Headers ───────────────────────────────────────────────
export const getAllDA = () => api.get<DaHeader[]>('/da-headers');
export const getDAById = (id: number) => api.get<DaHeader>(`/da-headers/${id}`);
export const getDAByStatus = (statut: string) => api.get<DaHeader[]>(`/da-headers/status/${statut}`);
export const getDAByDemandeur = (userId: number) => api.get<DaHeader[]>(`/da-headers/demandeur/${userId}`);
export const createDA = (da: Partial<DaHeader>) => api.post<DaHeader>('/da-headers', da);
export const updateDA = (id: number, da: Partial<DaHeader>) => api.put<DaHeader>(`/da-headers/${id}`, da);
export const deleteDA = (id: number) => api.delete(`/da-headers/${id}`);

// ── Demandes Achat Internes (BAG) ───────────────────────────
export const createDemandeInterne = (demande: any, userId: number) => 
  api.post('/demandes', demande, { params: { userId } });

export const getDemandeInterneById = (id: number) => 
  api.get(`/demandes/${id}`);

export const soumettreDemandeInterne = (id: number, userId: number) => 
  api.post(`/demandes/${id}/soumettre`, null, { params: { userId } });

export const validerN1DemandeInterne = (id: number, valider: boolean, commentaire: string, userId: number) => 
  api.put(`/demandes/${id}/valider-n1`, { valider, commentaire }, { params: { userId } });

export const validerTechDemandeInterne = (id: number, valider: boolean, commentaire: string, userId: number) => 
  api.put(`/demandes/${id}/valider-technicien`, { valider, commentaire }, { params: { userId } });

export const valoriserDemandeInterne = (id: number, prixUnitaire: number, supplierId: number) => 
  api.put(`/demandes/${id}/valoriser-achat`, null, { params: { prixUnitaire, supplierId } });

export const traiterAchatDemandeInterne = (id: number, userId: number) => 
  api.put(`/demandes/${id}/traiter-achat`, null, { params: { userId } });

export const validerAMGDemandeInterne = (id: number, valider: boolean, commentaire: string, userId: number) => 
  api.put(`/demandes/${id}/valider-amg`, { valider, commentaire }, { params: { userId } });

export const validerDAFDemandeInterne = (id: number, valider: boolean, commentaire: string, userId: number) => 
  api.put(`/demandes/${id}/valider-daf`, { valider, commentaire }, { params: { userId } });

export const validerDGDemandeInterne = (id: number, valider: boolean, commentaire: string, userId: number) => 
  api.put(`/demandes/${id}/valider-dg`, { valider, commentaire }, { params: { userId } });

export const creerPODemandeInterne = (id: number, userId: number) => 
  api.post(`/demandes/${id}/creer-po`, null, { params: { userId } });

export const getMesDemandesInternes = (userId: number) => 
  api.get('/demandes/mes-demandes', { params: { userId } });

export const getDemandesAValiderInternes = (userId: number) => 
  api.get('/demandes/a-valider', { params: { userId } });

// ── Workflow ──────────────────────────────────────────────────
export const validateWorkflow = (daId: number, userId: number, decision: ValidationDecision, motif?: string) =>
  api.post<DaHeader>('/workflow/validate', null, { params: { daId, userId, decision, motif } });

export const checkBudget = (daId: number, acheteurId: number) =>
  api.post<BudgetCheckResult>('/workflow/check-budget', null, { params: { daId, acheteurId } });

export const requestAdjustment = (daId: number, acheteurId: number, type: 'SUBFAMILY' | 'FAMILY', motif?: string) =>
  api.post<DaHeader>('/workflow/request-adjustment', null, { params: { daId, acheteurId, type, motif } });

export const adjustSubFamily = (daId: number, dafId: number, sourceId: number, cibleId: number, montant: number) =>
  api.post<DaHeader>('/workflow/adjust-subfamily', null, { params: { daId, dafId, sourceId, cibleId, montant } });

export const adjustFamily = (daId: number, dgId: number, cibleId: number, montant: number) =>
  api.post<DaHeader>('/workflow/adjust-family', null, { params: { daId, dgId, cibleId, montant } });

export const createPO = (daId: number, acheteurId: number) =>
  api.post<PurchaseOrder>('/workflow/create-po', null, { params: { daId, acheteurId } });

// ── Families & SubFamilies ────────────────────────────────────
export const getFamilies = () => api.get<Family[]>('/families');
export const getSubFamilies = () => api.get<SubFamily[]>('/sub-families');
export const getSubFamiliesByFamily = (familyId: number) =>
  api.get<SubFamily[]>(`/sub-families/family/${familyId}`);

// ── Suppliers ─────────────────────────────────────────────────
export const getSuppliers = () => api.get<Supplier[]>('/suppliers');

// ── Purchase Orders ───────────────────────────────────────────
export const getPurchaseOrders = () => api.get<PurchaseOrder[]>('/purchase-orders');

// ── GRN (Good Receipt Note) ───────────────────────────────────
export const createGrn = (grn: Partial<GrnHeader>) => api.post<GrnHeader>('/grn', grn);
export const validateGrn = (id: number) => api.put<GrnHeader>(`/grn/${id}/validate`);

// ── GRC (Good Receipt Costing) ────────────────────────────────
export const createGrc = (grc: Partial<GrcHeader>) => api.post<GrcHeader>('/grc', grc);
export const validateGrc = (id: number) => api.put<GrcHeader>(`/grc/${id}/validate`);

// ── Invoices ──────────────────────────────────────────────────
export const createInvoice = (invoice: Partial<Invoice>) => api.post<Invoice>('/invoice', invoice);
export const matchInvoice = (id: number) => api.post<Invoice>(`/invoice/${id}/match`);

// ── Credit Notes ──────────────────────────────────────────────
export const createCreditNote = (cn: Partial<CreditNote>) => api.post<CreditNote>('/creditnote', cn);

// ── Warehouse & Stock ─────────────────────────────────────────
export const getWarehouses = () => api.get<Warehouse[]>('/warehouse');
export const getStockItems = () => api.get<StockItem[]>('/warehouse/stock'); // Note: Endpoint à confirmer côté backend

