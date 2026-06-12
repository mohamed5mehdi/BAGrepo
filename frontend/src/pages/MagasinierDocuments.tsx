import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import { useAuth } from '../context/AuthContext';
import { getSourceHistory, getDestHistory, downloadLtoPdf, downloadLtiPdf } from '../api/services';

export default function MagasinierDocuments() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const userId = user?.userId;

  const [activeTab, setActiveTab] = useState<'LTO' | 'LTI'>('LTO');

  // LTOs (Expéditions depuis ce magasin)
  const { data: ltoHistory = [], isLoading: loadingLTO } = useQuery({
    queryKey: ['transfers', 'history', 'source', userId],
    queryFn: () => getSourceHistory(userId!).then(r => r.data),
    enabled: !!userId && activeTab === 'LTO',
  });

  // LTIs (Réceptions dans ce magasin)
  const { data: ltiHistory = [], isLoading: loadingLTI } = useQuery({
    queryKey: ['transfers', 'history', 'dest', userId],
    queryFn: () => getDestHistory(userId!).then(r => r.data),
    enabled: !!userId && activeTab === 'LTI',
  });

  const handleDownloadLto = async (id: number, ltoNumber: string) => {
    try {
      const res = await downloadLtoPdf(id);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${ltoNumber || 'LTO'}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success('Document LTO téléchargé.');
    } catch (e) {
      toast.error('Erreur lors du téléchargement du LTO');
    }
  };

  const handleDownloadLti = async (id: number, ltiNumber: string) => {
    try {
      const res = await downloadLtiPdf(id);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${ltiNumber || 'LTI'}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success('Document LTI téléchargé.');
    } catch (e) {
      toast.error('Erreur lors du téléchargement du LTI');
    }
  };

  return (
    <DashboardLayout title="Espace Magasin — Documents LTO/LTI">
      <div className="max-w-7xl mx-auto space-y-6">
        <header className="bg-gradient-to-r from-slate-700 to-slate-900 p-8 rounded-3xl text-white shadow-lg relative overflow-hidden">
          <div className="absolute top-0 right-0 opacity-10">
            <svg className="w-48 h-48" viewBox="0 0 24 24" fill="currentColor"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>
          </div>
          <div className="relative z-10">
            <h1 className="text-3xl font-black mb-2 tracking-tight">Documents de Transfert</h1>
            <p className="text-slate-300 font-medium">Consultez et téléchargez les bons de transfert inter-sites (LTO / LTI).</p>
          </div>
        </header>

        <div className="flex gap-4 border-b border-slate-200 dark:border-slate-700 pb-2">
          <button
            onClick={() => setActiveTab('LTO')}
            className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'LTO'
                ? 'bg-amber-500 text-white shadow-lg shadow-amber-100 dark:shadow-none'
                : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700'
            }`}
          >
            📤 LTO (Sorties)
          </button>
          <button
            onClick={() => setActiveTab('LTI')}
            className={`px-6 py-2 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'LTI'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-100 dark:shadow-none'
                : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700'
            }`}
          >
            📥 LTI (Entrées)
          </button>
        </div>

        {activeTab === 'LTO' && (
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden animate-fade-in">
            <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-amber-50 to-transparent dark:from-amber-900/10">
              <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2">
                Local Transfer Out (Bons de Sortie)
              </h2>
              <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'history', 'source'] })} className="text-xs font-bold text-amber-600 hover:underline">🔄 Actualiser</button>
            </div>
            
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
                <tr>
                  <th className="px-6 py-4">N° LTO / TRF</th>
                  <th className="px-6 py-4">Destination</th>
                  <th className="px-6 py-4">Date de sortie</th>
                  <th className="px-6 py-4 text-right">Document</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                {ltoHistory.map((t: any) => (
                  <tr key={t.id} className="hover:bg-amber-50/30 dark:hover:bg-amber-900/10 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-mono font-bold text-amber-600">{t.ltoNumber || 'En cours...'}</div>
                      <div className="text-xs font-mono text-slate-400 mt-1">TRF-{t.id}</div>
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">{t.warehouseDest?.name}</td>
                    <td className="px-6 py-4 text-slate-500">{t.shippedAt ? new Date(t.shippedAt).toLocaleString() : '—'}</td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleDownloadLto(t.id, t.ltoNumber)}
                        disabled={!t.ltoNumber}
                        className="px-5 py-2 rounded-xl bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300 text-xs font-bold hover:bg-slate-200 dark:hover:bg-slate-700 transition-all disabled:opacity-50"
                      >
                        📄 Télécharger PDF
                      </button>
                    </td>
                  </tr>
                ))}
                {ltoHistory.length === 0 && !loadingLTO && (
                  <tr><td colSpan={4} className="px-6 py-20 text-center text-slate-400 italic">Aucun LTO disponible.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'LTI' && (
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden animate-fade-in">
            <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center bg-gradient-to-r from-emerald-50 to-transparent dark:from-emerald-900/10">
              <h2 className="text-lg font-black text-slate-800 dark:text-white">Local Transfer In (Bons d'Entrée)</h2>
              <button onClick={() => qc.invalidateQueries({ queryKey: ['transfers', 'history', 'dest'] })} className="text-xs font-bold text-emerald-600 hover:underline">🔄 Actualiser</button>
            </div>
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
                <tr>
                  <th className="px-6 py-4">N° LTI / TRF</th>
                  <th className="px-6 py-4">Source</th>
                  <th className="px-6 py-4">Date d'entrée</th>
                  <th className="px-6 py-4 text-right">Document</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                {ltiHistory.map((t: any) => (
                  <tr key={t.id} className="hover:bg-emerald-50/30 dark:hover:bg-emerald-900/10 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-mono font-bold text-emerald-600">{t.ltiNumber || 'En cours...'}</div>
                      <div className="text-xs font-mono text-slate-400 mt-1">TRF-{t.id}</div>
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">{t.warehouseSource?.name}</td>
                    <td className="px-6 py-4 text-slate-500">{t.receivedAt ? new Date(t.receivedAt).toLocaleString() : '—'}</td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleDownloadLti(t.id, t.ltiNumber)}
                        disabled={!t.ltiNumber}
                        className="px-5 py-2 rounded-xl bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300 text-xs font-bold hover:bg-slate-200 dark:hover:bg-slate-700 transition-all disabled:opacity-50"
                      >
                        📄 Télécharger PDF
                      </button>
                    </td>
                  </tr>
                ))}
                {ltiHistory.length === 0 && !loadingLTI && (
                  <tr><td colSpan={4} className="px-6 py-20 text-center text-slate-400 italic">Aucun LTI disponible.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
