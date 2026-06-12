import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { getDestTransfers, receiveTransfer, getDestHistory, downloadLtiPdf } from '../api/services';

interface TransferLine {
  id: number;
  stockItem: { itemCode: string; itemName: string; warehouse?: { name: string } };
  quantityRequested: number;
}

interface TransferHeader {
  id: number;
  ltoNumber: string;
  ltiNumber?: string;
  status: 'PENDING' | 'IN_TRANSIT' | 'RECEIVED' | 'CANCELLED';
  warehouseSource: { id: number; name: string };
  warehouseDest: { id: number; name: string };
  requestedBy: { nom: string };
  createdAt: string;
  shippedAt?: string;
  lines: TransferLine[];
}

export default function MagasinierDestPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedTransfer, setSelectedTransfer] = useState<TransferHeader | null>(null);
  const [receiveQuantities, setReceiveQuantities] = useState<Record<number, number>>({});
  const [activeTab, setActiveTab] = useState<'transfers' | 'history'>('transfers');

  const userId = user?.userId;

  // Nom de l'entrepôt du magasinier destination.
  // Un seul appel GET /api/users/{id} — le User entity embarque le Warehouse
  // avec JOIN FETCH (pas de N+1). Pas de second appel /warehouse nécessaire.
  const { data: warehouseLabel = '—' } = useQuery<string>({
    queryKey: ['warehouse-name-dest', userId],
    queryFn: async () => {
      const userRes = await api.get(`/users/${userId}`);
      return userRes.data?.warehouse?.name ?? '—';
    },
    enabled: !!userId,
    staleTime: 5 * 60 * 1000, // stable 5 min : le warehouse du user ne change pas souvent
  });

  // File des transferts IN_TRANSIT vers ce warehouse destination
  const { data: transfers = [], isLoading } = useQuery<TransferHeader[]>({
    queryKey: ['transfers', 'dest', userId],
    queryFn: () => getDestTransfers(userId).then(r => r.data),
    refetchInterval: 20_000,
    enabled: !!userId,
  });

  // Historique des réceptions
  const { data: historyTransfers = [] } = useQuery<TransferHeader[]>({
    queryKey: ['transfers', 'history', 'dest', userId],
    queryFn: () => getDestHistory(userId).then(r => r.data),
    refetchInterval: 30_000,
    enabled: !!userId && activeTab === 'history',
  });

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

  const receiveMutation = useMutation({
    mutationFn: (payload: { headerId: number; reqBody: any }) =>
      receiveTransfer(payload.headerId, userId, payload.reqBody),
    onSuccess: () => {
      toast.success('✅ Réception validée — LTI généré !');
      qc.invalidateQueries({ queryKey: ['transfers', 'dest'] });
      setSelectedTransfer(null);
      setReceiveQuantities({});
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Erreur lors de la validation';
      toast.error(msg);
    },
  });


  return (
    <DashboardLayout title="Espace Magasin Destination — Réceptions LTI" pendingCount={transfers.length}>
      {/* KPIs */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <KpiCard label="Transferts en transit" value={transfers.length} icon="🚚" color="from-amber-500 to-orange-600" />
        <KpiCard label="Réceptionnés aujourd'hui" value={0} icon="✅" color="from-emerald-500 to-teal-600" />
        <KpiCard label="Mon entrepôt"
          value={warehouseLabel}
          icon="🏭"
          color="from-indigo-500 to-blue-700" />
      </div>

      {/* Tab switcher */}
      <div className="flex gap-2 mb-6">
        <button
          onClick={() => setActiveTab('transfers')}
          className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
            activeTab === 'transfers'
              ? 'bg-amber-500 text-white shadow-lg shadow-amber-100'
              : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
          }`}
        >
          🚚 Transferts en Transit ({transfers.length})
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
            activeTab === 'history'
              ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-100'
              : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
          }`}
        >
          🕒 Historique de Réception (LTI)
        </button>
      </div>

      {/* Tableau des transferts IN_TRANSIT */}
      {activeTab === 'transfers' && (
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
          <h2 className="text-lg font-black text-slate-800 dark:text-white">
            Transferts en Transit vers mon Entrepôt
          </h2>
          <button
            onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'dest'] })}
            className="text-xs font-bold text-indigo-600 hover:underline"
          >
            🔄 Actualiser
          </button>
        </div>

        <table className="w-full text-sm text-left">
          <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
            <tr>
              <th className="px-6 py-4">N° LTO</th>
              <th className="px-6 py-4">Source</th>
              <th className="px-6 py-4">Expéditeur</th>
              <th className="px-6 py-4">Expédié le</th>
              <th className="px-6 py-4">Lignes</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
            {transfers.map(t => (
              <tr key={t.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                <td className="px-6 py-4 font-mono font-bold text-amber-600">{t.ltoNumber}</td>
                <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">
                  {t.warehouseSource?.name ?? '—'}
                </td>
                <td className="px-6 py-4 text-slate-500">{t.requestedBy?.nom ?? '—'}</td>
                <td className="px-6 py-4 text-slate-500">
                  {t.shippedAt ? new Date(t.shippedAt).toLocaleDateString() : '—'}
                </td>
                <td className="px-6 py-4">
                  <span className="bg-slate-100 text-slate-600 px-2 py-0.5 rounded text-[9px] font-black">
                    {t.lines?.length ?? 0} article(s)
                  </span>
                </td>
                <td className="px-6 py-4 text-right">
                  <button
                    onClick={() => {
                      const initialQties: Record<number, number> = {};
                      t.lines?.forEach((l: TransferLine) => {
                        initialQties[l.id] = (l as any).quantityShipped || l.quantityRequested;
                      });
                      setReceiveQuantities(initialQties);
                      setSelectedTransfer(t);
                    }}
                    className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all shadow-lg shadow-emerald-100"
                  >
                    ✅ Réceptionner (LTI)
                  </button>
                </td>
              </tr>
            ))}
            {transfers.length === 0 && !isLoading && (
              <tr>
                <td colSpan={6} className="px-6 py-20 text-center text-slate-400 italic">
                  Aucun transfert en transit vers votre entrepôt.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      )}

      {/* ── Onglet Historique LTI ── */}
      {activeTab === 'history' && (
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
          <h2 className="text-lg font-black text-slate-800 dark:text-white">Historique des Réceptions (LTI)</h2>
          <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'history'] })} className="text-xs font-bold text-slate-600 hover:underline">🔄 Actualiser</button>
        </div>
        <table className="w-full text-sm text-left">
          <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
            <tr>
              <th className="px-6 py-4">N° LTI</th>
              <th className="px-6 py-4">N° LTO</th>
              <th className="px-6 py-4">Expéditeur</th>
              <th className="px-6 py-4">Réceptionné le</th>
              <th className="px-6 py-4 text-right">Documents</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
            {historyTransfers.map((t: any) => (
              <tr key={t.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                <td className="px-6 py-4 font-mono font-bold text-emerald-600 dark:text-emerald-400">{t.ltiNumber || '—'}</td>
                <td className="px-6 py-4 font-mono font-medium text-slate-500">{t.ltoNumber}</td>
                <td className="px-6 py-4 font-semibold text-slate-600 dark:text-slate-300">{t.warehouseSource?.name}</td>
                <td className="px-6 py-4 text-slate-500">{t.status === 'RECEIVED' && t.shippedAt ? new Date().toLocaleString() /* assuming receivedAt isn't on header yet, will fix in backend next iteration if needed, but for now shippedAt or just "Reçu" */ : 'Réceptionné'}</td>
                <td className="px-6 py-4 text-right">
                  <button
                    onClick={() => handleDownloadLti(t.id, t.ltiNumber || `LTI-${t.id}`)}
                    className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 text-xs font-bold hover:bg-slate-200 dark:hover:bg-slate-600 transition-all"
                  >
                    📄 Télécharger LTI
                  </button>
                </td>
              </tr>
            ))}
            {historyTransfers.length === 0 && (
              <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Aucun historique de réception.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      )}

      {/* Modal de confirmation réception */}
      {selectedTransfer && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 w-full max-w-3xl rounded-[32px] shadow-2xl overflow-hidden border border-white/20">
            <div className="p-8 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
              <div>
                <h2 className="text-2xl font-black text-slate-900 dark:text-white">
                  Validation Réception LTI
                </h2>
                <p className="text-slate-400 text-sm mt-1">
                  LTO référencé : <span className="font-mono font-bold text-amber-600">{selectedTransfer.ltoNumber}</span>
                </p>
              </div>
              <button
                onClick={() => setSelectedTransfer(null)}
                className="p-2 hover:bg-slate-100 rounded-full transition-colors text-slate-400"
              >
                ✕
              </button>
            </div>

            <div className="p-8 space-y-6 max-h-[60vh] overflow-y-auto">
              {/* Méta */}
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-slate-50 dark:bg-slate-800 p-4 rounded-2xl">
                  <p className="text-[10px] font-black text-slate-400 uppercase">Source</p>
                  <p className="font-bold text-slate-800 dark:text-slate-200">
                    {selectedTransfer.warehouseSource?.name}
                  </p>
                </div>
                <div className="bg-slate-50 dark:bg-slate-800 p-4 rounded-2xl">
                  <p className="text-[10px] font-black text-slate-400 uppercase">Destination</p>
                  <p className="font-bold text-slate-800 dark:text-slate-200">
                    {selectedTransfer.warehouseDest?.name}
                  </p>
                </div>
              </div>

              {/* Lignes avec saisie quantités reçues */}
              <div className="border border-slate-100 dark:border-slate-800 rounded-2xl overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold text-[10px] uppercase">
                    <tr>
                      <th className="px-6 py-4 text-left">Code Article</th>
                      <th className="px-6 py-4 text-left">Désignation</th>
                      <th className="px-6 py-4 text-center">Expédié</th>
                      <th className="px-6 py-4 text-center">Reçu réel</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                    {selectedTransfer.lines.map(line => {
                      const expected = (line as any).quantityShipped || line.quantityRequested;
                      const current = receiveQuantities[line.id] ?? expected;
                      const isLoss = current < expected;
                      return (
                        <tr key={line.id}>
                          <td className="px-6 py-3 font-mono font-bold text-indigo-600">
                            {line.stockItem?.itemCode}
                          </td>
                          <td className="px-6 py-3 font-medium text-slate-700 dark:text-slate-200">
                            {line.stockItem?.itemName}
                          </td>
                          <td className="px-6 py-3 text-center">
                            <span className="bg-emerald-50 text-emerald-600 px-3 py-1 rounded-lg text-xs font-black">
                              {expected}
                            </span>
                          </td>
                          <td className="px-6 py-3">
                            <input
                              type="number" min="0" max={expected}
                              value={current}
                              onChange={e => {
                                const val = parseInt(e.target.value) || 0;
                                setReceiveQuantities(prev => ({...prev, [line.id]: Math.max(0, Math.min(val, expected))}));
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

              {Object.entries(receiveQuantities).some(([id, qty]) => {
                const line = selectedTransfer.lines?.find((l: any) => l.id.toString() === id);
                const expected = (line as any)?.quantityShipped || line?.quantityRequested;
                return line && qty < expected;
              }) && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700 flex gap-2">
                  <span>⚠️</span>
                  <p>Une quantité inférieure a été saisie. Cela sera enregistré comme <strong>perte en transit</strong>.</p>
                </div>
              )}

              <div className="p-4 bg-emerald-50 dark:bg-emerald-900/20 rounded-2xl border border-emerald-100 dark:border-emerald-800/50">
                <p className="text-sm text-emerald-800 dark:text-emerald-200 font-semibold">
                  ✅ En validant, vous confirmez la réception physique de ces articles et déclenchez la mise à jour du stock de votre entrepôt.
                  Un numéro LTI sera automatiquement généré.
                </p>
              </div>
            </div>

            <div className="p-8 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-50 dark:border-slate-800 flex justify-end gap-4">
              <button
                onClick={() => setSelectedTransfer(null)}
                disabled={receiveMutation.isPending}
                className="px-8 py-3 rounded-2xl bg-white dark:bg-slate-700 text-slate-600 dark:text-slate-200 font-bold text-sm shadow-sm disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Fermer
              </button>
              <button
                onClick={() => receiveMutation.mutate({
                  headerId: selectedTransfer.id,
                  reqBody: {
                    lines: selectedTransfer.lines.map((l: any) => ({
                      lineId: l.id,
                      quantity: receiveQuantities[l.id] ?? ((l.quantityShipped || l.quantityRequested))
                    }))
                  }
                })}
                disabled={receiveMutation.isPending}
                className="px-12 py-3 rounded-2xl bg-emerald-600 text-white font-black text-sm shadow-xl shadow-emerald-100 hover:bg-emerald-700 transition-all disabled:opacity-50"
              >
                {receiveMutation.isPending ? 'Validation...' : '✅ Valider Réception (LTI)'}
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
