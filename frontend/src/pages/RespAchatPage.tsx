import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import { useAuth } from '../context/AuthContext';
import { getPurchaseOrders, approvePO, rejectPO } from '../api/services';
import { formatCurrency } from '../utils/constants';

export default function RespAchatPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedPo, setSelectedPo] = useState<any | null>(null);
  const [commentaire, setCommentaire] = useState('');

  const { data: pos = [], isLoading } = useQuery({
    queryKey: ['purchase-orders', 'all'],
    queryFn: () => getPurchaseOrders().then(r => r.data),
  });

  const pendingPOs = pos.filter((po: any) => po.statut === 'DRAFT' || po.statut === 'PENDING_APPROVAL');

  const approveMutation = useMutation({
    mutationFn: (id: number) => approvePO(id, user!.userId, commentaire),
    onSuccess: () => {
      toast.success('✅ Bon de Commande approuvé avec succès !');
      qc.invalidateQueries({ queryKey: ['purchase-orders'] });
      setSelectedPo(null);
      setCommentaire('');
    }
  });

  const rejectMutation = useMutation({
    mutationFn: (id: number) => rejectPO(id, user!.userId, commentaire),
    onSuccess: () => {
      toast.error('❌ Bon de Commande rejeté.');
      qc.invalidateQueries({ queryKey: ['purchase-orders'] });
      setSelectedPo(null);
      setCommentaire('');
    }
  });

  return (
    <DashboardLayout title="Approbation Achats — BAG Responsable Achat" pendingCount={pendingPOs.length}>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <KpiCard label="En attente" value={pendingPOs.length} icon="⏳" color="from-amber-500 to-orange-600" />
        <KpiCard label="Total Approuvés" value={pos.filter((p: any) => p.statut === 'APPROVED').length} icon="✅" color="from-emerald-500 to-teal-600" />
        <KpiCard label="Total Rejetés" value={pos.filter((p: any) => p.statut === 'REJECTED').length} icon="❌" color="from-rose-500 to-red-600" />
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-sm overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-100 dark:border-slate-800">
              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase">Référence</th>
              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase">Fournisseur</th>
              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase text-right">Montant TTC</th>
              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase">Statut</th>
              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase text-center">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
            {isLoading ? (
              <tr><td colSpan={5} className="px-6 py-10 text-center text-slate-400">Chargement...</td></tr>
            ) : pendingPOs.length === 0 ? (
              <tr><td colSpan={5} className="px-6 py-10 text-center text-slate-400">Aucun PO en attente d'approbation.</td></tr>
            ) : pendingPOs.map((po: any) => (
              <tr key={po.idPo} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-all cursor-pointer" onClick={() => setSelectedPo(po)}>
                <td className="px-6 py-4">
                  <span className="font-mono text-xs font-bold text-blue-600">{po.poNumber}</span>
                </td>
                <td className="px-6 py-4">
                  <p className="text-sm font-bold text-slate-700 dark:text-slate-200">{po.fournisseur?.nom || '—'}</p>
                </td>
                <td className="px-6 py-4 text-right">
                  <span className="text-sm font-black text-slate-800 dark:text-white">{formatCurrency(po.montantTotal)}</span>
                </td>
                <td className="px-6 py-4">
                  <span className="px-3 py-1 rounded-full text-[10px] font-black bg-amber-100 text-amber-700 uppercase tracking-tighter">
                    {po.statut}
                  </span>
                </td>
                <td className="px-6 py-4 text-center">
                  <button className="text-blue-600 font-bold text-xs hover:underline">Examiner</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selectedPo && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[70] flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-2xl animate-scale-in">
            <div className="px-8 py-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <h2 className="text-xl font-bold">Approbation du Bon de Commande</h2>
              <button onClick={() => setSelectedPo(null)} className="p-2 text-slate-400">✕</button>
            </div>
            
            <div className="p-8 space-y-6">
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 bg-slate-50 dark:bg-slate-800 rounded-2xl">
                  <p className="text-[10px] font-bold text-slate-400 uppercase">Référence</p>
                  <p className="text-lg font-black text-blue-600">{selectedPo.poNumber}</p>
                </div>
                <div className="p-4 bg-slate-50 dark:bg-slate-800 rounded-2xl text-right">
                  <p className="text-[10px] font-bold text-slate-400 uppercase">Total TTC</p>
                  <p className="text-lg font-black text-slate-800 dark:text-white">{formatCurrency(selectedPo.montantTotal)}</p>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-[10px] font-bold text-slate-400 uppercase ml-1">Commentaire / Justification</label>
                <textarea 
                  value={commentaire} onChange={e => setCommentaire(e.target.value)}
                  placeholder="Saisissez un commentaire pour l'acheteur..."
                  className="w-full px-5 py-4 rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm focus:ring-2 focus:ring-blue-500 outline-none resize-none"
                  rows={3}
                />
              </div>

              <div className="flex gap-4 pt-4">
                <button 
                  onClick={() => rejectMutation.mutate(selectedPo.idPo)}
                  disabled={!commentaire || rejectMutation.isPending}
                  className="flex-1 py-3.5 rounded-2xl bg-rose-50 text-rose-600 font-bold text-sm hover:bg-rose-100 transition-all border border-rose-100"
                >
                  {rejectMutation.isPending ? '...' : '❌ Rejeter'}
                </button>
                <button 
                  onClick={() => approveMutation.mutate(selectedPo.idPo)}
                  disabled={approveMutation.isPending}
                  className="flex-[2] py-3.5 rounded-2xl bg-blue-600 text-white font-black text-sm hover:bg-blue-700 shadow-xl shadow-blue-100 transition-all"
                >
                  {approveMutation.isPending ? 'Approbation...' : '✅ Approuver le PO'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
