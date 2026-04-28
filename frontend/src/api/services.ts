import api from './axios';
import type {
  DaHeader, DaDetails, Family, SubFamily,
  Supplier, PurchaseOrder, User,
  ValidationDecision, BudgetCheckResult,
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
