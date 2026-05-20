import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import { getDemandesAValiderInternes, validerN1DemandeInterne, validerTechDemandeInterne, validerAMGDemandeInterne, validerDAFDemandeInterne, validerDGDemandeInterne } from '../api/services';
import { getAIDecision } from '../api/ai-services';
import { formatCurrency } from '../utils/constants';

interface Props {
  role: 'MANAGER_N1' | 'TECHNICIEN' | 'AMG' | 'DAF' | 'DG';
  myStatut: string;
  title: string;
  icon: string;
  color: string;
}

export default function ValidatorPage({ role, myStatut, title, icon, color }: Props) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<any | null>(null);
  const [decision, setDecision] = useState<'ACCEPTE' | 'REJETE' | null>(null);
  const [comment, setComment] = useState('');
  const [search, setSearch] = useState('');

  // ── Décision IA pour la DA sélectionnée ────────────────────────────
  const { data: aiDecision, isLoading: loadingDecision } = useQuery({
    queryKey: ['ai', 'decision', selectedDa?.id],
    queryFn: () => getAIDecision(selectedDa!.id).then(r => r.data),
    enabled: !!selectedDa?.id,
    staleTime: 60_000,
  });

  const { data: mine = [], isLoading } = useQuery({
    queryKey: ['da', 'a-valider', role, user?.userId],
    queryFn: () => getDemandesAValiderInternes(user!.userId).then(r => r.data),
    refetchInterval: 30_000,
    enabled: !!user,
  });

  const validateMutation = useMutation({
    mutationFn: async () => {
      const isAccepted = decision === 'ACCEPTE';
      const id = selectedDa.id;
      const uid = user!.userId;
      
      switch (role) {
        case 'MANAGER_N1': return validerN1DemandeInterne(id, isAccepted, comment, uid);
        case 'TECHNICIEN': return validerTechDemandeInterne(id, isAccepted, comment, uid);
        case 'AMG':        return validerAMGDemandeInterne(id, isAccepted, comment, uid);
        case 'DAF':        return validerDAFDemandeInterne(id, isAccepted, comment, uid);
        case 'DG':         return validerDGDemandeInterne(id, isAccepted, comment, uid);
      }
    },
    onSuccess: () => {
      toast.success('Action enregistrée avec succès !');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
      setComment('');
      setDecision(null);
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la validation');
    },
  });

  const kpis = {
    mine:    mine.length,
    total:   mine.length, // Simplified for this view
  };

  return (
    <DashboardLayout title={title} pendingCount={kpis.mine}>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <KpiCard label="En attente de validation"  value={kpis.mine}     icon={icon} color={color} />
      </div>

      <div className="flex gap-3 mb-5">
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="🔍 Rechercher une demande..."
          className="flex-1 px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" />
        <button onClick={() => navigate('/ai-dashboard')}
          className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition-colors flex items-center gap-2">
          📊 Tableau de Bord BI IA
        </button>
        <button onClick={() => qc.invalidateQueries({ queryKey: ['da'] })}
          className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 text-sm font-medium hover:bg-slate-200 transition-colors">
          🔄 Actualiser
        </button>
      </div>

      <DaTable rows={mine} onRowClick={setSelectedDa} loading={isLoading} searchQuery={search} showRequester={true} />

      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => { setSelectedDa(null); setComment(''); setDecision(null); }} title="Décision de Validation" wide>
          <div className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-700 space-y-6">

            {/* ── Panneau Décisionnel IA ─────────────────────────── */}
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
                  <span className="text-[11px] font-black text-white uppercase tracking-widest">Intelligence Décisionnelle IA</span>
                </div>
                {loadingDecision ? (
                  <div className="h-6 w-24 bg-white/20 rounded-full animate-pulse" />
                ) : aiDecision ? (
                  <div className="flex items-center gap-2">
                    <span className="text-xl">{aiDecision.recommandationIcon}</span>
                    <span className="text-sm font-black text-white tracking-wide">{aiDecision.recommandation}</span>
                    <span className="text-[10px] text-white/60 ml-1">({Math.round(aiDecision.confidence * 100)}% conf.)</span>
                  </div>
                ) : null}
              </div>
              <div className="p-4 bg-slate-50 dark:bg-slate-900/50 space-y-3">
                {loadingDecision ? (
                  <div className="space-y-2">
                    <div className="h-3 bg-slate-200 dark:bg-slate-700 rounded animate-pulse w-3/4" />
                    <div className="h-3 bg-slate-200 dark:bg-slate-700 rounded animate-pulse w-1/2" />
                  </div>
                ) : aiDecision ? (
                  <>
                    <div>
                      <div className="flex justify-between text-[10px] font-bold text-slate-500 mb-1">
                        <span>Score de risque IA</span>
                        <span className="font-black text-slate-700 dark:text-slate-200">{aiDecision.score}/100</span>
                      </div>
                      <div className="h-2.5 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                        <div className="h-full rounded-full transition-all duration-700" style={{
                          width: `${aiDecision.score}%`,
                          background: aiDecision.score >= 85 ? '#ef4444' : aiDecision.score >= 50 ? '#f97316' : '#22c55e'
                        }} />
                      </div>
                    </div>
                    <div className="flex items-center gap-2 text-xs">
                      <span className="text-slate-400 font-semibold">Circuit conseillé :</span>
                      <span className="font-black px-2 py-0.5 rounded-md bg-indigo-100 dark:bg-indigo-900/40 text-indigo-700 dark:text-indigo-300">{aiDecision.suggestedRole}</span>
                    </div>
                    {aiDecision.anomalie && (
                      <div className={`text-[11px] font-bold px-3 py-2 rounded-xl border ${
                        aiDecision.anomalie.niveau === 'CRITIQUE'
                          ? 'bg-red-50 border-red-200 text-red-700 dark:bg-red-950/30 dark:border-red-900 dark:text-red-300'
                          : 'bg-amber-50 border-amber-200 text-amber-700 dark:bg-amber-950/30 dark:border-amber-900 dark:text-amber-300'
                      }`}>
                        {aiDecision.anomalie.niveau === 'CRITIQUE' ? '⛔' : '⚠️'} Anomalie {aiDecision.anomalie.niveau} — Z-score {aiDecision.anomalie.zScore.toFixed(2)}<br />
                        <span className="font-normal">{aiDecision.anomalie.raison}</span>
                      </div>
                    )}
                    {aiDecision.justifications?.length > 0 && (
                      <ul className="space-y-1">
                        {aiDecision.justifications.map((j: string, i: number) => (
                          <li key={i} className="text-[11px] text-slate-600 dark:text-slate-300 flex items-start gap-1.5">
                            <span className="text-indigo-400">›</span>{j}
                          </li>
                        ))}
                      </ul>
                    )}
                  </>
                ) : (
                  <p className="text-xs text-slate-400">Analyse indisponible</p>
                )}
              </div>
            </div>


            {/* Surveillance Budget pour Valideurs */}
            <div className="bg-slate-50 dark:bg-slate-900/50 rounded-2xl border border-slate-100 dark:border-slate-800 p-5">
              <div className="flex items-center gap-2 mb-4">
                <span className="w-6 h-6 rounded-lg bg-indigo-600 text-white flex items-center justify-center text-[10px] font-black">B</span>
                <h4 className="text-xs font-black text-slate-500 uppercase tracking-widest">Surveillance Budgétaire</h4>
              </div>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Sous-Famille Monitor */}
                <div className={`p-4 rounded-xl border ${((selectedDa.montantEstime || 0) > (selectedDa.budgetSousFamille?.budget_disponible || selectedDa.budgetSousFamille?.budget_restant || 0)) ? 'bg-rose-50 border-rose-200' : 'bg-emerald-50 border-emerald-200'}`}>
                  <p className="text-[10px] font-bold text-slate-400 uppercase mb-1">Sous-Famille: {selectedDa.budgetSousFamille?.name || '—'}</p>
                  <div className="flex justify-between items-end">
                    <div>
                      <p className="text-[9px] text-slate-400 uppercase">Disponible</p>
                      <p className="text-sm font-black text-slate-700">{formatCurrency(selectedDa.budgetSousFamille?.budget_disponible || selectedDa.budgetSousFamille?.budget_restant || 0)}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-[9px] text-slate-400 uppercase">Impact DA</p>
                      <p className="text-sm font-black text-slate-800">-{formatCurrency(selectedDa.montantEstime || 0)}</p>
                    </div>
                  </div>
                  <div className="mt-2 w-full h-1 bg-slate-200 rounded-full overflow-hidden">
                    <div 
                      className={`h-full transition-all duration-500 ${((selectedDa.montantEstime || 0) > (selectedDa.budgetSousFamille?.budget_disponible || selectedDa.budgetSousFamille?.budget_restant || 0)) ? 'bg-rose-500' : 'bg-emerald-500'}`} 
                      style={{ width: `${Math.min(100, ((selectedDa.montantEstime || 0) / (selectedDa.budgetSousFamille?.budget_disponible || selectedDa.budgetSousFamille?.budget_restant || 1)) * 100)}%` }}
                    />
                  </div>
                </div>

                {/* Famille Monitor */}
                <div className="p-4 rounded-xl border bg-slate-100 border-slate-200">
                  <p className="text-[10px] font-bold text-slate-400 uppercase mb-1">Famille: {selectedDa.budgetFamille?.name || '—'}</p>
                  <div className="flex justify-between items-end">
                    <div>
                      <p className="text-[9px] text-slate-400 uppercase">Restant Global</p>
                      <p className="text-sm font-black text-slate-700">{formatCurrency(selectedDa.budgetFamille?.budget_restant || 0)}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-[9px] text-slate-400 uppercase">Nouv. Solde</p>
                      <p className="text-sm font-black text-indigo-600">{formatCurrency((selectedDa.budgetFamille?.budget_restant || 0) - (selectedDa.montantEstime || 0))}</p>
                    </div>
                  </div>
                </div>
              </div>

              {(selectedDa.montantEstime || 0) > (selectedDa.budgetSousFamille?.budget_disponible || selectedDa.budgetSousFamille?.budget_restant || 0) && (
                <div className="mt-3 p-3 bg-rose-500/10 border border-rose-500/20 rounded-lg flex items-center gap-3">
                  <span className="text-xl">⚠️</span>
                  <p className="text-[11px] font-bold text-rose-700 leading-tight">
                    Attention : Cette demande dépasse le budget disponible de la sous-famille. 
                    Une validation entraînera un blocage au niveau de l'acheteur sauf ajustement.
                  </p>
                </div>
              )}
            </div>

            <div className="space-y-3">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wide">Votre décision *</p>
              <div className="flex gap-6">
                <label className="flex items-center gap-2 cursor-pointer group">
                  <input 
                    type="radio" name="decision" value="ACCEPTE" 
                    checked={decision === 'ACCEPTE'} onChange={() => setDecision('ACCEPTE')}
                    className="w-4 h-4 text-emerald-600 focus:ring-emerald-500" 
                  />
                  <span className="text-sm font-semibold text-slate-700 dark:text-slate-200 group-hover:text-emerald-600">✅ Accepter la demande</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer group">
                  <input 
                    type="radio" name="decision" value="REJETE" 
                    checked={decision === 'REJETE'} onChange={() => setDecision('REJETE')}
                    className="w-4 h-4 text-red-600 focus:ring-red-500" 
                  />
                  <span className="text-sm font-semibold text-slate-700 dark:text-slate-200 group-hover:text-red-600">❌ Refuser / Retour</span>
                </label>
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wide">
                Commentaire Décisionnel (Obligatoire)
              </label>
              <textarea
                value={comment} onChange={e => setComment(e.target.value)} rows={3}
                placeholder="Expliquez votre décision (obligatoire pour la traçabilité)..."
                className={`w-full px-4 py-2.5 rounded-xl border ${!comment.trim() ? 'border-red-300' : 'border-slate-200'} dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300 resize-none`}
              />
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <button onClick={() => { setSelectedDa(null); setComment(''); setDecision(null); }}
                className="px-6 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-200 text-sm font-medium hover:bg-slate-200 transition-colors">
                Annuler
              </button>
              <button
                onClick={() => validateMutation.mutate()}
                disabled={!decision || !comment.trim() || validateMutation.isPending}
                className={`px-8 py-2.5 rounded-xl text-white text-sm font-bold transition-all shadow-lg flex items-center gap-2
                  ${decision === 'ACCEPTE' ? 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-200' : 
                    decision === 'REJETE' ? 'bg-red-600 hover:bg-red-700 shadow-red-200' : 'bg-slate-300 cursor-not-allowed'}`}
              >
                {validateMutation.isPending ? 'Chargement...' : 'Confirmer la décision'}
              </button>
            </div>
          </div>
        </DaModal>
      )}
    </DashboardLayout>
  );
}
