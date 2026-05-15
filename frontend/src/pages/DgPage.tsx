import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import { getAllDA, validateWorkflow, adjustFamily, getSubFamilies } from '../api/services';
import { getAIDecision } from '../api/ai-services';
import type { DaHeader, ValidationDecision, SubFamily } from '../types';
import { formatCurrency } from '../utils/constants';

export default function DgPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DaHeader | null>(null);
  const [comment, setComment] = useState('');
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

  const { data: aiDecision, isLoading: loadingDecision } = useQuery({
    queryKey: ['ai', 'decision', selectedDa?.oid_da],
    queryFn: () => getAIDecision(selectedDa!.oid_da).then(r => r.data),
    enabled: !!selectedDa?.oid_da,
    staleTime: 60_000,
  });

  const mine = all.filter(d => ['VALIDE_DG'].includes(d.statut));

  const validateMutation = useMutation({
    mutationFn: ({ decision }: { decision: ValidationDecision }) =>
      validateWorkflow(selectedDa!.oid_da, user!.userId, decision, comment),
    onSuccess: (_, { decision }) => {
      toast.success(decision === 'ACCEPTE' ? '✅ Validation finale effectuée !' : '❌ Dossier rejeté');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null); setComment('');
    },
    onError: () => toast.error('Erreur lors de la validation'),
  });

  const adjustMutation = useMutation({
    mutationFn: () => adjustFamily(selectedDa!.oid_da, user!.userId, Number(cibleId), parseFloat(montant)),
    onSuccess: () => {
      toast.success('✅ Budget famille augmenté — dossier validé !');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
    },
    onError: (err: any) => toast.error(err?.response?.data?.message ?? 'Erreur'),
  });

  const kpis = { total: all.length, mine: mine.length, done: all.filter(d => ['APPROUVEE', 'PO_CREE', 'EN_LIVRAISON'].includes(d.statut)).length, rejected: all.filter(d => d.statut === 'REJETEE').length };

  return (
    <DashboardLayout title="Direction Générale — Arbitrage Final" pendingCount={kpis.mine}>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <KpiCard label="Toutes les DA"     value={kpis.total}    icon="📄" color="from-indigo-500 to-blue-600" />
        <KpiCard label="En attente de moi" value={kpis.mine}     icon="🏢" color="from-rose-500 to-red-700" />
        <KpiCard label="PO Créé"           value={kpis.done}     icon="✅" color="from-emerald-500 to-green-600" />
        <KpiCard label="Rejetées"          value={kpis.rejected} icon="❌" color="from-red-500 to-rose-600" />
      </div>

      <div className="flex gap-3 mb-5">
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="🔍 Rechercher..."
          className="flex-1 px-4 py-2 rounded-xl border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" />
        <button onClick={() => window.location.href='/ai-dashboard'}
          className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition-colors flex items-center gap-2">
          📊 Tableau de Bord BI IA
        </button>
        <button onClick={() => qc.invalidateQueries({ queryKey: ['da'] })}
          className="px-4 py-2 rounded-xl bg-slate-100 text-slate-600 text-sm font-medium hover:bg-slate-200 transition-colors">🔄 Actualiser</button>
      </div>

      <DaTable rows={mine as any} onRowClick={da => { setSelectedDa(da as DaHeader); setComment(''); setCibleId(''); setMontant(''); }} loading={isLoading} searchQuery={search} />

      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="🏢 Arbitrage DG">
          <div className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-700 space-y-4">
            {/* Family budget injection (if it comes from DG path) */}
            <div className="bg-rose-50 dark:bg-rose-900/20 border border-rose-200 rounded-xl p-4">
              <h4 className="font-semibold text-rose-800 dark:text-rose-300 mb-3">💰 Injection Budgétaire Famille (optionnelle)</h4>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-500 mb-1 block">Sous-famille Cible</label>
                  <select value={cibleId} onChange={e => setCibleId(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-rose-300">
                    <option value="">Choisir...</option>
                    {(subFamilies as SubFamily[]).map(sf => (
                      <option key={sf.id} value={sf.id}>{sf.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-500 mb-1 block">Montant (€)</label>
                  <input type="number" min={1} step="0.01" value={montant} onChange={e => setMontant(e.target.value)}
                    placeholder="0.00"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-rose-300" />
                </div>
              </div>
              <button
                id="btn-dg-inject"
                onClick={() => adjustMutation.mutate()}
                disabled={!cibleId || !montant || adjustMutation.isPending}
                className="mt-3 w-full py-2.5 rounded-xl bg-gradient-to-r from-rose-500 to-red-600 text-white text-sm font-semibold hover:from-rose-600 hover:to-red-700 transition-all disabled:opacity-60">
                {adjustMutation.isPending ? 'Traitement...' : '💰 Injecter Budget & Valider'}
              </button>
            </div>

            {/* Intelligence Décisionnelle IA pour DG */}
            <div className={`rounded-2xl border-2 overflow-hidden ${
              aiDecision?.recommandationColor === 'red'    ? 'border-red-400'
              : aiDecision?.recommandationColor === 'orange' ? 'border-orange-400'
              : 'border-emerald-400'
            }`}>
              <div className={`px-5 py-3 flex items-center justify-between ${
                aiDecision?.recommandationColor === 'red'    ? 'bg-gradient-to-r from-red-900 to-red-800'
                : aiDecision?.recommandationColor === 'orange' ? 'bg-gradient-to-r from-orange-900 to-amber-800'
                : 'bg-gradient-to-r from-emerald-900 to-green-800'
              }`}>
                <div className="flex items-center gap-2">
                  <span className="text-lg">🤖</span>
                  <span className="text-[11px] font-black text-white uppercase tracking-widest">Intelligence Décisionnelle IA — Arbitrage DG</span>
                </div>
                {loadingDecision ? (
                  <div className="h-6 w-28 bg-white/20 rounded-full animate-pulse" />
                ) : aiDecision ? (
                  <div className="flex items-center gap-2">
                    <span className="text-xl">{aiDecision.recommandationIcon}</span>
                    <span className="text-sm font-black text-white tracking-wide">{aiDecision.recommandation}</span>
                    <span className="text-[10px] text-white/60">({Math.round(aiDecision.confidence * 100)}% conf.)</span>
                  </div>
                ) : null}
              </div>
              <div className="p-4 bg-slate-900 space-y-3">
                {loadingDecision ? (
                  <div className="space-y-2">
                    <div className="h-3 bg-slate-700 rounded animate-pulse w-3/4" />
                    <div className="h-3 bg-slate-700 rounded animate-pulse w-1/2" />
                  </div>
                ) : aiDecision ? (
                  <>
                    {/* Score */}
                    <div>
                      <div className="flex justify-between text-[10px] font-bold text-slate-400 mb-1">
                        <span>Score de risque IA</span>
                        <span className="font-black text-white">{aiDecision.score}/100</span>
                      </div>
                      <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                        <div className="h-full rounded-full transition-all duration-700" style={{
                          width: `${aiDecision.score}%`,
                          background: aiDecision.score >= 85 ? '#ef4444' : aiDecision.score >= 50 ? '#f97316' : '#22c55e'
                        }} />
                      </div>
                    </div>
                    {/* Fournisseur + TTC */}
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-[9px] text-slate-500 uppercase font-bold">Fournisseur</p>
                        <p className="text-sm font-bold text-indigo-300">{selectedDa?.fournisseur?.nom || '—'}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-[9px] text-slate-500 uppercase font-bold">Total TTC (20%)</p>
                        <p className="text-lg font-black text-emerald-400">{formatCurrency((selectedDa?.montantEstime || 0) * 1.20)}</p>
                      </div>
                    </div>
                    {/* Impact budget */}
                    <div className="pt-2 border-t border-white/10 flex justify-between items-center">
                      <p className="text-[9px] text-slate-500 uppercase font-bold">Impact Budget Sous-Famille</p>
                      <p className="text-xs font-mono">
                        {formatCurrency(selectedDa?.budgetSousFamille?.budget_restant || 0)}
                        <span className="text-rose-400 mx-1">→</span>
                        <span className="text-emerald-400 font-bold">{formatCurrency((selectedDa?.budgetSousFamille?.budget_restant || 0) - (selectedDa?.montantEstime || 0))}</span>
                      </p>
                    </div>
                    {/* Circuit conseillé */}
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] text-slate-400 font-semibold">Circuit IA :</span>
                      <span className="text-xs font-black px-2 py-0.5 rounded bg-indigo-800 text-indigo-200">{aiDecision.suggestedRole}</span>
                    </div>
                    {/* Anomalie */}
                    {aiDecision.anomalie && (
                      <div className={`text-[11px] font-bold px-3 py-2 rounded-xl border ${
                        aiDecision.anomalie.niveau === 'CRITIQUE'
                          ? 'bg-red-950/60 border-red-700 text-red-300'
                          : 'bg-amber-950/60 border-amber-700 text-amber-300'
                      }`}>
                        {aiDecision.anomalie.niveau === 'CRITIQUE' ? '⛔' : '⚠️'} Anomalie {aiDecision.anomalie.niveau} — Z={aiDecision.anomalie.zScore.toFixed(2)}<br />
                        <span className="font-normal opacity-80">{aiDecision.anomalie.raison}</span>
                      </div>
                    )}
                    {/* Justifications */}
                    {aiDecision.justifications?.length > 0 && (
                      <ul className="space-y-1">
                        {aiDecision.justifications.map((j: string, i: number) => (
                          <li key={i} className="text-[11px] text-slate-400 flex items-start gap-1.5">
                            <span className="text-indigo-400">›</span>{j}
                          </li>
                        ))}
                      </ul>
                    )}
                  </>
                ) : (
                  <p className="text-xs text-slate-500">Analyse indisponible</p>
                )}
              </div>
            </div>

            {/* Standard validation */}
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Décision Finale</p>
              <textarea value={comment} onChange={e => setComment(e.target.value)} rows={2}
                placeholder="Commentaire (facultatif)..."
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-rose-300 resize-none" />
            </div>

            <div className="flex justify-end gap-3">
              <button onClick={() => setSelectedDa(null)} className="px-5 py-2 rounded-xl bg-slate-100 text-slate-700 text-sm font-medium hover:bg-slate-200 transition-colors">Fermer</button>
              <button
                id="btn-dg-reject"
                onClick={() => validateMutation.mutate({ decision: 'REJETE' })}
                disabled={validateMutation.isPending}
                className="px-5 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 border border-red-200 text-sm font-semibold transition-colors disabled:opacity-60">
                ✖ Rejet Définitif
              </button>
              <button
                id="btn-dg-validate"
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
