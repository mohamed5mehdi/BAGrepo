import api from './axios';

// AUTH
export const login = (email: string, password: string) => 
  api.post(`/auth/login?email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`);

// DEMANDES ACHAT INTERNES (DA)
export const getMesDemandesInternes = (userId: number) => api.get(`/demandes/mes-demandes?userId=${userId}`);
export const getDemandesAValiderInternes = (userId: number) => api.get(`/demandes/a-valider?userId=${userId}`);
export const getDemandeInterneById = (id: number) => api.get(`/demandes/${id}`);

export const createDemandeInterne = (payload: any, userId: number) => api.post(`/demandes?userId=${userId}`, payload);
export const soumettreDemandeInterne = (id: number, userId: number) => api.post(`/demandes/${id}/soumettre?userId=${userId}`);

// CIRCUIT DE VALIDATION (PUT)
export const validerN1DemandeInterne = (id: number, approved: boolean, comment: string, userId: number) => 
  api.put(`/demandes/${id}/valider-n1?userId=${userId}`, { valider: approved, commentaire: comment });

export const validerTechDemandeInterne = (id: number, approved: boolean, comment: string, userId: number) => 
  api.put(`/demandes/${id}/valider-technicien?userId=${userId}`, { valider: approved, commentaire: comment });

export const validerAMGDemandeInterne = (id: number, approved: boolean, comment: string, userId: number) => 
  api.put(`/demandes/${id}/valider-amg?userId=${userId}`, { valider: approved, commentaire: comment });

export const validerDAFDemandeInterne = (id: number, approved: boolean, comment: string, userId: number) => 
  api.put(`/demandes/${id}/valider-daf?userId=${userId}`, { valider: approved, commentaire: comment });

export const validerDGDemandeInterne = (id: number, approved: boolean, comment: string, userId: number) => 
  api.put(`/demandes/${id}/valider-dg?userId=${userId}`, { valider: approved, commentaire: comment });

// ACHATS & PO
export const valoriserDemandeInterne = (id: number, prixUnitaire: number, supplierId: number) => 
    api.put(`/demandes/${id}/valoriser-achat?prixUnitaire=${prixUnitaire}&supplierId=${supplierId}`);
export const valoriserDaClassic = (id: number, prixUnitaire: number, supplierId: number) => 
    api.put(`/da-headers/${id}/valoriser-achat?prixUnitaire=${prixUnitaire}&supplierId=${supplierId}`);

export const traiterAchatDemandeInterne = (id: number, userId: number) => api.put(`/demandes/${id}/traiter-achat?userId=${userId}`);
export const creerPODemandeInterne = (id: number, userId: number) => api.post(`/demandes/${id}/creer-po?userId=${userId}`);
export const autoGenerateGrnGrc = (poId: number, userId: number) => api.post(`/purchase-orders/${poId}/auto-grn-grc?userId=${userId}`);
export const getPendingInternalPOsForAutomation = () => api.get(`/purchase-orders/pending-grn-internal`);

// BUDGETS & FOURNISSEURS
export const getFamilies = () => api.get('/families');
export const getSubFamiliesByFamily = (familyId: number) => api.get(`/sub-families/family/${familyId}`);
export const getSuppliers = () => api.get('/suppliers');
export const getDemandeOffres = (daId: number) => api.get(`/demandes/${daId}/offres`);
export const postDemandeOffre = (daId: number, data: { fournisseurId: number; prixPropose: number; delai: number; conditions: string }) => api.post(`/demandes/${daId}/offres`, data);

// ARTICLES & STOCK
export const getArticles = () => api.get('/warehouse/stock');
export const getStockItems = () => api.get('/warehouse/stock');

// ── ADMIN & GLOBAL ─────────────────────────────────────
export const getAllDA = () => api.get('/demandes');
export const getUsers = () => api.get('/users');
export const deleteDA = (id: number) => api.delete(`/demandes/${id}`);
export const getAllTransfers = () => api.get('/transfers/all');
export const getAllOffres = () => api.get('/demandes/offres/all');

// ── DAF / DG ─────────────────────────────────────────
export const getSubFamilies = () => api.get('/sub-families');
export const adjustSubFamily = (daId: number, userId: number, sourceId: number, cibleId: number, montant: number) =>
  api.post(`/workflow/adjust-subfamily?daId=${daId}&dafId=${userId}&sourceId=${sourceId}&cibleId=${cibleId}&montant=${montant}`);
export const adjustFamily = (daId: number, userId: number, familyId: number, montant: number) =>
  api.post(`/workflow/adjust-family?daId=${daId}&dgId=${userId}&cibleId=${familyId}&montant=${montant}`);
export const validateWorkflow = (daId: number, userId: number, decision: string, comment: string) =>
  api.post(`/demandes/${daId}/validate?userId=${userId}&decision=${decision}&comment=${encodeURIComponent(comment)}`);
export const checkBudgetClassic = (daId: number, acheteurId: number) => 
  api.post(`/workflow/check-budget?daId=${daId}&acheteurId=${acheteurId}`);

// ── GRN / GRC ────────────────────────────────────────
export const getGrnByStatus = (status: string) => api.get(`/grn/status/${status}`);
export const getAllGrns = () => api.get('/grn');
export const createGrc = (payload: any) => api.post('/grc', payload);
export const validateGrc = (id: number) => api.put(`/grc/${id}/valider`);
export const approveGrc = (id: number) => api.put(`/grc/${id}/approuver`);
export const getAllGrcs = () => api.get('/grc');

// ── INVOICE ──────────────────────────────────────────
export const getInvoices = () => api.get('/invoice');
export const createInvoice = (payload: any) => api.post('/invoice', payload);
export const matchInvoice = (id: number) => api.post(`/invoice/${id}/match`);
export const approveInvoice = (id: number) => api.post(`/invoice/${id}/approve`);

// ── DOCUMENTS (PDFs) ─────────────────────────────────
export const downloadPoPdf = (id: number) => api.get(`/purchase-orders/${id}/download`, { responseType: 'blob' });
export const downloadGrnPdf = (id: number) => api.get(`/grn/${id}/download`, { responseType: 'blob' });
export const downloadGrcPdf = (id: number) => api.get(`/grc/${id}/download`, { responseType: 'blob' });
export const downloadInvoicePdf = (id: number) => api.get(`/invoice/${id}/download`, { responseType: 'blob' });

export const getPurchaseOrdersByStatus = (status: string) => api.get(`/purchase-orders/status/${status}`);
export const getAllPurchaseOrders = () => api.get('/purchase-orders');
export const getPoBalance = (poId: number) => api.get(`/purchase-orders/${poId}/balance`);

export const createGrn = (payload: any) => 
  api.post('/grn', payload);

export const validateGrn = (grnId: number) => 
  api.put(`/grn/${grnId}/valider`);

export const downloadPOByDA = (daId: number) => 
  api.get(`/purchase-orders/da/${daId}/download`, { responseType: 'blob' });

export const getPurchaseOrders = () => 
  api.get('/purchase-orders');

export const approvePO = (id: number, userId: number, commentaire?: string) => 
  api.put(`/purchase-orders/${id}/approve?userId=${userId}${commentaire ? '&commentaire=' + encodeURIComponent(commentaire) : ''}`);

export const rejectPO = (id: number, userId: number, motif: string) => 
  api.put(`/purchase-orders/${id}/reject?userId=${userId}&motif=${encodeURIComponent(motif)}`);

export const createPO = (daId: number, userId: number) => 
  api.post(`/purchase-orders?daId=${daId}&userId=${userId}`);

export const solliciterAjustementDemandeInterne = (daId: number, type: 'SOUS_FAMILLE' | 'FAMILLE', userId: number) => 
  api.post(`/demandes/${daId}/ajustement?type=${type}&userId=${userId}`);

// ── CHATBOT ──────────────────────────────────────────
export const demarrerChatbotSession = (userId: number) => 
  api.post(`/chatbot/session?userId=${userId}`);

export const envoyerChatbotMessage = (sessionId: string, userId: number, message: string) => 
  api.post('/chatbot/message', { sessionId, userId, message });

export const confirmerChatbotDemande = (sessionId: string, userId: number) => 
  api.post('/chatbot/confirmer', { sessionId, userId });

export const getChatbotMessages = (sessionId: string, userId: number) => 
  api.get(`/chatbot/session/${sessionId}/messages?userId=${userId}`);

// ── TRANSFERT INTER-SITES (LTO/LTI) ──────────────────────────────────────────
export const getWarehouses = () => api.get('/warehouse');

/** Stock disponible avec warehouse (RISQUE-13 : JOIN FETCH garanti côté backend). */
export const getAvailableStock = () => api.get('/transfers/stock/available');

/** Soumet une demande de transfert groupée multi-sources (MAGASINIER). */
export const submitBulkTransfers = (payload: any, userId: number) =>
  api.post(`/transfers/bulk?userId=${userId}`, payload);

/** Expédie le transfert PENDING → IN_TRANSIT (MAGASINIER source). */
export const shipTransfer = (id: number, userId: number, payload: any) =>
  api.put(`/transfers/${id}/ship?userId=${userId}`, payload);

/** Valide la réception IN_TRANSIT → RECEIVED (MAGASINIER_DEST). */
export const receiveTransfer = (id: number, userId: number, payload: any) =>
  api.put(`/transfers/${id}/receive?userId=${userId}`, payload);

/** Annule le transfert PENDING → CANCELLED (auteur ou ADMINISTRATEUR). */
export const cancelTransfer = (id: number, userId: number) =>
  api.delete(`/transfers/${id}?userId=${userId}`);

/** Historique des transferts soumis par l'employé. */
export const getMyTransfers = (userId: number) =>
  api.get(`/transfers/my?userId=${userId}`);

/** File PENDING pour le MAGASINIER source (onglet expédition). */
export const getSourceTransfers = (userId: number) =>
  api.get(`/transfers/source?userId=${userId}`);

/** File IN_TRANSIT pour le MAGASINIER_DEST (vue réception). */
export const getDestTransfers = (userId: number) =>
  api.get(`/transfers/dest?userId=${userId}`);

/** Historique d'expédition pour MAGASINIER source. */
export const getSourceHistory = (userId: number) =>
  api.get(`/transfers/history/source?userId=${userId}`);

/** Historique de réception pour MAGASINIER_DEST. */
export const getDestHistory = (userId: number) =>
  api.get(`/transfers/history/dest?userId=${userId}`);

/** Télécharge le LTO en PDF (Blob). */
export const downloadLtoPdf = (id: number) =>
  api.get(`/transfers/${id}/pdf/lto`, { responseType: 'blob' });

/** Télécharge le LTI en PDF (Blob). */
export const downloadLtiPdf = (id: number) =>
  api.get(`/transfers/${id}/pdf/lti`, { responseType: 'blob' });
