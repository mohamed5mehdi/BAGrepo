import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { getAllDA, adjustSubFamily, getSubFamilies } from '../api/services';
import type { DemandeAchatInterne, SubFamily, ValidationDecision } from '../types';
import { formatCurrency } from '../utils/constants';

export default function DafPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DemandeAchatInterne | null>(null);
  const [comment, setComment] = useState('');
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

  const mine = all.filter(d => ['VALIDE_DAF', 'EN_ATTENTE_AJUSTEMENT_DAF'].includes(d.statut));

  const adjustMutation = useMutation({
    mutationFn: () => adjustSubFamily(
      (selectedDa as any)!.id || selectedDa!.oid_da, user!.userId,
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

  const validateMutation = useMutation({
    mutationFn: ({ decision }: { decision: ValidationDecision }) =>
      api.put(`/demandes/${(selectedDa as any)!.id || selectedDa!.oid_da}/valider-daf?approved=${decision === 'ACCEPTE'}&comment=${encodeURIComponent(comment)}&userId=${user!.userId}`),
    onSuccess: (_, { decision }) => {
      toast.success(decision === 'ACCEPTE' ? '✅ Validation DAF effectuée !' : '❌ Dossier renvoyé en brouillon');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null); setComment('');
    },
    onError: () => toast.error('Erreur lors de la validation'),
  });

  const kpis = {
    total:   all.length,
    mine:    mine.length,
    done:    all.filter(d => ['APPROUVEE', 'PO_CREE', 'EN_LIVRAISON', 'AFFECTEE'].includes(d.statut)).length,
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

      <DaTable rows={mine as any} onRowClick={(da) => { setSelectedDa(da as DemandeAchatInterne); setComment(''); setSourceId(''); setCibleId(''); setMontant(''); }} loading={isLoading} searchQuery={search} />

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

              <button
                id="btn-daf-adjust"
                onClick={() => adjustMutation.mutate()}
                disabled={!sourceId || !cibleId || !montant || adjustMutation.isPending}
                className="mt-3 w-full py-2.5 rounded-xl bg-gradient-to-r from-fuchsia-500 to-purple-700 text-white text-sm font-semibold hover:from-fuchsia-600 hover:to-purple-800 transition-all shadow-md disabled:opacity-60">
                {adjustMutation.isPending ? 'Traitement...' : '💰 Effectuer le Transfert'}
              </button>

            {/* Standard validation */}
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Décision Finale (DAF)</p>
              <textarea value={comment} onChange={e => setComment(e.target.value)} rows={2}
                placeholder="Commentaire (facultatif)..."
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-fuchsia-300 resize-none" />
            </div>

            <div className="flex justify-end gap-3">
              <button onClick={() => setSelectedDa(null)} className="px-5 py-2 rounded-xl bg-slate-100 text-slate-700 text-sm font-medium hover:bg-slate-200 transition-colors">Fermer</button>
              <button
                id="btn-daf-reject"
                onClick={() => validateMutation.mutate({ decision: 'REJETE' })}
                disabled={validateMutation.isPending}
                className="px-5 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 border border-red-200 text-sm font-semibold transition-colors disabled:opacity-60">
                ✖ Rejeter (Brouillon)
              </button>
              <button
                id="btn-daf-validate"
                onClick={() => validateMutation.mutate({ decision: 'ACCEPTE' })}
                disabled={validateMutation.isPending}
                className="px-5 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-green-600 text-white text-sm font-semibold hover:from-emerald-600 hover:to-green-700 transition-all shadow-md disabled:opacity-60">
                ✔ Validation Finale
              </button>
            </div>
          </div>
        </DaModal>
      )}
    </DashboardLayout>
  );
}
