import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import { useAuth } from '../context/AuthContext';
import { getSourceTransfers, getSourceHistory, shipTransfer } from '../api/services';

export default function MagasinierTransferOut() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const userId = user?.userId;

  const [activeTab, setActiveTab] = useState<'pending' | 'history'>('pending');
  
  // Modals state
  const [shipConfirmTransfer, setShipConfirmTransfer] = useState<any | null>(null);
  const [shipQuantities, setShipQuantities] = useState<Record<number, number>>({});

  const { data: pendingTransfers = [], isLoading: loadingPending } = useQuery({
    queryKey: ['transfers', 'source', userId],
    queryFn: () => getSourceTransfers(userId!).then(r => r.data),
    enabled: !!userId,
  });

  const { data: historyTransfers = [], isLoading: loadingHistory } = useQuery({
    queryKey: ['transfers', 'history', 'source', userId],
    queryFn: () => getSourceHistory(userId!).then(r => r.data),
    enabled: !!userId && activeTab === 'history',
  });

  const shipMutation = useMutation({
    mutationFn: (payload: {headerId: number, reqBody: any}) => shipTransfer(payload.headerId, userId!, payload.reqBody).then(r => r.data),
    onSuccess: (header) => {
      toast.success(`Transfert expédié avec succès ! LTO généré : ${header.ltoNumber}`);
      setShipConfirmTransfer(null);
      setShipQuantities({});
      qc.invalidateQueries({ queryKey: ['transfers', 'source'] });
      qc.invalidateQueries({ queryKey: ['transfers', 'history', 'source'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de l\'expédition');
    },
  });

  return (
    <DashboardLayout title="Espace Magasin — Expéditions OUT">
      <div className="max-w-7xl mx-auto space-y-6">
        <header className="bg-gradient-to-r from-amber-500 to-orange-600 p-8 rounded-3xl text-white shadow-lg relative overflow-hidden">
          <div className="absolute top-0 right-0 opacity-10">
            <svg className="w-48 h-48" viewBox="0 0 24 24" fill="currentColor"><path d="M20 8h-3V4H3c-1.1 0-2 .9-2 2v11h2c0 1.66 1.34 3 3 3s3-1.34 3-3h6c0 1.66 1.34 3 3 3s3-1.34 3-3h2v-5l-3-4zM6 18.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm13.5-9l1.96 2.5H17V9.5h2.5zm-1.5 9c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"/></svg>
          </div>
          <div className="relative z-10">
            <h1 className="text-3xl font-black mb-2 tracking-tight">Transfer OUT (Mes Demandes OUT)</h1>
            <p className="text-amber-100 font-medium">Gérez l'expédition des transferts inter-sites demandés depuis votre entrepôt et consultez l'historique.</p>
          </div>
        </header>

        <div className="flex gap-4 border-b border-slate-200 dark:border-slate-700 pb-2">
          <button
            onClick={() => setActiveTab('pending')}
            className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'pending'
                ? 'bg-amber-500 text-white shadow-lg shadow-amber-100 dark:shadow-none'
                : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700'
            }`}
          >
            📤 File d'attente ({pendingTransfers.length})
          </button>
          <button
            onClick={() => setActiveTab('history')}
            className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'history'
                ? 'bg-slate-700 text-white shadow-lg shadow-slate-200 dark:shadow-none'
                : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700'
            }`}
          >
            🕒 Historique (Traçabilité)
          </button>
        </div>

        {activeTab === 'pending' && (
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden animate-fade-in">
            <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-amber-50 to-transparent dark:from-amber-900/10">
              <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                Transferts à Expédier (PENDING)
              </h2>
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
                      <span className="bg-slate-100 text-slate-600 px-2 py-0.5 rounded text-[9px] font-black dark:bg-slate-700 dark:text-slate-300">{t.lines?.length ?? 0} article(s)</span>
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
                        className="px-5 py-2 rounded-xl bg-amber-500 text-white text-xs font-bold hover:bg-amber-600 transition-all shadow-lg shadow-amber-100 dark:shadow-none"
                      >
                        🚚 Expédier
                      </button>
                    </td>
                  </tr>
                ))}
                {pendingTransfers.length === 0 && !loadingPending && (
                  <tr><td colSpan={6} className="px-6 py-20 text-center text-slate-400 italic">Aucun transfert en attente d'expédition.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'history' && (
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden animate-fade-in">
            <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
              <h2 className="text-lg font-black text-slate-800 dark:text-white">Historique de Traçabilité (Expéditions)</h2>
              <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'history', 'source'] })} className="text-xs font-bold text-slate-600 hover:underline">🔄 Actualiser</button>
            </div>
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
                <tr>
                  <th className="px-6 py-4">N° TRF / LTO</th>
                  <th className="px-6 py-4">Destination</th>
                  <th className="px-6 py-4">Statut</th>
                  <th className="px-6 py-4">Expédié le</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                {historyTransfers.map((t: any) => (
                  <tr key={t.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-mono font-bold text-slate-700 dark:text-slate-200">TRF-{t.id}</div>
                      {t.ltoNumber && <div className="text-xs font-mono text-amber-600 mt-1">{t.ltoNumber}</div>}
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-600 dark:text-slate-300">{t.warehouseDest?.name}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                        t.status === 'RECEIVED' 
                          ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' 
                          : 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
                      }`}>
                        {t.status === 'RECEIVED' ? 'RÉCEPTIONNÉ' : 'EN TRANSIT'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-slate-500">{t.shippedAt ? new Date(t.shippedAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
                {historyTransfers.length === 0 && !loadingHistory && (
                  <tr><td colSpan={4} className="px-6 py-20 text-center text-slate-400 italic">Aucun historique d'expédition.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Confirmation Expédition */}
      {shipConfirmTransfer && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/70 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-800 w-full max-w-lg overflow-hidden animate-fade-in-up">
            <div className="bg-gradient-to-r from-amber-500 to-orange-500 p-6 flex items-center gap-4">
              <div className="w-12 h-12 bg-white/20 rounded-2xl flex items-center justify-center text-2xl">🚚</div>
              <div>
                <h3 className="text-white font-black text-lg">Confirmer l'expédition</h3>
                <p className="text-amber-100 text-sm">Génération automatique du LTO</p>
              </div>
            </div>
            <div className="p-6 space-y-6 max-h-[60vh] overflow-y-auto">
              <div className="space-y-2">
                <h4 className="text-xs font-bold text-slate-500 uppercase tracking-wider">Ajustement des quantités</h4>
                <div className="border border-slate-200 dark:border-slate-700 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 dark:bg-slate-800">
                      <tr>
                        <th className="px-4 py-3 text-xs text-slate-500">Article</th>
                        <th className="px-4 py-3 text-xs text-slate-500 text-center">Demandé</th>
                        <th className="px-4 py-3 text-xs text-slate-500 text-center">Expédié</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                      {shipConfirmTransfer.lines?.map((l: any) => (
                        <tr key={l.id}>
                          <td className="px-4 py-3 font-medium text-slate-700 dark:text-slate-300">
                            {l.stockItem?.itemCode} - {l.stockItem?.itemName}
                          </td>
                          <td className="px-4 py-3 text-center text-slate-500 font-bold">{l.quantityRequested}</td>
                          <td className="px-4 py-3">
                            <input 
                              type="number" min="0" max={l.quantityRequested}
                              value={shipQuantities[l.id] ?? ''}
                              onChange={e => {
                                const val = parseInt(e.target.value) || 0;
                                setShipQuantities(prev => ({...prev, [l.id]: Math.min(val, l.quantityRequested)}));
                              }}
                              className="w-full text-center py-1.5 rounded-lg border border-amber-300 focus:ring-2 focus:ring-amber-500 dark:bg-slate-800 dark:border-slate-600 outline-none font-black text-amber-700 dark:text-amber-400"
                            />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
            <div className="p-6 pt-0 flex gap-3">
              <button
                onClick={() => setShipConfirmTransfer(null)}
                disabled={shipMutation.isPending}
                className="flex-1 px-4 py-3 rounded-2xl border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-all disabled:opacity-40"
              >
                Annuler
              </button>
              <button
                onClick={() => shipMutation.mutate({ 
                  headerId: shipConfirmTransfer.id, 
                  reqBody: { lines: shipConfirmTransfer.lines.map((l:any) => ({ lineId: l.id, quantity: shipQuantities[l.id] ?? l.quantityRequested })) } 
                })}
                disabled={shipMutation.isPending}
                className="flex-1 px-4 py-3 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-500 text-white font-black hover:opacity-90 transition-all shadow-lg shadow-amber-200 dark:shadow-none disabled:opacity-50"
              >
                {shipMutation.isPending ? 'Expédition...' : 'Confirmer & Créer LTO'}
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
