import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { getSourceTransfers, shipTransfer, getSourceHistory, downloadLtoPdf } from '../api/services';
import type { PurchaseOrder, GrnHeader, GrnDetails, POStatus } from '../types';
import { formatCurrency } from '../utils/constants';

export default function MagasinierPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedPo, setSelectedPo] = useState<PurchaseOrder | null>(null);
  const [deliveryNote, setDeliveryNote] = useState('');
  const [isEntryCompleted, setIsEntryCompleted] = useState(true);
  const [grnItems, setGrnItems] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState<'grn' | 'transfers' | 'history' | 'destTransfers'>('grn');

  // ── Modal confirmation expédition ───────────────────────────────────────────
  const [shipConfirmTransfer, setShipConfirmTransfer] = useState<any | null>(null);
  // ── Modal LTO généré ────────────────────────────────────────────────────────
  const [ltoResult, setLtoResult] = useState<{ lto: string; trf: string } | null>(null);

  const userId = user?.userId;

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

  // 3. Historique des transferts expédiés
  const { data: historyTransfers = [] } = useQuery<any[]>({
    queryKey: ['transfers', 'history', 'source', userId],
    queryFn: () => getSourceHistory(userId).then(r => r.data),
    refetchInterval: 30_000,
    enabled: !!userId && activeTab === 'history',
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

  const shipMutation = useMutation({
    mutationFn: (headerId: number) => shipTransfer(headerId, userId),
    onSuccess: (res) => {
      const header = res.data;
      const lto = header?.ltoNumber ?? 'LTO généré';
      const trf = `TRF-${header?.id ?? ''}` ;
      setLtoResult({ lto, trf });
      setShipConfirmTransfer(null);
      qc.invalidateQueries({ queryKey: ['transfers', 'source'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de l\'expédition');
      setShipConfirmTransfer(null);
    },
  });

  const receiveTransferMut = useMutation({
    mutationFn: (headerId: number) => api.put(`/transfers/${headerId}/receive?userId=${userId}`).then(r => r.data),
    onSuccess: (res) => {
      const header = res;
      toast.success(`Transfert TRF-${header.id} réceptionné avec succès !`);
      qc.invalidateQueries({ queryKey: ['transfers'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la réception');
    },
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
    const isClassic = !!po.daHeader;
    if (isClassic) {
      setGrnItems(po.daHeader!.details!.map(d => ({
        itemCode: d.itemCode,
        itemName: d.itemName || d.description,
        orderedQuantity: d.quantite,
        receivedQuantity: 0,
        shippedQuantity: 0, // Sera mis à jour via poBalance
      })));
    } else {
      setGrnItems([{
        itemCode: '', // Libre pour flux Interne
        itemName: po.demandeInterne!.designation,
        orderedQuantity: po.demandeInterne!.quantite,
        receivedQuantity: 0,
        shippedQuantity: 0,
      }]);
    }
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
        <div onClick={() => setActiveTab('grn')} className="cursor-pointer transition-transform hover:scale-105">
            <KpiCard label="Commandes en attente" value={approvedPOs.length} icon="🚚" color="from-blue-600 to-indigo-700" />
        </div>
        <div onClick={() => setActiveTab('transfers')} className="cursor-pointer transition-transform hover:scale-105">
            <KpiCard label="Transferts à expédier" value={pendingTransfers.length} icon="📦" color="from-amber-500 to-orange-600" />
        </div>
        <div onClick={() => setActiveTab('destTransfers')} className="cursor-pointer transition-transform hover:scale-105">
            <KpiCard label="Transferts en réception" value={destTransfers.length} icon="📥" color="from-emerald-500 to-teal-600" />
        </div>
        <div className="cursor-not-allowed opacity-80">
            <KpiCard label="Alertes Stock" value={0} icon="⚠️" color="from-rose-500 to-red-600" />
        </div>
      </div>

      {/* Tab switcher */}
      <div className="flex gap-2 mb-6">
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
        <button
          onClick={() => setActiveTab('transfers')}
          className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
            activeTab === 'transfers'
              ? 'bg-amber-500 text-white shadow-lg shadow-amber-100'
              : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
          }`}
        >
          🚚 Transferts à Expédier ({pendingTransfers.length})
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
            activeTab === 'history'
              ? 'bg-slate-700 text-white shadow-lg shadow-slate-200'
              : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
          }`}
        >
          🕒 Historique & LTO
        </button>
        <button
          onClick={() => setActiveTab('destTransfers')}
          className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
            activeTab === 'destTransfers'
              ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-100'
              : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
          }`}
        >
          📥 Transferts en Réception ({destTransfers.length})
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
                  <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase ${po.daHeader ? 'bg-slate-100 text-slate-600' : 'bg-violet-100 text-violet-600'}`}>
                    {po.daHeader ? 'Classique' : 'Interne'}
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

      {/* ── Onglet Transferts ── */}
      {activeTab === 'transfers' && (
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
          <h2 className="text-lg font-black text-slate-800 dark:text-white">Transferts à Expédier</h2>
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
                    onClick={() => setShipConfirmTransfer(t)}
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

      {/* ── Onglet Historique LTO ── */}
      {activeTab === 'history' && (
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
          <h2 className="text-lg font-black text-slate-800 dark:text-white">Historique des Expéditions (LTO)</h2>
          <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'history'] })} className="text-xs font-bold text-slate-600 hover:underline">🔄 Actualiser</button>
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
                <td className="px-6 py-4 text-slate-500">{new Date(t.shippedAt).toLocaleString()}</td>
                <td className="px-6 py-4 text-right">
                  <button
                    onClick={() => handleDownloadLto(t.id, t.ltoNumber)}
                    className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 text-xs font-bold hover:bg-slate-200 dark:hover:bg-slate-600 transition-all"
                  >
                    📄 Télécharger LTO
                  </button>
                </td>
              </tr>
            ))}
            {historyTransfers.length === 0 && (
              <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Aucun historique d'expédition.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      )}

      {/* ── Onglet destTransfers (Réception IN_TRANSIT) ── */}
      {activeTab === 'destTransfers' && (
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden animate-fade-in">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-emerald-50 to-transparent dark:from-emerald-900/10">
            <div>
                <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                    <span className="w-8 h-8 rounded-xl bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 flex items-center justify-center">📥</span>
                    Transferts en Réception
                </h2>
                <p className="text-xs text-slate-500 mt-1">Acceptez les transferts en provenance d'autres entrepôts (LTO)</p>
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
                        if (window.confirm(`Confirmer la réception de l'ensemble des articles du TRF-${t.id} ? Le stock sera mis à jour.`)) {
                            receiveTransferMut.mutate(t.id);
                        }
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
            <div className="p-6 space-y-4">
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
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">Articles</span>
                  <span className="font-semibold text-slate-700 dark:text-slate-200">{shipConfirmTransfer.lines?.length ?? 0} article(s)</span>
                </div>
              </div>
              <p className="text-slate-500 dark:text-slate-400 text-xs text-center">
                Un <strong>LTO</strong> (Lettre de Transport Officielle) sera généré automatiquement et le stock sera déduit de votre entrepôt.
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
                onClick={() => shipMutation.mutate(shipConfirmTransfer.id)}
                disabled={shipMutation.isPending}
                className="flex-1 px-4 py-3 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-500 text-white font-black hover:opacity-90 transition-all shadow-lg shadow-amber-200 disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {shipMutation.isPending ? (
                  <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                ) : '🚚'}
                {shipMutation.isPending ? 'Expédition...' : 'Confirmer l\'expédition'}
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
    </>
  );
}
