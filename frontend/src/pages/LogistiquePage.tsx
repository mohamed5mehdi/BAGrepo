import { useQuery } from '@tanstack/react-query';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { formatCurrency } from '../utils/constants';

export default function LogistiquePage() {
  const { user } = useAuth();

  const { data: allPOs = [], isLoading: loadingPOs } = useQuery({
    queryKey: ['pos', 'logistics'],
    queryFn: () => api.get('/purchase-orders').then(r => r.data),
    enabled: !!user,
    refetchInterval: 20_000,
  });

  const pending  = allPOs.filter((p: any) => p.statut === 'DRAFT' || p.statut === 'PENDING_APPROVAL');
  const approved = allPOs.filter((p: any) => p.statut === 'APPROVED');
  const received = allPOs.filter((p: any) => p.statut === 'SHORT_CLOSED' || p.statut === 'REJECTED');

  const statusLabel: Record<string, { label: string; cls: string }> = {
    DRAFT:            { label: 'Brouillon',       cls: 'bg-slate-100 text-slate-500' },
    PENDING_APPROVAL: { label: 'En approbation',  cls: 'bg-amber-100 text-amber-700' },
    APPROVED:         { label: 'Approuvé',         cls: 'bg-emerald-100 text-emerald-700' },
    REJECTED:         { label: 'Rejeté',            cls: 'bg-rose-100 text-rose-600' },
    SHORT_CLOSED:     { label: 'Short-clôturé',    cls: 'bg-violet-100 text-violet-700' },
  };

  return (
    <DashboardLayout title="Logistique — Suivi des Bons de Commande" pendingCount={approved.length}>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <KpiCard label="En attente d'approbation" value={pending.length}  icon="⏳" color="from-amber-500 to-orange-600" />
        <KpiCard label="Approuvés / À réceptionner" value={approved.length} icon="🚚" color="from-blue-600 to-indigo-700" />
        <KpiCard label="Réceptionnés / Clôturés"   value={received.length} icon="✅" color="from-emerald-500 to-teal-600" />
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
        <div className="p-6 border-b border-slate-50 dark:border-slate-800 flex justify-between items-center">
          <h2 className="text-lg font-black text-slate-800 dark:text-white">Tous les Bons de Commande</h2>
          <span className="text-xs text-slate-400 font-semibold">{allPOs.length} PO(s) au total</span>
        </div>

        {loadingPOs ? (
          <div className="p-16 text-center text-slate-400">Chargement...</div>
        ) : (
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
              <tr>
                <th className="px-6 py-4">N° PO</th>
                <th className="px-6 py-4">Flux</th>
                <th className="px-6 py-4">Fournisseur</th>
                <th className="px-6 py-4">Date Création</th>
                <th className="px-6 py-4 text-right">Montant TTC</th>
                <th className="px-6 py-4 text-center">Statut</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
              {allPOs.map((po: any) => {
                const s = statusLabel[po.statut] ?? { label: po.statut, cls: 'bg-slate-100 text-slate-600' };
                return (
                  <tr key={po.id_po} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4 font-mono font-bold text-blue-600">{po.poNumber || `PO-${po.id_po}`}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase ${po.daHeader ? 'bg-slate-100 text-slate-600' : 'bg-violet-100 text-violet-600'}`}>
                        {po.daHeader ? 'Classique' : 'Interne'}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">{po.fournisseur?.nom || '—'}</td>
                    <td className="px-6 py-4 text-slate-500">{new Date(po.date_creation).toLocaleDateString('fr-FR')}</td>
                    <td className="px-6 py-4 text-right font-black text-slate-800 dark:text-slate-200">
                      {po.montant_total != null ? formatCurrency(po.montant_total) : '—'}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className={`px-3 py-1 rounded-full text-[10px] font-black uppercase ${s.cls}`}>{s.label}</span>
                    </td>
                  </tr>
                );
              })}
              {allPOs.length === 0 && (
                <tr><td colSpan={6} className="px-6 py-20 text-center text-slate-400 italic">Aucun bon de commande trouvé.</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </DashboardLayout>
  );
}
