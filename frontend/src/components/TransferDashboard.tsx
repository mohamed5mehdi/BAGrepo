import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { getSourceTransfers, shipTransfer, receiveTransfer, submitBulkTransfers, getSourceHistory, downloadLtoPdf, getAvailableStock, getDestHistory, downloadLtiPdf } from '../api/services';
import type { PurchaseOrder, GrnHeader } from '../types';


export default function TransferDashboard() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<'grn' | 'stock' | 'transferReq' | 'transferOut' | 'transferIn' | 'documents'>(
    user?.role === 'MAGASINIER' ? 'grn' : 'transferOut'
  );
  const [selectedPo, setSelectedPo] = useState<PurchaseOrder | null>(null);
  const [deliveryNote, setDeliveryNote] = useState('');
  const [isEntryCompleted, setIsEntryCompleted] = useState(true);
  const [grnItems, setGrnItems] = useState<any[]>([]);

  // ── Modal confirmation expédition ───────────────────────────────────────────
  const [shipConfirmTransfer, setShipConfirmTransfer] = useState<any | null>(null);
  const [shipQuantities, setShipQuantities] = useState<Record<number, number>>({});
  
  // ── Modal confirmation réception ────────────────────────────────────────────
  const [receiveConfirmTransfer, setReceiveConfirmTransfer] = useState<any | null>(null);
  const [receiveQuantities, setReceiveQuantities] = useState<Record<number, number>>({});
  
  // ── Modal demande transfert (Panier) ────────────────────────────────────────
  const [showBulkTransferModal, setShowBulkTransferModal] = useState(false);
  const [transferCart, setTransferCart] = useState<{stockItemId: number, qty: number, maxQty: number, itemCode: string, itemName: string, sourceWarehouseId: number, sourceWarehouseName: string}[]>([]);
  const [stockSearch, setStockSearch] = useState('');

  // ── Modal LTO généré ────────────────────────────────────────────────────────
  const [ltoResult, setLtoResult] = useState<{ lto: string; trf: string } | null>(null);

  const userId = user?.userId;

  // CRITIQUE : warehouse n'est PAS dans AuthUser — on le charge via l'API
  const { data: userWarehouse } = useQuery<{ id: number; name: string } | null>({
    queryKey: ['user-warehouse', userId],
    queryFn: async () => {
      const res = await api.get(`/users/me`);
      const wh = res.data?.warehouse;
      return wh ? { id: wh.id, name: wh.name } : null;
    },
    enabled: !!userId,
    staleTime: 5 * 60 * 1000,
  });
  const userWarehouseId = userWarehouse?.id ?? null;
  const userWarehouseName = userWarehouse?.name ?? '—';


  // 1. Liste PO filtrée : APPROVED uniquement
  const { data: approvedPOs = [], isLoading } = useQuery({
    queryKey: ['pos', 'approved'],
    queryFn: () => api.get('/purchase-orders/status/APPROVED').then(r => r.data),
    refetchInterval: 15_000,
    enabled: !!user,
  });

  // 2. File de transferts PENDING (onglet expédition)
  const { data: pendingTransfers = [], isLoading: loadingTransfers } = useQuery<any[]>({
    queryKey: ['transfers', 'source', userId],
    queryFn: () => getSourceTransfers(userId).then(r => r.data),
    refetchInterval: 20_000,
    enabled: !!userId,
  });

  // 2b. File de transferts IN_TRANSIT (onglet réception)
  const { data: destTransfers = [], isLoading: loadingDestTransfers } = useQuery<any[]>({
    queryKey: ['transfers', 'dest', userId],
    queryFn: () => api.get(`/transfers/dest?userId=${userId}`).then(r => r.data),
    refetchInterval: 20_000,
    enabled: !!userId,
  });

  // 2c. Stock global pour demande de transfert — enabled seulement quand warehouse connu
  const { data: availableStock = [] } = useQuery<any[]>({
    queryKey: ['transfers', 'stock'],
    queryFn: () => getAvailableStock().then(r => r.data),
    enabled: userWarehouseId !== undefined && userWarehouseId !== null,
  });

  // 3. Historique des transferts expédiés (LTO)
  const { data: historyTransfers = [] } = useQuery<any[]>({
    queryKey: ['transfers', 'history', 'source', userId],
    queryFn: () => getSourceHistory(userId).then(r => r.data),
    refetchInterval: 30_000,
    enabled: !!userId && activeTab === 'documents',
  });

  // Historique des transferts reçus (LTI)
  const { data: destHistoryTransfers = [] } = useQuery<any[]>({
    queryKey: ['transfers', 'history', 'dest', userId],
    queryFn: () => getDestHistory(userId).then(r => r.data),
    refetchInterval: 30_000,
    enabled: !!userId && activeTab === 'documents',
  });

  // 4. Mes Demandes de transferts (initiées par l'utilisateur courant)
  const { data: myTransfers = [], isLoading: loadingMyTransfers } = useQuery<any[]>({
    queryKey: ['transfers', 'my', userId],
    queryFn: () => api.get(`/transfers/my?userId=${userId}`).then(r => r.data),
    refetchInterval: 20_000,
    enabled: !!userId,
  });

  const handleDownloadLto = async (id: number, ltoNumber: string) => {
    try {
      const res = await downloadLtoPdf(id);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${ltoNumber}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (e) {
      toast.error('Erreur lors du téléchargement du PDF');
    }
  };

  const handleDownloadLti = async (id: number, ltiNumber: string) => {
    try {
      const res = await downloadLtiPdf(id);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${ltiNumber}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (e) {
      toast.error('Erreur lors du téléchargement du PDF');
    }
  };

  const shipMutation = useMutation({
    mutationFn: (payload: {headerId: number, reqBody: any}) => shipTransfer(payload.headerId, userId, payload.reqBody),
    onSuccess: (res) => {
      const header = res.data;
      const lto = header?.ltoNumber ?? 'LTO généré';
      const trf = `TRF-${header?.id ?? ''}` ;
      setLtoResult({ lto, trf });
      setShipConfirmTransfer(null);
      setShipQuantities({});
      qc.invalidateQueries({ queryKey: ['transfers', 'source'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de l\'expédition');
    },
  });

  const receiveTransferMut = useMutation({
    mutationFn: (payload: {headerId: number, reqBody: any}) => receiveTransfer(payload.headerId, userId, payload.reqBody).then(r => r.data),
    onSuccess: (res) => {
      const header = res;
      toast.success(`Transfert TRF-${header.id} réceptionné avec succès !`);
      setReceiveConfirmTransfer(null);
      setReceiveQuantities({});
      qc.invalidateQueries({ queryKey: ['transfers'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la réception');
    },
  });

  const submitBulkMutation = useMutation({
    mutationFn: (payload: any) => submitBulkTransfers(payload, userId),
    onSuccess: () => {
      toast.success('Transfert inter-sites demandé avec succès !');
      setShowBulkTransferModal(false);
      setTransferCart([]);
      qc.invalidateQueries({ queryKey: ['transfers'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la soumission de la demande');
    }
  });

  // Fetch balance for selected PO
  const { data: poBalance = {}, isFetching: loadingBalance } = useQuery({
    queryKey: ['po-balance', selectedPo?.id_po],
    queryFn: () => api.get(`/purchase-orders/${selectedPo!.id_po}/balance`).then(r => r.data),
    enabled: !!selectedPo,
  });

  const openGrnForm = (po: PurchaseOrder) => {
    setSelectedPo(po);
    setDeliveryNote('');
    setIsEntryCompleted(true);
    
    // Initialisation des lignes GRN selon le flux
    setGrnItems([{
      itemCode: '', // Libre pour flux Interne
      itemName: po.demandeInterne!.designation,
      orderedQuantity: po.demandeInterne!.quantite,
      receivedQuantity: 0,
      shippedQuantity: 0,
    }]);
  };

  const createGrnMutation = useMutation({
    mutationFn: async (payload: Partial<GrnHeader>) => {
      const res = await api.post('/grn', payload);
      return res.data;
    },
    onSuccess: () => {
      toast.success(isEntryCompleted ? '📦 Réception terminée et stock mis à jour !' : '📝 Brouillon GRN créé.');
      qc.invalidateQueries({ queryKey: ['pos'] });
      setSelectedPo(null);
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la réception');
    }
  });

  const handleReceivedChange = (index: number, val: number) => {
    const newItems = [...grnItems];
    const item = newItems[index];
    const shipped = getShippedQty(item.itemCode);
    
    if (val > shipped) {
      toast.error(`Quantité invalide : le reliquat (Shipped Qty) est de ${shipped}`);
      return;
    }
    item.receivedQuantity = val;
    setGrnItems(newItems);
  };

  const getShippedQty = (itemCode: string) => {
    const isInternal = !!selectedPo?.demandeInterne;
    const ordered = isInternal ? selectedPo!.demandeInterne!.quantite : (grnItems.find(i => i.itemCode === itemCode)?.orderedQuantity || 0);
    const received = isInternal ? (poBalance["GLOBAL"] || 0) : (poBalance[itemCode] || 0);
    return Math.max(0, ordered - received);
  };

  const isFormValid = () => {
    if (!deliveryNote.trim()) return false;
    if (grnItems.some(i => i.receivedQuantity <= 0)) return false;
    if (!!selectedPo?.demandeInterne && !grnItems[0].itemCode.trim()) return false;
    return true;
  };

  return (
    <>
    <DashboardLayout title="Espace Magasin — Réceptions BAG ERP" pendingCount={approvedPOs.length + pendingTransfers.length}>
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        {user?.role === 'MAGASINIER' && (
          <div onClick={() => setActiveTab('grn')} className="cursor-pointer transition-transform hover:scale-105">
              <KpiCard label="Commandes en attente" value={approvedPOs.length} icon="🚚" color="from-blue-600 to-indigo-700" />
          </div>
        )}
        <div onClick={() => setActiveTab('transferOut')} className="cursor-pointer transition-transform hover:scale-105">
            <KpiCard label="Transferts à expédier" value={pendingTransfers.length} icon="📦" color="from-amber-500 to-orange-600" />
        </div>
        <div onClick={() => setActiveTab('transferIn')} className="cursor-pointer transition-transform hover:scale-105">
            <KpiCard label="Transferts en réception" value={destTransfers.length} icon="📥" color="from-emerald-500 to-teal-600" />
        </div>
        <div className="cursor-not-allowed opacity-80">
            <KpiCard label="Alertes Stock" value={0} icon="⚠️" color="from-rose-500 to-red-600" />
        </div>
      </div>

      {/* Tab switcher */}
      <div className="flex flex-wrap justify-between items-center gap-4 mb-6">
        <div className="flex flex-wrap gap-2">
          {user?.role === 'MAGASINIER' && (
            <button
              onClick={() => setActiveTab('grn')}
              className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
                activeTab === 'grn'
                  ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-100'
                  : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
              }`}
            >
              📋 Réceptions GRN ({approvedPOs.length})
            </button>
          )}
          <button
            onClick={() => setActiveTab('transferOut')}
            className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'transferOut'
                ? 'bg-amber-500 text-white shadow-lg shadow-amber-100'
                : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
            }`}
          >
            📤 À Expédier ({pendingTransfers.length})
          </button>
          <button
            onClick={() => setActiveTab('transferIn')}
            className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'transferIn'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-100'
                : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
            }`}
          >
            📥 En Réception ({destTransfers.length})
          </button>
          <button
            onClick={() => setActiveTab('transferReq')}
            className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'transferReq'
                ? 'bg-sky-600 text-white shadow-lg shadow-sky-100'
                : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
            }`}
          >
            📦 Mes Demandes ({myTransfers.length})
          </button>
          <button
            onClick={() => setActiveTab('documents')}
            className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'documents'
                ? 'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-200 shadow-sm border border-slate-200 dark:border-slate-600'
                : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
            }`}
          >
            🕒 Historique & LTO
          </button>
        </div>
        
        <button
            onClick={() => setShowBulkTransferModal(true)}
            className="px-6 py-2 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 text-white font-black shadow-lg shadow-indigo-200 hover:opacity-90 transition-all flex items-center gap-2"
        >
            <span>➕</span> Demander un Transfert
        </button>
      </div>

      {/* ── Onglet GRN ── */}
      {activeTab === 'grn' && (
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
            <h2 className="text-lg font-black text-slate-800 dark:text-white">Bons de Commande Approuvés</h2>
            <button onClick={() => qc.invalidateQueries({ queryKey: ['pos'] })} className="text-xs font-bold text-indigo-600 hover:underline">🔄 Actualiser</button>
        </div>
        
        <table className="w-full text-sm text-left">
          <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
            <tr>
              <th className="px-6 py-4">N° PO</th>
              <th className="px-6 py-4">Flux</th>
              <th className="px-6 py-4">Fournisseur</th>
              <th className="px-6 py-4">Date Commande</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
            {approvedPOs.map((po: any) => (
              <tr key={po.id_po} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                <td className="px-6 py-4 font-mono font-bold text-blue-600">{po.poNumber || `PO-${po.id_po}`}</td>
                <td className="px-6 py-4">
                  <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase bg-violet-100 text-violet-600`}>
                    Interne
                  </span>
                </td>
                <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">{po.fournisseur?.nom || '—'}</td>
                <td className="px-6 py-4 text-slate-500">{new Date(po.date_creation).toLocaleDateString()}</td>
                <td className="px-6 py-4 text-right">
                  <button onClick={() => openGrnForm(po)} className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold hover:bg-indigo-700 transition-all shadow-lg shadow-indigo-100">
                    📦 Réceptionner
                  </button>
                </td>
              </tr>
            ))}
            {approvedPOs.length === 0 && !isLoading && (
              <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Aucune commande approuvée en attente de réception.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      )} {/* fin onglet GRN */}

      {/* ── Onglet View Stock ── */}
      {activeTab === 'stock' && (
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden animate-fade-in">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-sky-50 to-transparent dark:from-sky-900/10">
            <div>
                <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                    <span className="w-8 h-8 rounded-xl bg-sky-100 dark:bg-sky-900/30 text-sky-600 flex items-center justify-center">👁️</span>
                    Vue Globale des Stocks
                </h2>
                <p className="text-xs text-slate-500 mt-1">Tous les articles disponibles sur les 4 sites</p>
            </div>
            <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'stock'] })} className="text-xs font-bold text-sky-600 hover:underline">🔄 Actualiser</button>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
              <tr>
                <th className="px-6 py-4">Code Site</th>
                <th className="px-6 py-4">Nom du Site</th>
                <th className="px-6 py-4">Code Article</th>
                <th className="px-6 py-4">Désignation</th>
                <th className="px-6 py-4 text-right">Quantité Disponible</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
              {availableStock.map((item: any) => (
                <tr key={`${item.warehouseId}-${item.itemCode}`} className="hover:bg-sky-50/30 dark:hover:bg-sky-900/10 transition-colors">
                  <td className="px-6 py-4 font-mono font-bold text-slate-700 dark:text-slate-300">WH-{item.warehouseId}</td>
                  <td className="px-6 py-4 font-semibold text-slate-800 dark:text-slate-200">{item.warehouseName}</td>
                  <td className="px-6 py-4 font-mono font-bold text-sky-600">{item.itemCode}</td>
                  <td className="px-6 py-4 font-medium text-slate-600 dark:text-slate-400">{item.itemName}</td>
                  <td className="px-6 py-4 text-right">
                    <span className="px-3 py-1 bg-slate-100 dark:bg-slate-800 rounded-lg font-black text-slate-700 dark:text-slate-300">
                      {item.quantity}
                    </span>
                  </td>
                </tr>
              ))}
              {availableStock.length === 0 && (
                <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Aucun stock disponible.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      )}

      {/* ── Onglet Mes Demandes OUT ── */}
      {activeTab === 'transferOut' && (
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
          <h2 className="text-lg font-black text-slate-800 dark:text-white">Mes Demandes OUT (À Expédier)</h2>
          <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'source'] })} className="text-xs font-bold text-amber-600 hover:underline">🔄 Actualiser</button>
        </div>
        <table className="w-full text-sm text-left">
          <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
            <tr>
              <th className="px-6 py-4">N° Demande</th>
              <th className="px-6 py-4">Demandeur</th>
              <th className="px-6 py-4">Destination</th>
              <th className="px-6 py-4">Articles</th>
              <th className="px-6 py-4">Soumis le</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
            {pendingTransfers.map((t: any) => (
              <tr key={t.id} className="hover:bg-amber-50/30 dark:hover:bg-amber-900/10 transition-colors">
                <td className="px-6 py-4 font-mono font-bold text-amber-600">TRF-{t.id}</td>
                <td className="px-6 py-4 text-slate-600 dark:text-slate-300">{t.requestedBy?.nom}</td>
                <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">{t.warehouseDest?.name}</td>
                <td className="px-6 py-4">
                  <span className="bg-slate-100 text-slate-600 px-2 py-0.5 rounded text-[9px] font-black">{t.lines?.length ?? 0} article(s)</span>
                </td>
                <td className="px-6 py-4 text-slate-500">{new Date(t.createdAt).toLocaleDateString()}</td>
                <td className="px-6 py-4 text-right">
                  <button
                    onClick={() => {
                      const initialQties: Record<number, number> = {};
                      t.lines?.forEach((l: any) => initialQties[l.id] = l.quantityRequested);
                      setShipQuantities(initialQties);
                      setShipConfirmTransfer(t);
                    }}
                    disabled={shipMutation.isPending}
                    className="px-4 py-2 rounded-xl bg-amber-500 text-white text-xs font-bold hover:bg-amber-600 transition-all shadow-lg shadow-amber-100 disabled:opacity-50"
                  >
                    🚚 Expédier
                  </button>
                </td>
              </tr>
            ))}
            {pendingTransfers.length === 0 && !loadingTransfers && (
              <tr><td colSpan={6} className="px-6 py-20 text-center text-slate-400 italic">Aucun transfert en attente d'expédition depuis votre entrepôt.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      )} {/* fin onglet Transferts */}

      {/* ── Onglet Documents (LTO & LTI) ── */}
      {activeTab === 'documents' && (
      <div className="space-y-6 animate-fade-in">
        {/* Table LTO */}
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
          <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-slate-50 to-transparent dark:from-slate-800/50">
            <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                <span className="w-8 h-8 rounded-xl bg-slate-200 dark:bg-slate-700 flex items-center justify-center">📤</span>
                Documents LTO (Transferts Sortants)
            </h2>
            <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'history', 'source'] })} className="text-xs font-bold text-slate-600 hover:underline">🔄 Actualiser</button>
          </div>
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
              <tr>
                <th className="px-6 py-4">N° LTO</th>
                <th className="px-6 py-4">Statut</th>
                <th className="px-6 py-4">Destination</th>
                <th className="px-6 py-4">Expédié le</th>
                <th className="px-6 py-4 text-right">Documents</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
              {historyTransfers.map((t: any) => (
                <tr key={t.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                  <td className="px-6 py-4 font-mono font-bold text-slate-700 dark:text-white">{t.ltoNumber}</td>
                  <td className="px-6 py-4">
                    <span className={`px-2 py-0.5 rounded text-[9px] font-black ${
                      t.status === 'RECEIVED' ? 'bg-emerald-100 text-emerald-600' : 'bg-blue-100 text-blue-600'
                    }`}>
                      {t.status === 'RECEIVED' ? 'RÉCEPTIONNÉ' : 'EN TRANSIT'}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-semibold text-slate-600 dark:text-slate-300">{t.warehouseDest?.name}</td>
                  <td className="px-6 py-4 text-slate-500">{t.shippedAt ? new Date(t.shippedAt).toLocaleString() : '—'}</td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => handleDownloadLto(t.id, t.ltoNumber)}
                      className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 text-xs font-bold hover:bg-slate-200 dark:hover:bg-slate-600 transition-all"
                    >
                      📄 PDF LTO
                    </button>
                  </td>
                </tr>
              ))}
              {historyTransfers.length === 0 && (
                <tr><td colSpan={5} className="px-6 py-10 text-center text-slate-400 italic">Aucun document LTO.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Table LTI */}
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
          <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-emerald-50 to-transparent dark:from-emerald-900/10">
            <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                <span className="w-8 h-8 rounded-xl bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 flex items-center justify-center">📥</span>
                Documents LTI (Transferts Entrants)
            </h2>
            <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'history', 'dest'] })} className="text-xs font-bold text-emerald-600 hover:underline">🔄 Actualiser</button>
          </div>
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
              <tr>
                <th className="px-6 py-4">N° LTI</th>
                <th className="px-6 py-4">Source</th>
                <th className="px-6 py-4">Réceptionné le</th>
                <th className="px-6 py-4 text-right">Documents</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
              {destHistoryTransfers.map((t: any) => (
                <tr key={t.id} className="hover:bg-emerald-50/30 dark:hover:bg-emerald-900/10 transition-colors">
                  <td className="px-6 py-4 font-mono font-bold text-slate-700 dark:text-white">{t.ltiNumber}</td>
                  <td className="px-6 py-4 font-semibold text-slate-600 dark:text-slate-300">{t.warehouseSource?.name}</td>
                  <td className="px-6 py-4 text-slate-500">{t.receivedAt ? new Date(t.receivedAt).toLocaleString() : '—'}</td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => handleDownloadLti(t.id, t.ltiNumber)}
                      className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 text-xs font-bold hover:bg-slate-200 dark:hover:bg-slate-600 transition-all"
                    >
                      📄 PDF LTI
                    </button>
                  </td>
                </tr>
              ))}
              {destHistoryTransfers.length === 0 && (
                <tr><td colSpan={4} className="px-6 py-10 text-center text-slate-400 italic">Aucun document LTI.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      )}

      {/* ── Onglet Mes Demandes IN ── */}
      {activeTab === 'transferIn' && (
      <div className="space-y-6 animate-fade-in">
        {/* Table Mes Demandes initiées */}
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
          <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-blue-50 to-transparent dark:from-blue-900/10">
              <div>
                  <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                      <span className="w-8 h-8 rounded-xl bg-blue-100 dark:bg-blue-900/30 text-blue-600 flex items-center justify-center">📤</span>
                      Mes Demandes IN (Statut des requêtes)
                  </h2>
                  <p className="text-xs text-slate-500 mt-1">Suivez l'état des demandes que vous avez envoyées aux autres entrepôts pour recevoir du stock</p>
              </div>
              <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'my'] })} className="text-xs font-bold text-blue-600 hover:underline">🔄 Actualiser</button>
          </div>
        <table className="w-full text-sm text-left">
          <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
            <tr>
              <th className="px-6 py-4">N° Demande</th>
              <th className="px-6 py-4">Source Sollicitée</th>
              <th className="px-6 py-4">Articles</th>
              <th className="px-6 py-4">Statut</th>
              <th className="px-6 py-4">Créée le</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
            {myTransfers.map((t: any) => (
              <tr key={t.id} className="hover:bg-blue-50/30 dark:hover:bg-blue-900/10 transition-colors">
                <td className="px-6 py-4 font-mono font-bold text-blue-600">TRF-{t.id}</td>
                <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">{t.warehouseSource?.name}</td>
                <td className="px-6 py-4">
                  <span className="bg-slate-100 text-slate-600 px-2 py-0.5 rounded text-[9px] font-black">{t.lines?.length ?? 0} article(s)</span>
                </td>
                <td className="px-6 py-4">
                  <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase ${
                    t.status === 'PENDING' ? 'bg-amber-100 text-amber-600' :
                    t.status === 'IN_TRANSIT' ? 'bg-blue-100 text-blue-600' :
                    t.status === 'RECEIVED' ? 'bg-emerald-100 text-emerald-600' : 'bg-slate-100 text-slate-600'
                  }`}>
                    {t.status === 'PENDING' ? 'En attente d\'expédition' :
                     t.status === 'IN_TRANSIT' ? 'Expédié (LTO)' :
                     t.status === 'RECEIVED' ? 'Réceptionné' : t.status}
                  </span>
                </td>
                <td className="px-6 py-4 text-slate-500">{new Date(t.createdAt).toLocaleDateString()}</td>
              </tr>
            ))}
            {myTransfers.length === 0 && !loadingMyTransfers && (
              <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Vous n'avez effectué aucune demande de transfert.</td></tr>
            )}
          </tbody>
        </table>
      </div>

        {/* Table Transferts en Réception */}
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
          <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-emerald-50 to-transparent dark:from-emerald-900/10">
              <div>
                  <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                      <span className="w-8 h-8 rounded-xl bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 flex items-center justify-center">📥</span>
                      Transferts Physiques en Réception
                  </h2>
                  <p className="text-xs text-slate-500 mt-1">Acceptez les transferts en provenance d'autres entrepôts (LTO générés)</p>
              </div>
              <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'dest'] })} className="text-xs font-bold text-emerald-600 hover:underline">🔄 Actualiser</button>
          </div>
        
        <table className="w-full text-sm text-left">
          <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
            <tr>
              <th className="px-6 py-4">N° TRF / LTO</th>
              <th className="px-6 py-4">Source</th>
              <th className="px-6 py-4">Articles</th>
              <th className="px-6 py-4">Date Expédition</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
            {destTransfers.map((t: any) => (
              <tr key={t.id} className="hover:bg-emerald-50/30 dark:hover:bg-emerald-900/10 transition-colors">
                <td className="px-6 py-4">
                  <div className="font-mono font-bold text-emerald-600">TRF-{t.id}</div>
                  {t.ltoNumber && <div className="text-xs font-mono text-slate-400 mt-1">{t.ltoNumber}</div>}
                </td>
                <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">{t.warehouseSource?.name}</td>
                <td className="px-6 py-4 font-medium text-slate-600">{t.lines?.length || 0} article(s)</td>
                <td className="px-6 py-4 text-slate-500">{t.shippedAt ? new Date(t.shippedAt).toLocaleString() : '—'}</td>
                <td className="px-6 py-4 text-right">
                  <button
                    onClick={() => {
                      const initialQties: Record<number, number> = {};
                      t.lines?.forEach((l: any) => initialQties[l.id] = l.quantityShipped || l.quantityRequested);
                      setReceiveQuantities(initialQties);
                      setReceiveConfirmTransfer(t);
                    }}
                    disabled={receiveTransferMut.isPending}
                    className="px-5 py-2 rounded-xl bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-100 disabled:opacity-50"
                  >
                    {receiveTransferMut.isPending ? 'Chargement...' : '✅ Réceptionner tout'}
                  </button>
                </td>
              </tr>
            ))}
            {destTransfers.length === 0 && !loadingDestTransfers && (
              <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Aucun transfert en attente de réception pour votre entrepôt.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      </div>
      )}

      {/* ── Onglet Transfer Request ── */}
      {activeTab === 'transferReq' && (
      <div className="space-y-6 animate-fade-in">
        {/* Header */}
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
          <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-sky-50 to-transparent dark:from-sky-900/10">
            <div>
              <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                <span className="w-8 h-8 rounded-xl bg-sky-100 dark:bg-sky-900/30 text-sky-600 flex items-center justify-center">📦</span>
                Mes Demandes de Transfert
              </h2>
              <p className="text-xs text-slate-500 mt-1">
                Suivez l'état de toutes les demandes de transfert que vous avez initiées.
              </p>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'my'] })}
                className="text-xs font-bold text-sky-600 hover:underline"
              >
                🔄 Actualiser
              </button>
              <button
                onClick={() => setShowBulkTransferModal(true)}
                className="px-4 py-2 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 text-white font-black text-xs shadow-lg hover:opacity-90 transition-all flex items-center gap-2"
              >
                ➕ Nouvelle Demande
              </button>
            </div>
          </div>

          {/* Table */}
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
              <tr>
                <th className="px-6 py-4">N° Demande</th>
                <th className="px-6 py-4">Source Sollicitée</th>
                <th className="px-6 py-4">Destination</th>
                <th className="px-6 py-4">Articles</th>
                <th className="px-6 py-4">Statut</th>
                <th className="px-6 py-4">Créée le</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
              {myTransfers.length === 0 && !loadingMyTransfers && (
                <tr>
                  <td colSpan={6} className="px-6 py-20 text-center">
                    <div className="flex flex-col items-center gap-4 text-slate-400">
                      <span className="text-5xl">📋</span>
                      <p className="italic">Aucune demande de transfert initiée pour l'instant.</p>
                      <button
                        onClick={() => setShowBulkTransferModal(true)}
                        className="px-6 py-3 rounded-2xl bg-gradient-to-r from-violet-600 to-indigo-600 text-white font-black text-sm shadow-xl hover:opacity-90 transition-all"
                      >
                        🚀 Démarrer la Saisie
                      </button>
                    </div>
                  </td>
                </tr>
              )}
              {loadingMyTransfers && (
                <tr>
                  <td colSpan={6} className="px-6 py-10 text-center text-slate-400 italic animate-pulse">
                    Chargement des demandes...
                  </td>
                </tr>
              )}
              {myTransfers.map((t: any) => (
                <tr key={t.id} className="hover:bg-sky-50/30 dark:hover:bg-sky-900/10 transition-colors">
                  <td className="px-6 py-4 font-mono font-bold text-sky-600">
                    TRF-{String(t.id).padStart(5, '0')}
                  </td>
                  <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">
                    {t.warehouseSource?.name ?? '—'}
                  </td>
                  <td className="px-6 py-4 text-slate-600 dark:text-slate-300">
                    {t.warehouseDest?.name ?? '—'}
                  </td>
                  <td className="px-6 py-4">
                    <span className="bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 px-2 py-0.5 rounded text-[9px] font-black">
                      {t.lines?.length ?? 0} article(s)
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`px-2.5 py-1 rounded-lg text-[9px] font-black uppercase ${
                      t.status === 'PENDING'     ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300' :
                      t.status === 'IN_TRANSIT'  ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300' :
                      t.status === 'RECEIVED'    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300' :
                                                   'bg-slate-100 text-slate-600'
                    }`}>
                      {t.status === 'PENDING'    ? '⏳ En attente d\'expédition' :
                       t.status === 'IN_TRANSIT' ? '🚚 En Transit (LTO)' :
                       t.status === 'RECEIVED'   ? '✅ Réceptionné' : t.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-slate-400 text-xs">
                    {t.createdAt ? new Date(t.createdAt).toLocaleDateString('fr-FR') : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      )}

      {selectedPo && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="bg-white dark:bg-slate-900 w-full max-w-4xl rounded-[32px] shadow-2xl overflow-hidden border border-white/20">
                <div className="p-8 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
                    <div>
                        <h2 className="text-2xl font-black text-slate-900 dark:text-white">Bon de Réception (GRN)</h2>
                        <p className="text-slate-400 text-sm mt-1">Réception liée au {selectedPo.poNumber}</p>
                    </div>
                    <button onClick={() => setSelectedPo(null)} className="p-2 hover:bg-slate-100 rounded-full transition-colors text-slate-400">✕</button>
                </div>

                <div className="p-8 space-y-8 max-h-[70vh] overflow-y-auto">
                    {/* Header Info */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <label className="text-[10px] font-black text-slate-400 uppercase ml-1">N° Bon de Livraison (BL)</label>
                            <input 
                                value={deliveryNote} onChange={e => setDeliveryNote(e.target.value)}
                                placeholder="Saisir le numéro du BL fournisseur..."
                                className="w-full px-5 py-3 rounded-2xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm font-bold focus:ring-2 focus:ring-indigo-500 outline-none"
                            />
                        </div>
                        <div className="bg-slate-50 dark:bg-slate-800 p-4 rounded-2xl border border-slate-100 dark:border-slate-700">
                            <p className="text-[10px] font-black text-slate-400 uppercase">Fournisseur</p>
                            <p className="font-bold text-slate-800 dark:text-slate-200">{selectedPo.fournisseur?.nom}</p>
                            <p className="text-[10px] text-slate-500 mt-1">{selectedPo.fournisseur?.adresse}</p>
                        </div>
                    </div>

                    {/* Items Table */}
                    <div className="space-y-4">
                        <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                            <span className="w-5 h-5 rounded-full bg-indigo-600 flex items-center justify-center text-[10px] text-white">i</span>
                            Lignes de Réception
                        </h3>
                        
                        <div className="border border-slate-100 dark:border-slate-800 rounded-2xl overflow-hidden">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold text-[10px] uppercase">
                                    <tr>
                                        <th className="px-6 py-4">Item Code</th>
                                        <th className="px-6 py-4">Désignation</th>
                                        <th className="px-6 py-4 text-center">Commandé</th>
                                        <th className="px-6 py-4 text-center">Reliquat (Shipped)</th>
                                        <th className="px-6 py-4 text-center">Reçu</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                                    {grnItems.map((item, idx) => (
                                        <tr key={idx}>
                                            <td className="px-6 py-4 font-mono font-bold text-indigo-600">
                                                {!!selectedPo.demandeInterne ? (
                                                    <input 
                                                        value={item.itemCode} 
                                                        onChange={e => {
                                                            const n = [...grnItems];
                                                            n[idx].itemCode = e.target.value;
                                                            setGrnItems(n);
                                                        }}
                                                        placeholder="Saisir Code"
                                                        className="w-24 px-2 py-1 rounded border border-indigo-200 text-xs focus:ring-1 focus:ring-indigo-500 outline-none"
                                                    />
                                                ) : item.itemCode}
                                            </td>
                                            <td className="px-6 py-4 font-medium text-slate-700 dark:text-slate-200">{item.itemName}</td>
                                            <td className="px-6 py-4 text-center font-bold text-slate-500">{item.orderedQuantity}</td>
                                            <td className="px-6 py-4 text-center">
                                                <span className="bg-amber-50 text-amber-600 px-2 py-1 rounded text-xs font-black">
                                                    {loadingBalance ? '...' : getShippedQty(item.itemCode)}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 text-center">
                                                <input 
                                                    type="number" 
                                                    value={item.receivedQuantity}
                                                    onChange={e => handleReceivedChange(idx, parseInt(e.target.value) || 0)}
                                                    className="w-20 px-3 py-1.5 rounded-xl border border-indigo-200 font-black text-center text-indigo-600 focus:ring-2 focus:ring-indigo-500 outline-none"
                                                />
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div className="flex items-center gap-3 p-4 bg-indigo-50 dark:bg-indigo-900/20 rounded-2xl border border-indigo-100 dark:border-indigo-800/50">
                        <input 
                            type="checkbox" id="entryComplete" checked={isEntryCompleted} onChange={e => setIsEntryCompleted(e.target.checked)}
                            className="w-5 h-5 rounded border-indigo-300 text-indigo-600 focus:ring-indigo-500"
                        />
                        <label htmlFor="entryComplete" className="text-sm font-bold text-indigo-900 dark:text-indigo-200">
                            Entry Completed (Mise à jour immédiate du stock et clôture de la saisie)
                        </label>
                    </div>
                </div>

                <div className="p-8 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-50 dark:border-slate-800 flex justify-end gap-4">
                    <button onClick={() => setSelectedPo(null)} className="px-8 py-3 rounded-2xl bg-white dark:bg-slate-700 text-slate-600 dark:text-slate-200 font-bold text-sm shadow-sm">Annuler</button>
                    <button 
                        onClick={() => createGrnMutation.mutate({
                            purchaseOrder: { id_po: selectedPo.id_po } as any,
                            deliveryNoteNumber: deliveryNote,
                            receiptDate: new Date().toISOString().split('T')[0],
                            status: 'PENDING',
                            details: grnItems.map(i => ({
                                itemCode: i.itemCode,
                                itemName: i.itemName,
                                receivedQuantity: i.receivedQuantity,
                                acceptedQuantity: i.receivedQuantity,
                                qualityStatus: 'APPROVED'
                            })) as any
                        })}
                        disabled={!isFormValid() || createGrnMutation.isPending}
                        className="px-12 py-3 rounded-2xl bg-indigo-600 text-white font-black text-sm shadow-xl shadow-indigo-100 hover:bg-indigo-700 transition-all disabled:opacity-50"
                    >
                        {createGrnMutation.isPending ? 'Chargement...' : '🚀 Valider la Réception'}
                    </button>
                </div>
            </div>
        </div>
      )}
    </DashboardLayout>

      {/* ── Modal Confirmation Expédition ─────────────────────────────────── */}
      {shipConfirmTransfer && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/70 backdrop-blur-sm">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-800 w-full max-w-md mx-4 overflow-hidden animate-fade-in">
            {/* Header */}
            <div className="bg-gradient-to-r from-amber-500 to-orange-500 p-6 flex items-center gap-4">
              <div className="w-12 h-12 bg-white/20 rounded-2xl flex items-center justify-center text-2xl">🚚</div>
              <div>
                <h3 className="text-white font-black text-lg">Confirmer l'expédition</h3>
                <p className="text-amber-100 text-sm">Cette action est irréversible</p>
              </div>
            </div>
            {/* Body */}
            <div className="p-6 space-y-4 max-h-[60vh] overflow-y-auto">
              <div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-2xl p-4 space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">N° Transfert</span>
                  <span className="font-bold text-amber-600">TRF-{shipConfirmTransfer.id}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">Demandeur</span>
                  <span className="font-semibold text-slate-700 dark:text-slate-200">{shipConfirmTransfer.requestedBy?.nom}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">Destination</span>
                  <span className="font-semibold text-slate-700 dark:text-slate-200">{shipConfirmTransfer.warehouseDest?.name}</span>
                </div>
              </div>
              
              <div className="space-y-2">
                <h4 className="text-xs font-bold text-slate-500 uppercase tracking-wider">Ajustement des quantités</h4>
                <div className="border border-slate-200 dark:border-slate-700 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 dark:bg-slate-800">
                      <tr>
                        <th className="px-3 py-2 text-xs text-slate-500">Article</th>
                        <th className="px-3 py-2 text-xs text-slate-500 text-center">Demandé</th>
                        <th className="px-3 py-2 text-xs text-slate-500 text-center">Expédié</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                      {shipConfirmTransfer.lines?.map((l: any) => (
                        <tr key={l.id}>
                          <td className="px-3 py-2 font-medium text-slate-700 dark:text-slate-300">
                            {l.stockItem?.itemCode} - {l.stockItem?.itemName}
                          </td>
                          <td className="px-3 py-2 text-center text-slate-500 font-bold">{l.quantityRequested}</td>
                          <td className="px-3 py-2">
                            <input 
                              type="number" min="0" max={l.quantityRequested}
                              value={shipQuantities[l.id] ?? ''}
                              onChange={e => {
                                const val = parseInt(e.target.value) || 0;
                                setShipQuantities(prev => ({...prev, [l.id]: Math.min(val, l.quantityRequested)}));
                              }}
                              className="w-full text-center py-1 rounded border border-amber-300 focus:ring-2 focus:ring-amber-500 dark:bg-slate-800 dark:border-slate-600 outline-none font-black text-amber-700"
                            />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <p className="text-slate-500 dark:text-slate-400 text-xs text-center">
                Un <strong>LTO</strong> sera généré automatiquement. Assurez-vous que les quantités reflètent exactement la marchandise sortante.
              </p>
            </div>
            {/* Actions */}
            <div className="p-6 pt-0 flex gap-3">
              <button
                onClick={() => setShipConfirmTransfer(null)}
                disabled={shipMutation.isPending}
                className="flex-1 px-4 py-3 rounded-2xl border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Annuler
              </button>
              <button
                onClick={() => shipMutation.mutate({ 
                  headerId: shipConfirmTransfer.id, 
                  reqBody: { lines: shipConfirmTransfer.lines.map((l:any) => ({ lineId: l.id, quantity: shipQuantities[l.id] ?? l.quantityRequested })) } 
                })}
                disabled={shipMutation.isPending}
                className="flex-1 px-4 py-3 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-500 text-white font-black hover:opacity-90 transition-all shadow-lg shadow-amber-200 disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {shipMutation.isPending ? (
                  <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                ) : '🚚'}
                {shipMutation.isPending ? 'Expédition...' : 'Confirmer'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Modal Confirmation Réception ──────────────────────────────────── */}
      {receiveConfirmTransfer && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/70 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-800 w-full max-w-md overflow-hidden animate-fade-in">
            {/* Header */}
            <div className="bg-gradient-to-r from-emerald-500 to-teal-500 p-6 flex items-center gap-4">
              <div className="w-12 h-12 bg-white/20 rounded-2xl flex items-center justify-center text-2xl">📥</div>
              <div>
                <h3 className="text-white font-black text-lg">Confirmer la réception</h3>
                <p className="text-emerald-100 text-sm">Vérification de la marchandise (LTI)</p>
              </div>
            </div>
            {/* Body */}
            <div className="p-6 space-y-4 max-h-[60vh] overflow-y-auto">
              <div className="bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800 rounded-2xl p-4 space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">N° Transfert</span>
                  <span className="font-bold text-emerald-600">TRF-{receiveConfirmTransfer.id}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">Source</span>
                  <span className="font-semibold text-slate-700 dark:text-slate-200">{receiveConfirmTransfer.warehouseSource?.name}</span>
                </div>
                {receiveConfirmTransfer.ltoNumber && (
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-500">N° LTO</span>
                    <span className="font-mono text-slate-700 dark:text-slate-300">{receiveConfirmTransfer.ltoNumber}</span>
                  </div>
                )}
              </div>
              
              <div className="space-y-2">
                <h4 className="text-xs font-bold text-slate-500 uppercase tracking-wider">Vérification des quantités</h4>
                <div className="border border-slate-200 dark:border-slate-700 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 dark:bg-slate-800">
                      <tr>
                        <th className="px-3 py-2 text-xs text-slate-500">Article</th>
                        <th className="px-3 py-2 text-xs text-slate-500 text-center">Expédié</th>
                        <th className="px-3 py-2 text-xs text-slate-500 text-center">Reçu</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                      {receiveConfirmTransfer.lines?.map((l: any) => {
                        const expected = l.quantityShipped || l.quantityRequested;
                        const current = receiveQuantities[l.id] ?? expected;
                        const isLoss = current < expected;
                        return (
                          <tr key={l.id}>
                            <td className="px-3 py-2 font-medium text-slate-700 dark:text-slate-300">
                              {l.stockItem?.itemCode} - {l.stockItem?.itemName}
                            </td>
                            <td className="px-3 py-2 text-center text-slate-500 font-bold">{expected}</td>
                            <td className="px-3 py-2">
                              <input 
                                type="number" min="0" max={expected}
                                value={current === 0 && receiveQuantities[l.id] === undefined ? '' : current}
                                onChange={e => {
                                  const val = parseInt(e.target.value) || 0;
                                  setReceiveQuantities(prev => ({...prev, [l.id]: Math.max(0, Math.min(val, expected))}));
                                }}
                                className={`w-full text-center py-1 rounded border focus:ring-2 outline-none font-black ${
                                  isLoss 
                                    ? 'border-rose-400 text-rose-600 focus:ring-rose-500' 
                                    : 'border-emerald-300 text-emerald-700 focus:ring-emerald-500'
                                } dark:bg-slate-800`}
                              />
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>

              {Object.entries(receiveQuantities).some(([id, qty]) => {
                  const line = receiveConfirmTransfer.lines?.find((l:any) => l.id.toString() === id);
                  return line && qty < (line.quantityShipped || line.quantityRequested);
              }) && (
                <div className="bg-rose-50 border border-rose-200 text-rose-700 p-3 rounded-xl text-xs flex gap-2">
                  <span>⚠️</span>
                  <p>Une quantité inférieure a été saisie. Cela sera enregistré comme <strong>perte en transit</strong>.</p>
                </div>
              )}
            </div>
            {/* Actions */}
            <div className="p-6 pt-0 flex gap-3">
              <button
                onClick={() => setReceiveConfirmTransfer(null)}
                disabled={receiveTransferMut.isPending}
                className="flex-1 px-4 py-3 rounded-2xl border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-all disabled:opacity-40"
              >
                Annuler
              </button>
              <button
                onClick={() => receiveTransferMut.mutate({ 
                  headerId: receiveConfirmTransfer.id, 
                  reqBody: { lines: receiveConfirmTransfer.lines.map((l:any) => ({ lineId: l.id, quantity: receiveQuantities[l.id] ?? (l.quantityShipped || l.quantityRequested) })) } 
                })}
                disabled={receiveTransferMut.isPending}
                className="flex-1 px-4 py-3 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 text-white font-black hover:opacity-90 transition-all shadow-lg shadow-emerald-200 disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {receiveTransferMut.isPending ? 'Chargement...' : '✅ Valider'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Modal LTO Généré ──────────────────────────────────────────────── */}
      {ltoResult && (
        <div className="fixed inset-0 z-[110] flex items-center justify-center bg-slate-900/70 backdrop-blur-sm">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-800 w-full max-w-md mx-4 overflow-hidden">
            {/* Success header */}
            <div className="bg-gradient-to-r from-emerald-500 to-teal-500 p-6 text-center">
              <div className="w-16 h-16 bg-white/20 rounded-full flex items-center justify-center text-3xl mx-auto mb-3">✅</div>
              <h3 className="text-white font-black text-xl">Transfert Expédié !</h3>
              <p className="text-emerald-100 text-sm mt-1">{ltoResult.trf} est maintenant EN TRANSIT</p>
            </div>
            {/* LTO Number */}
            <div className="p-6 space-y-4">
              <div className="text-center">
                <p className="text-slate-400 text-xs font-semibold uppercase tracking-widest mb-2">Numéro LTO Généré</p>
                <div className="inline-block bg-emerald-50 dark:bg-emerald-900/30 border-2 border-emerald-300 dark:border-emerald-700 rounded-2xl px-6 py-4">
                  <span className="font-mono font-black text-2xl text-emerald-700 dark:text-emerald-300 tracking-wider">
                    {ltoResult.lto}
                  </span>
                </div>
                <p className="text-slate-400 text-xs mt-3">
                  Communiquez ce numéro au magasinier de destination pour la réception.
                </p>
              </div>
              <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800 rounded-xl p-3 text-xs text-blue-600 dark:text-blue-400 text-center">
                📦 Le stock a été déduit de votre entrepôt et le magasinier de destination peut maintenant réceptionner la marchandise.
              </div>
            </div>
            {/* Close */}
            <div className="p-6 pt-0">
              <button
                onClick={() => setLtoResult(null)}
                className="w-full px-4 py-3 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 text-white font-black hover:opacity-90 transition-all shadow-lg shadow-emerald-200"
              >
                Fermer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Modal Demander un Transfert (Catalogue) ───────────────────────── */}
      {showBulkTransferModal && (
        <div className="fixed inset-0 z-[120] flex items-center justify-center bg-slate-900/70 backdrop-blur-md p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-800 w-full max-w-5xl flex flex-col max-h-[90vh] animate-scale-in overflow-hidden">
            <div className="px-8 py-6 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center bg-indigo-50/50 dark:bg-indigo-900/10">
              <div>
                <h2 className="text-xl font-black text-indigo-700 dark:text-indigo-400 flex items-center gap-2">
                  <span>🔄</span> Demander un transfert inter-sites
                </h2>
                <p className="text-xs text-slate-500 mt-1">
                  Les articles sélectionnés seront transférés vers votre entrepôt : <strong>{userWarehouseName}</strong>.
                </p>
              </div>
              <button onClick={() => setShowBulkTransferModal(false)} className="w-8 h-8 flex items-center justify-center rounded-full bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-500 transition-colors">✕</button>
            </div>
            
            <div className="p-8 flex-1 flex flex-col gap-6 min-h-0 overflow-hidden">
              {/* Barre de recherche */}
              <div className="relative shrink-0">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">🔍</span>
                <input 
                  type="text" 
                  value={stockSearch} 
                  onChange={e => setStockSearch(e.target.value)}
                  placeholder="Rechercher par code, nom ou entrepôt d'origine..."
                  className="w-full pl-12 pr-4 py-3 rounded-2xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500 outline-none font-medium text-sm"
                />
              </div>

              {/* Table Catalogue */}
              <div className="border border-slate-200 dark:border-slate-700 rounded-2xl shadow-sm flex-1 overflow-y-auto">
                <table className="w-full text-left text-sm relative">
                  <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 uppercase text-[10px] tracking-widest font-black sticky top-0 z-10">
                    <tr>
                      <th className="px-4 py-3">Article</th>
                      <th className="px-4 py-3">Source (Entrepôt)</th>
                      <th className="px-4 py-3 text-center">Disponible</th>
                      <th className="px-4 py-3 w-32 text-center">Qté Demandée</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {availableStock
                      .filter((s:any) => s.quantityAvailable > 0 && s.warehouse?.id !== userWarehouseId && !s.warehouse?.name?.toLowerCase().includes('central'))
                      .filter((s:any) => {
                        const term = stockSearch.toLowerCase();
                        return s.itemCode.toLowerCase().includes(term) || 
                               s.itemName.toLowerCase().includes(term) || 
                               s.warehouse?.name?.toLowerCase().includes(term);
                      })
                      .map((stock: any) => {
                        const cartItem = transferCart.find(c => c.stockItemId === stock.id);
                        return (
                          <tr key={stock.id} className={`transition-colors ${cartItem ? 'bg-indigo-50/50 dark:bg-indigo-900/10' : 'hover:bg-slate-50 dark:hover:bg-slate-800/50'}`}>
                            <td className="px-4 py-3 font-medium text-slate-800 dark:text-slate-200">
                              <div className="font-mono text-xs text-slate-400 mb-0.5">{stock.itemCode}</div>
                              {stock.itemName}
                            </td>
                            <td className="px-4 py-3">
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-slate-100 dark:bg-slate-800 text-xs font-bold text-slate-600 dark:text-slate-300">
                                🏭 {stock.warehouse?.name}
                              </span>
                            </td>
                            <td className="px-4 py-3 text-center">
                              <span className="font-black text-emerald-600">{stock.quantityAvailable}</span>
                            </td>
                            <td className="px-4 py-3">
                              <input 
                                type="number" min="0" max={stock.quantityAvailable}
                                value={cartItem?.qty || ''}
                                onChange={e => {
                                  const val = parseInt(e.target.value) || 0;
                                  if (val > 0) {
                                    setTransferCart(prev => {
                                      const filtered = prev.filter(p => p.stockItemId !== stock.id);
                                      return [...filtered, { 
                                        stockItemId: stock.id, 
                                        qty: Math.min(val, stock.quantityAvailable), 
                                        maxQty: stock.quantityAvailable, 
                                        itemCode: stock.itemCode, 
                                        itemName: stock.itemName, 
                                        sourceWarehouseId: stock.warehouse?.id,
                                        sourceWarehouseName: stock.warehouse?.name
                                      }];
                                    });
                                  } else {
                                    setTransferCart(prev => prev.filter(p => p.stockItemId !== stock.id));
                                  }
                                }}
                                className="w-full text-center py-2 rounded-xl border border-slate-300 dark:border-slate-600 focus:ring-2 focus:ring-indigo-500 outline-none font-bold bg-white dark:bg-slate-700"
                                placeholder="0"
                              />
                            </td>
                          </tr>
                        );
                      })}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Panier & Footer */}
            <div className="p-6 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50 flex flex-col md:flex-row justify-between items-center gap-4">
              <div className="text-sm font-semibold text-slate-600 dark:text-slate-300 flex items-center gap-2">
                <span className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center font-black">
                  {transferCart.length}
                </span>
                {transferCart.length > 1 ? 'articles sélectionnés' : 'article sélectionné'}
              </div>
              <div className="flex gap-3 w-full md:w-auto">
                <button 
                  type="button" 
                  onClick={() => setShowBulkTransferModal(false)} 
                  className="flex-1 md:flex-none px-6 py-3 rounded-2xl bg-white border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 font-bold hover:bg-slate-50"
                >
                  Annuler
                </button>
                <button 
                  type="button"
                  disabled={transferCart.length === 0 || submitBulkMutation.isPending} 
                  onClick={() => {
                    const payload = {
                      destWarehouseId: userWarehouseId,
                      items: transferCart.map(c => ({
                        stockItemId: c.stockItemId,
                        sourceWarehouseId: c.sourceWarehouseId,
                        quantityRequested: c.qty
                      }))
                    };
                    submitBulkMutation.mutate(payload);
                  }}
                  className="flex-1 md:flex-none px-8 py-3 rounded-2xl bg-indigo-600 text-white font-black hover:bg-indigo-700 disabled:opacity-50 shadow-lg shadow-indigo-200 flex items-center justify-center gap-2"
                >
                  {submitBulkMutation.isPending ? 'Envoi...' : 'Soumettre la demande 🚀'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
