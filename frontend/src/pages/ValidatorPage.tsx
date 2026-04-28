import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import { getAllDA, validateWorkflow } from '../api/services';
import type { DaHeader, ValidationDecision } from '../types';

interface Props {
  role: 'ROLE_N1' | 'ROLE_TECHNICIEN' | 'ROLE_AMG' | 'ROLE_DG';
  myStatut: string;   // statut that belongs to this role
  title: string;
  icon: string;
  color: string;
}

export default function ValidatorPage({ role, myStatut, title, icon, color }: Props) {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DaHeader | null>(null);
  const [comment, setComment] = useState('');
  const [search, setSearch] = useState('');

  const { data: all = [], isLoading } = useQuery({
    queryKey: ['da', 'all'],
    queryFn: () => getAllDA().then(r => r.data),
    refetchInterval: 30_000,
  });

  const mine = all.filter(d => d.statut === myStatut);

  const validateMutation = useMutation({
    mutationFn: ({ decision }: { decision: ValidationDecision }) =>
      validateWorkflow(selectedDa!.oid_da, user!.userId, decision, comment),
    onSuccess: (_, { decision }) => {
      toast.success(decision === 'ACCEPTE' ? '✅ Demande validée !' : '❌ Demande rejetée');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
      setComment('');
    },
    onError: () => toast.error('Erreur lors de la validation'),
  });

  const kpis = {
    total:   all.length,
    mine:    mine.length,
    done:    all.filter(d => d.statut === 'PO_CREE').length,
    rejected:all.filter(d => d.statut === 'REJETEE').length,
  };

  return (
    <DashboardLayout title={title} pendingCount={kpis.mine}>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <KpiCard label="Toutes les DA"      value={kpis.total}    icon="📄" color="from-indigo-500 to-blue-600" />
        <KpiCard label="En attente de moi"  value={kpis.mine}     icon={icon} color={color} />
        <KpiCard label="PO Créé"            value={kpis.done}     icon="✅" color="from-emerald-500 to-green-600" />
        <KpiCard label="Rejetées"           value={kpis.rejected} icon="❌" color="from-red-500 to-rose-600" />
      </div>

      <div className="flex gap-3 mb-5">
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="🔍 Rechercher..."
          className="flex-1 px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" />
        <button onClick={() => qc.invalidateQueries({ queryKey: ['da'] })}
          className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 text-sm font-medium hover:bg-slate-200 transition-colors">
          🔄 Actualiser
        </button>
      </div>

      <DaTable rows={mine} onRowClick={setSelectedDa} loading={isLoading} searchQuery={search} />

      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => { setSelectedDa(null); setComment(''); }}>
          {/* Action panel */}
          <div className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-700 space-y-3">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Votre décision</p>
            <textarea
              value={comment} onChange={e => setComment(e.target.value)} rows={2}
              placeholder="Commentaire (facultatif)..."
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300 resize-none"
            />
            <div className="flex justify-end gap-3">
              <button onClick={() => { setSelectedDa(null); setComment(''); }}
                className="px-5 py-2 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-200 text-sm font-medium hover:bg-slate-200 transition-colors">
                Fermer
              </button>
              {selectedDa.statut === myStatut && (
                <>
                  <button
                    id="btn-reject"
                    onClick={() => validateMutation.mutate({ decision: 'REJETE' })}
                    disabled={validateMutation.isPending}
                    className="px-5 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 border border-red-200 text-sm font-semibold transition-colors disabled:opacity-60"
                  >
                    ✖ Rejeter
                  </button>
                  <button
                    id="btn-validate"
                    onClick={() => validateMutation.mutate({ decision: 'ACCEPTE' })}
                    disabled={validateMutation.isPending}
                    className="px-5 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-green-600 text-white text-sm font-semibold hover:from-emerald-600 hover:to-green-700 transition-all shadow-md shadow-green-200 disabled:opacity-60"
                  >
                    ✔ Valider
                  </button>
                </>
              )}
            </div>
          </div>
        </DaModal>
      )}
    </DashboardLayout>
  );
}
