import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import { useAuth } from '../context/AuthContext';
import { 
  getGrnByStatus, createGrc, validateGrc 
} from '../api/services';
import type { GrnHeader, GrcHeader, GrcDetails } from '../types';
import { formatCurrency } from '../utils/constants';

export default function ComptablePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [selectedGrn, setSelectedGrn] = useState<GrnHeader | null>(null);
  const [vendorInvoiceAmount, setVendorInvoiceAmount] = useState<number>(0);
  const [details, setDetails] = useState<any[]>([]);

  // 1. Liste GRN filtrée : ENTRY_COMPLETED uniquement
  const { data: pendingGrns = [], isLoading } = useQuery({
    queryKey: ['grns', 'entry-completed'],
    queryFn: () => getGrnByStatus('ENTRY_COMPLETED').then(r => r.data),
    refetchInterval: 20_000,
    enabled: !!user,
  });

  const openGrcForm = (grn: GrnHeader) => {
    setSelectedGrn(grn);
    const grcTotal = grn.details?.reduce((acc, d) => acc + (d.receivedQuantity * 0), 0) || 0; // Unit cost initially 0
    setVendorInvoiceAmount(grcTotal);
    
    setDetails(grn.details?.map(d => ({
      grnDetailId: d.id,
      itemCode: d.itemCode,
      itemName: d.itemName,
      acceptedQuantity: d.receivedQuantity,
      unitCost: 0,
      taxRate: 20,
      mainAccount: '',
      subAccount: '',
      // Détection catégorie via itemCode ou meta-data PO (Simplification : on regarde si c'est ADMIN dans le code)
      category: (grn.purchaseOrder?.daHeader?.categorie || grn.purchaseOrder?.demandeInterne?.categorie || 'AUTRE')
    })) || []);
  };

  const createGrcMutation = useMutation({
    mutationFn: async (payload: Partial<GrcHeader>) => {
      const res = await createGrc(payload);
      await validateGrc(res.data.id);
      return res.data;
    },
    onSuccess: () => {
      toast.success('💰 Valorisation validée et impact financier enregistré !');
      qc.invalidateQueries({ queryKey: ['grns'] });
      setSelectedGrn(null);
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la valorisation');
    }
  });

  const calculateTotalGrc = () => {
    return details.reduce((acc, d) => {
      const lineHT = d.unitCost * d.acceptedQuantity;
      return acc + (lineHT * (1 + d.taxRate / 100));
    }, 0);
  };

  const totalGrc = calculateTotalGrc();
  const diff = Math.abs(totalGrc - vendorInvoiceAmount);
  const hasAlert = diff > 0.01;

  const isFormValid = () => {
    if (details.some(d => d.unitCost <= 0)) return false;
    if (details.some(d => d.category === 'ADMINISTRATIF' && (!d.mainAccount || !d.subAccount))) return false;
    return true;
  };

  return (
    <DashboardLayout title="Valorisation Comptable — GRC & Facturation" pendingCount={pendingGrns.length}>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <KpiCard label="Réceptions à valoriser" value={pendingGrns.length} icon="💰" color="from-emerald-600 to-teal-700" />
        <KpiCard label="Total valorisé (Mois)" value={0} icon="📊" color="from-blue-500 to-indigo-600" />
        <KpiCard label="Écarts Factures" value={0} icon="⚖️" color="from-rose-500 to-red-600" />
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
            <h2 className="text-lg font-black text-slate-800 dark:text-white">Réceptions Magasin Validées (ENTRY_COMPLETED)</h2>
            <button onClick={() => navigate('/ai-dashboard')}
              className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-sm font-bold hover:bg-indigo-700 transition-colors flex items-center gap-2">
              📊 Surveillance IA
            </button>
        </div>
        
        <table className="w-full text-sm text-left">
          <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
            <tr>
              <th className="px-6 py-4">N° GRN</th>
              <th className="px-6 py-4">N° PO</th>
              <th className="px-6 py-4">Fournisseur</th>
              <th className="px-6 py-4">Date Réception</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
            {pendingGrns.map(grn => (
              <tr key={grn.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                <td className="px-6 py-4 font-mono font-bold text-emerald-600">{grn.grnNumber}</td>
                <td className="px-6 py-4 font-mono text-slate-500">{grn.purchaseOrder?.poNumber}</td>
                <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">{grn.purchaseOrder?.fournisseur?.nom || '—'}</td>
                <td className="px-6 py-4 text-slate-500">{new Date(grn.receiptDate).toLocaleDateString()}</td>
                <td className="px-6 py-4 text-right">
                  <button onClick={() => openGrcForm(grn)} className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-100">
                    💰 Valoriser (GRC)
                  </button>
                </td>
              </tr>
            ))}
            {pendingGrns.length === 0 && !isLoading && (
              <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Aucune réception en attente de valorisation.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {selectedGrn && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="bg-white dark:bg-slate-900 w-full max-w-5xl rounded-[32px] shadow-2xl overflow-hidden border border-white/20">
                <div className="p-8 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
                    <div>
                        <h2 className="text-2xl font-black text-slate-900 dark:text-white">Valorisation GRC</h2>
                        <p className="text-slate-400 text-sm mt-1">Référence : <span className="font-mono font-bold text-emerald-600">{selectedGrn.grnNumber}</span></p>
                    </div>
                    <button onClick={() => setSelectedGrn(null)} className="p-2 hover:bg-slate-100 rounded-full transition-colors text-slate-400">✕</button>
                </div>

                <div className="p-8 space-y-8 max-h-[70vh] overflow-y-auto">
                    {/* Invoice Check */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="md:col-span-1 space-y-2">
                            <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Montant Facture Fournisseur (TTC)</label>
                            <input 
                                type="number"
                                value={vendorInvoiceAmount} onChange={e => setVendorInvoiceAmount(parseFloat(e.target.value) || 0)}
                                className={`w-full px-5 py-3 rounded-2xl border ${hasAlert ? 'border-rose-300 bg-rose-50 text-rose-600' : 'border-slate-200 bg-slate-50'} text-sm font-black focus:ring-2 outline-none transition-all`}
                            />
                            {hasAlert && <p className="text-[10px] text-rose-500 font-bold ml-1">⚠️ Écart de {formatCurrency(diff)} avec le calcul GRC</p>}
                        </div>
                        <div className="md:col-span-2 bg-slate-50 dark:bg-slate-800 p-4 rounded-2xl border border-slate-100 dark:border-slate-700 flex items-center justify-around">
                            <div className="text-center">
                                <p className="text-[10px] font-black text-slate-400 uppercase">Total GRC Calculé</p>
                                <p className="text-xl font-black text-emerald-600">{formatCurrency(totalGrc)}</p>
                            </div>
                            <div className="h-8 w-px bg-slate-200 dark:bg-slate-700"></div>
                            <div className="text-center">
                                <p className="text-[10px] font-black text-slate-400 uppercase">Devise</p>
                                <p className="text-xl font-black text-slate-800 dark:text-white">MAD</p>
                            </div>
                        </div>
                    </div>

                    {/* Costing Table */}
                    <div className="space-y-4">
                        <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                            <span className="w-5 h-5 rounded-full bg-emerald-600 flex items-center justify-center text-[10px] text-white">💰</span>
                            Détails de Valorisation par Ligne
                        </h3>
                        
                        <div className="border border-slate-100 dark:border-slate-800 rounded-2xl overflow-hidden">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold text-[10px] uppercase">
                                    <tr>
                                        <th className="px-6 py-4">Article</th>
                                        <th className="px-6 py-4 text-center">Qté Reçue</th>
                                        <th className="px-6 py-4">Coût Unit. (HT)</th>
                                        <th className="px-6 py-4">TVA (%)</th>
                                        <th className="px-6 py-4">Comptabilité (Admin Only)</th>
                                        <th className="px-6 py-4 text-right">Total TTC</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                                    {details.map((item, idx) => (
                                        <tr key={idx} className={item.category === 'ADMINISTRATIF' ? 'bg-amber-50/30' : ''}>
                                            <td className="px-6 py-4">
                                                <p className="font-bold text-slate-800 dark:text-slate-200">{item.itemName}</p>
                                                <p className="text-[10px] font-mono text-slate-400">{item.itemCode}</p>
                                                <span className="text-[8px] font-black uppercase px-1 rounded bg-slate-100 text-slate-500">{item.category}</span>
                                            </td>
                                            <td className="px-6 py-4 text-center font-black text-slate-600">{item.acceptedQuantity}</td>
                                            <td className="px-6 py-4">
                                                <input 
                                                    type="number"
                                                    value={item.unitCost}
                                                    onChange={e => {
                                                        const n = [...details];
                                                        n[idx].unitCost = parseFloat(e.target.value) || 0;
                                                        setDetails(n);
                                                    }}
                                                    className="w-24 px-3 py-1.5 rounded-xl border border-emerald-200 font-bold text-emerald-600 focus:ring-2 focus:ring-emerald-500 outline-none"
                                                />
                                            </td>
                                            <td className="px-6 py-4">
                                                <select 
                                                    value={item.taxRate}
                                                    onChange={e => {
                                                        const n = [...details];
                                                        n[idx].taxRate = parseInt(e.target.value);
                                                        setDetails(n);
                                                    }}
                                                    className="px-2 py-1.5 rounded-lg border border-slate-200 text-xs font-bold outline-none"
                                                >
                                                    <option value={20}>20%</option>
                                                    <option value={14}>14%</option>
                                                    <option value={10}>10%</option>
                                                    <option value={7}>7%</option>
                                                    <option value={0}>0%</option>
                                                </select>
                                            </td>
                                            <td className="px-6 py-4">
                                                {item.category === 'ADMINISTRATIF' ? (
                                                    <div className="flex flex-col gap-1">
                                                        <input 
                                                            placeholder="Main Acc." value={item.mainAccount}
                                                            onChange={e => { const n = [...details]; n[idx].mainAccount = e.target.value; setDetails(n); }}
                                                            className="px-2 py-1 rounded border border-amber-200 text-[10px] font-bold"
                                                        />
                                                        <input 
                                                            placeholder="Sub Acc." value={item.subAccount}
                                                            onChange={e => { const n = [...details]; n[idx].subAccount = e.target.value; setDetails(n); }}
                                                            className="px-2 py-1 rounded border border-amber-200 text-[10px] font-bold"
                                                        />
                                                    </div>
                                                ) : (
                                                    <span className="text-[10px] text-slate-400 italic">Auto-Accounted</span>
                                                )}
                                            </td>
                                            <td className="px-6 py-4 text-right font-black text-slate-900 dark:text-white">
                                                {formatCurrency((item.unitCost * item.acceptedQuantity) * (1 + item.taxRate / 100))}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div className="p-8 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-50 dark:border-slate-800 flex justify-end gap-4">
                    <button onClick={() => setSelectedGrn(null)} className="px-8 py-3 rounded-2xl bg-white dark:bg-slate-700 text-slate-600 dark:text-slate-200 font-bold text-sm shadow-sm">Annuler</button>
                    <button 
                        onClick={() => createGrcMutation.mutate({
                            grnHeader: { id: selectedGrn.id } as any,
                            costingDate: new Date().toISOString().split('T')[0],
                            status: 'PENDING_APPROVAL',
                            devise: 'MAD',
                            details: details.map(d => ({
                                grnDetail: { id: d.grnDetailId } as any,
                                itemCode: d.itemCode,
                                acceptedQuantity: d.acceptedQuantity,
                                unitCost: d.unitCost,
                                taxRate: d.taxRate,
                                mainAccount: d.mainAccount,
                                subAccount: d.subAccount
                            })) as any
                        })}
                        disabled={!isFormValid() || createGrcMutation.isPending}
                        className="px-12 py-3 rounded-2xl bg-emerald-600 text-white font-black text-sm shadow-xl shadow-emerald-100 hover:bg-emerald-700 transition-all disabled:opacity-50"
                    >
                        {createGrcMutation.isPending ? 'Chargement...' : '⚖️ Approuver & Poster GRC'}
                    </button>
                </div>
            </div>
        </div>
      )}
    </DashboardLayout>
  );
}
