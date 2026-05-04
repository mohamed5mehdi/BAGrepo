import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import { getAllDA, adjustSubFamily, getSubFamilies } from '../api/services';
import type { DaHeader, SubFamily } from '../types';
import { formatCurrency } from '../utils/constants';

export default function DafPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DaHeader | null>(null);
  const [sourceId, setSourceId] = useState('');
  const [cibleId, setCibleId] = useState('');
  const [montant, setMontant] = useState('');
  const [search, setSearch] = useState('');

  const { data: all = [], isLoading } = useQuery({
    queryKey: ['da', 'all'],
    queryFn: () => getAllDA().then(r => r.data),
    refetchInterval: 30_000,
  });

  const { data: subFamilies = [] } = useQuery({
    queryKey: ['sub-families'],
    queryFn: () => getSubFamilies().then(r => r.data),
  });

  const mine = all.filter(d => ['EN_VALIDATION_DAF', 'AJUSTEMENT_DAF'].includes(d.statut));

  const adjustMutation = useMutation({
    mutationFn: () => adjustSubFamily(
      selectedDa!.oid_da, user!.userId,
      Number(sourceId), Number(cibleId), parseFloat(montant)
    ),
    onSuccess: () => {
      toast.success('✅ Ajustement sous-famille effectué — dossier renvoyé vers DG');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
      setSourceId(''); setCibleId(''); setMontant('');
    },
    onError: (err: any) => toast.error(err?.response?.data?.message ?? 'Erreur lors de l\'ajustement'),
  });

  const kpis = {
    total:   all.length,
    mine:    mine.length,
    done:    all.filter(d => ['APPROUVEE', 'PO_CREE', 'EN_LIVRAISON'].includes(d.statut)).length,
    rejected:all.filter(d => d.statut === 'REJETEE').length,
  };

  return (
    <DashboardLayout title="Dashboard DAF — Contrôle Budgétaire" pendingCount={kpis.mine}>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <KpiCard label="Toutes les DA"     value={kpis.total}    icon="📄" color="from-indigo-500 to-blue-600" />
        <KpiCard label="En attente de moi" value={kpis.mine}     icon="💰" color="from-fuchsia-500 to-purple-700" />
        <KpiCard label="PO Créé"           value={kpis.done}     icon="✅" color="from-emerald-500 to-green-600" />
        <KpiCard label="Rejetées"          value={kpis.rejected} icon="❌" color="from-red-500 to-rose-600" />
      </div>

      <div className="flex gap-3 mb-5">
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="🔍 Rechercher..."
          className="flex-1 px-4 py-2 rounded-xl border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" />
        <button onClick={() => qc.invalidateQueries({ queryKey: ['da'] })}
          className="px-4 py-2 rounded-xl bg-slate-100 text-slate-600 text-sm font-medium hover:bg-slate-200 transition-colors">🔄 Actualiser</button>
      </div>

      <DaTable rows={mine as any} onRowClick={(da) => setSelectedDa(da as DaHeader)} loading={isLoading} searchQuery={search} />

      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="💰 Ajustement Budgétaire — DAF">
          <div className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-700 space-y-4">
            <div className="bg-fuchsia-50 dark:bg-fuchsia-900/20 border border-fuchsia-200 rounded-xl p-4">
              <h4 className="font-semibold text-fuchsia-800 dark:text-fuchsia-300 mb-3">
                📂 Transfert de Budget entre Sous-Familles
              </h4>
              <div className="grid grid-cols-1 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-500 mb-1 block">Sous-famille Source (budget prélevé)</label>
                  <select value={sourceId} onChange={e => setSourceId(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-fuchsia-300">
                    <option value="">Choisir la source...</option>
                    {(subFamilies as SubFamily[]).map(sf => (
                      <option key={sf.id} value={sf.id}>
                        {sf.name} — Restant: {formatCurrency(sf.budget_restant ?? 0)}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-500 mb-1 block">Sous-famille Cible (budget crédité)</label>
                  <select value={cibleId} onChange={e => setCibleId(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-fuchsia-300">
                    <option value="">Choisir la cible...</option>
                    {(subFamilies as SubFamily[]).filter(sf => String(sf.id) !== sourceId).map(sf => (
                      <option key={sf.id} value={sf.id}>{sf.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-500 mb-1 block">Montant du transfert (€)</label>
                  <input type="number" min={1} step="0.01" value={montant} onChange={e => setMontant(e.target.value)}
                    placeholder="0.00"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-fuchsia-300" />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3">
              <button onClick={() => setSelectedDa(null)}
                className="px-5 py-2 rounded-xl bg-slate-100 text-slate-700 text-sm font-medium hover:bg-slate-200 transition-colors">
                Fermer
              </button>
              <button
                id="btn-daf-adjust"
                onClick={() => adjustMutation.mutate()}
                disabled={!sourceId || !cibleId || !montant || adjustMutation.isPending}
                className="px-6 py-2 rounded-xl bg-gradient-to-r from-fuchsia-500 to-purple-700 text-white text-sm font-semibold hover:from-fuchsia-600 hover:to-purple-800 transition-all shadow-md disabled:opacity-60">
                {adjustMutation.isPending ? 'Traitement...' : '💰 Effectuer le Transfert'}
              </button>
            </div>
          </div>
        </DaModal>
      )}
    </DashboardLayout>
  );
}
