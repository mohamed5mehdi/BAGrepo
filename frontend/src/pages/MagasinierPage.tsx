import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import { useAuth } from '../context/AuthContext';
import { 
  getPurchaseOrdersByStatus, createGrn, validateGrn, getPoBalance 
} from '../api/services';
import type { PurchaseOrder, GrnHeader, GrnDetails, POStatus } from '../types';
import { formatCurrency } from '../utils/constants';

export default function MagasinierPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedPo, setSelectedPo] = useState<PurchaseOrder | null>(null);
  const [deliveryNote, setDeliveryNote] = useState('');
  const [isEntryCompleted, setIsEntryCompleted] = useState(true);
  const [grnItems, setGrnItems] = useState<any[]>([]);

  // 1. Liste PO filtrée : APPROVED uniquement
  const { data: approvedPOs = [], isLoading } = useQuery({
    queryKey: ['pos', 'approved'],
    queryFn: () => getPurchaseOrdersByStatus('APPROVED' as POStatus).then(r => r.data),
    refetchInterval: 15_000,
    enabled: !!user,
  });

  // Fetch balance for selected PO
  const { data: poBalance = {}, isFetching: loadingBalance } = useQuery({
    queryKey: ['po-balance', selectedPo?.id_po],
    queryFn: () => getPoBalance(selectedPo!.id_po).then(r => r.data),
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
      const res = await createGrn(payload);
      if (isEntryCompleted) {
        await validateGrn(res.data.id);
      }
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
    <DashboardLayout title="Espace Magasin — Réceptions BAG ERP" pendingCount={approvedPOs.length}>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <KpiCard label="Commandes en attente" value={approvedPOs.length} icon="🚚" color="from-blue-600 to-indigo-700" />
        <KpiCard label="Reçu aujourd'hui" value={0} icon="📦" color="from-emerald-500 to-teal-600" />
        <KpiCard label="Alertes Stock" value={0} icon="⚠️" color="from-amber-500 to-orange-600" />
      </div>

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
            {approvedPOs.map(po => (
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
  );
}
