import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import { useAuth } from '../context/AuthContext';
import { getDestTransfers, getDestHistory, receiveTransfer } from '../api/services';

export default function MagasinierTransferIn() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const userId = user?.userId;

  const [activeTab, setActiveTab] = useState<'pending' | 'history'>('pending');
  
  // Modals state
  const [receiveConfirmTransfer, setReceiveConfirmTransfer] = useState<any | null>(null);
  const [receiveQuantities, setReceiveQuantities] = useState<Record<number, number>>({});

  const { data: destTransfers = [], isLoading: loadingDest } = useQuery({
    queryKey: ['transfers', 'dest', userId],
    queryFn: () => getDestTransfers(userId!).then(r => r.data),
    enabled: !!userId,
  });

  const { data: historyTransfers = [], isLoading: loadingHistory } = useQuery({
    queryKey: ['transfers', 'history', 'dest', userId],
    queryFn: () => getDestHistory(userId!).then(r => r.data),
    enabled: !!userId && activeTab === 'history',
  });

  const receiveTransferMut = useMutation({
    mutationFn: (payload: {headerId: number, reqBody: any}) => receiveTransfer(payload.headerId, userId!, payload.reqBody).then(r => r.data),
    onSuccess: (header) => {
      toast.success(`Transfert TRF-${header.id} réceptionné avec succès !`);
      setReceiveConfirmTransfer(null);
      setReceiveQuantities({});
      qc.invalidateQueries({ queryKey: ['transfers', 'dest'] });
      qc.invalidateQueries({ queryKey: ['transfers', 'history', 'dest'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la réception');
    },
  });

  return (
    <DashboardLayout title="Espace Magasin — Réceptions IN">
      <div className="max-w-7xl mx-auto space-y-6">
        <header className="bg-gradient-to-r from-emerald-600 to-teal-700 p-8 rounded-3xl text-white shadow-lg relative overflow-hidden">
          <div className="absolute top-0 right-0 opacity-10">
            <svg className="w-48 h-48" viewBox="0 0 24 24" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
          </div>
          <div className="relative z-10">
            <h1 className="text-3xl font-black mb-2 tracking-tight">Transfer IN (Mes Demandes IN)</h1>
            <p className="text-emerald-100 font-medium">Gérez la réception des transferts inter-sites vers votre entrepôt et consultez l'historique de traçabilité.</p>
          </div>
        </header>

        <div className="flex gap-4 border-b border-slate-200 dark:border-slate-700 pb-2">
          <button
            onClick={() => setActiveTab('pending')}
            className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'pending'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-100 dark:shadow-none'
                : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700'
            }`}
          >
            📥 File d'attente ({destTransfers.length})
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
            <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-emerald-50 to-transparent dark:from-emerald-900/10">
              <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                Transferts en cours d'acheminement (IN_TRANSIT)
              </h2>
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
                    <td className="px-6 py-4 font-medium text-slate-600 dark:text-slate-400">{t.lines?.length || 0} article(s)</td>
                    <td className="px-6 py-4 text-slate-500">{t.shippedAt ? new Date(t.shippedAt).toLocaleString() : '—'}</td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => {
                          const initialQties: Record<number, number> = {};
                          t.lines?.forEach((l: any) => initialQties[l.id] = l.quantityShipped || l.quantityRequested);
                          setReceiveQuantities(initialQties);
                          setReceiveConfirmTransfer(t);
                        }}
                        className="px-5 py-2 rounded-xl bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-100 dark:shadow-none"
                      >
                        ✅ Réceptionner
                      </button>
                    </td>
                  </tr>
                ))}
                {destTransfers.length === 0 && !loadingDest && (
                  <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Aucun transfert entrant en attente de réception.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'history' && (
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden animate-fade-in">
            <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
              <h2 className="text-lg font-black text-slate-800 dark:text-white">Historique de Traçabilité (Réceptions)</h2>
              <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'history', 'dest'] })} className="text-xs font-bold text-slate-600 hover:underline">🔄 Actualiser</button>
            </div>
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
                <tr>
                  <th className="px-6 py-4">N° TRF / LTI</th>
                  <th className="px-6 py-4">Source</th>
                  <th className="px-6 py-4">Statut</th>
                  <th className="px-6 py-4">Réceptionné le</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                {historyTransfers.map((t: any) => (
                  <tr key={t.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-mono font-bold text-slate-700 dark:text-slate-200">TRF-{t.id}</div>
                      {t.ltiNumber && <div className="text-xs font-mono text-emerald-600 mt-1">{t.ltiNumber}</div>}
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-600 dark:text-slate-300">{t.warehouseSource?.name}</td>
                    <td className="px-6 py-4">
                      <span className="bg-emerald-100 text-emerald-700 px-2.5 py-1 rounded-lg text-xs font-bold dark:bg-emerald-900/30 dark:text-emerald-400">
                        RÉCEPTIONNÉ
                      </span>
                    </td>
                    <td className="px-6 py-4 text-slate-500">{t.receivedAt ? new Date(t.receivedAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
                {historyTransfers.length === 0 && !loadingHistory && (
                  <tr><td colSpan={4} className="px-6 py-20 text-center text-slate-400 italic">Aucun historique de réception.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Confirmation Réception */}
      {receiveConfirmTransfer && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/70 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-100 dark:border-slate-800 w-full max-w-lg overflow-hidden animate-fade-in-up">
            <div className="bg-gradient-to-r from-emerald-500 to-teal-500 p-6 flex items-center gap-4">
              <div className="w-12 h-12 bg-white/20 rounded-2xl flex items-center justify-center text-2xl">📦</div>
              <div>
                <h3 className="text-white font-black text-lg">Valider la réception</h3>
                <p className="text-emerald-100 text-sm">Génération automatique du LTI</p>
              </div>
            </div>
            <div className="p-6 space-y-6 max-h-[60vh] overflow-y-auto">
              <div className="space-y-2">
                <h4 className="text-xs font-bold text-slate-500 uppercase tracking-wider">Ajustement des quantités reçues</h4>
                <div className="border border-slate-200 dark:border-slate-700 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 dark:bg-slate-800">
                      <tr>
                        <th className="px-4 py-3 text-xs text-slate-500">Article</th>
                        <th className="px-4 py-3 text-xs text-slate-500 text-center">Expédié</th>
                        <th className="px-4 py-3 text-xs text-slate-500 text-center">Reçu</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                      {receiveConfirmTransfer.lines?.map((l: any) => (
                        <tr key={l.id}>
                          <td className="px-4 py-3 font-medium text-slate-700 dark:text-slate-300">
                            {l.stockItem?.itemCode} - {l.stockItem?.itemName}
                          </td>
                          <td className="px-4 py-3 text-center text-slate-500 font-bold">{l.quantityShipped || l.quantityRequested}</td>
                          <td className="px-4 py-3">
                            <input 
                              type="number" min="0" max={l.quantityShipped || l.quantityRequested}
                              value={receiveQuantities[l.id] ?? ''}
                              onChange={e => {
                                const val = parseInt(e.target.value) || 0;
                                setReceiveQuantities(prev => ({...prev, [l.id]: Math.min(val, l.quantityShipped || l.quantityRequested)}));
                              }}
                              className="w-full text-center py-1.5 rounded-lg border border-emerald-300 focus:ring-2 focus:ring-emerald-500 dark:bg-slate-800 dark:border-slate-600 outline-none font-black text-emerald-700 dark:text-emerald-400"
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
                className="flex-1 px-4 py-3 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 text-white font-black hover:opacity-90 transition-all shadow-lg shadow-emerald-200 dark:shadow-none disabled:opacity-50"
              >
                {receiveTransferMut.isPending ? 'Validation...' : 'Confirmer & Créer LTI'}
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
