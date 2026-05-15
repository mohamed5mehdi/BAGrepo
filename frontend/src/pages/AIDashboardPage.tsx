import React from 'react';
import { useQuery } from '@tanstack/react-query';
import DashboardLayout from '../components/DashboardLayout';
import {
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend
} from 'recharts';
import { getAIDashboard, getAIAnomalies, getAIInsights, getAIDelays } from '../api/ai-services';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmt = (n: number) =>
  new Intl.NumberFormat('fr-MA', { style: 'decimal', maximumFractionDigits: 0 }).format(n) + ' MAD';

const MOIS = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];

const COLORS = ['#6366f1', '#22d3ee', '#f59e0b', '#10b981', '#f43f5e', '#a78bfa'];

const PRIORITY_META: Record<string, { bg: string; border: string; badge: string }> = {
  HAUTE:   { bg: 'bg-red-50 dark:bg-red-950/30',    border: 'border-red-200 dark:border-red-900',    badge: 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300' },
  MOYENNE: { bg: 'bg-amber-50 dark:bg-amber-950/30', border: 'border-amber-200 dark:border-amber-900', badge: 'bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-300' },
  INFO:    { bg: 'bg-blue-50 dark:bg-blue-950/30',   border: 'border-blue-200 dark:border-blue-900',   badge: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300' },
};

// ─── Sub-components ───────────────────────────────────────────────────────────

function KpiTile({ label, value, sub, icon, gradient }: {
  label: string; value: string | number; sub?: string; icon: string; gradient: string;
}) {
  return (
    <div className={`rounded-2xl p-5 text-white shadow-lg ${gradient} relative overflow-hidden`}>
      <div className="absolute right-4 top-4 text-3xl opacity-20 select-none">{icon}</div>
      <p className="text-xs font-bold uppercase tracking-widest opacity-80 mb-1">{label}</p>
      <p className="text-3xl font-black">{value}</p>
      {sub && <p className="text-xs opacity-70 mt-1">{sub}</p>}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-white dark:bg-slate-800 rounded-2xl border border-slate-100 dark:border-slate-700 shadow-sm p-6">
      <h2 className="text-sm font-bold text-slate-800 dark:text-white mb-4 flex items-center gap-2">
        {title}
      </h2>
      {children}
    </div>
  );
}

function SkeletonBlock({ h = 'h-48' }: { h?: string }) {
  return <div className={`${h} bg-slate-100 dark:bg-slate-700 rounded-xl animate-pulse`} />;
}

// ─── Custom Tooltip ───────────────────────────────────────────────────────────

function CustomTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-slate-900 text-white text-xs rounded-xl px-3 py-2 shadow-xl">
      <p className="font-bold mb-1">{label}</p>
      {payload.map((p: any, i: number) => (
        <p key={i} style={{ color: p.color }}>{p.name}: {fmt(p.value)}</p>
      ))}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function AIDashboardPage() {
  const { data: dashboard, isLoading: loadingDash } = useQuery({
    queryKey: ['ai', 'dashboard'],
    queryFn: () => getAIDashboard().then(r => r.data),
    staleTime: 60_000,
  });

  const { data: anomalies = [], isLoading: loadingAnomaly } = useQuery({
    queryKey: ['ai', 'anomalies'],
    queryFn: () => getAIAnomalies().then(r => r.data),
    staleTime: 60_000,
  });

  const { data: insights, isLoading: loadingInsights } = useQuery({
    queryKey: ['ai', 'insights'],
    queryFn: () => getAIInsights().then(r => r.data),
    staleTime: 60_000,
  });

  const { data: delays, isLoading: loadingDelays } = useQuery({
    queryKey: ['ai', 'delays'],
    queryFn: () => getAIDelays().then(r => r.data),
    staleTime: 60_000,
  });

  const kpi = dashboard?.kpi;
  const depensesCategorie: any[] = dashboard?.depensesCategorie ?? [];
  const evolution: any[] = dashboard?.evolutionMensuelle ?? [];
  const depensesDept: any[] = dashboard?.depensesDepartement ?? [];
  const budgets: any[] = dashboard?.consommationBudget ?? [];

  // Formatage axe X évolution mensuelle
  const evolutionFormatted = evolution.map(e => ({
    ...e,
    label: `${MOIS[(e.mois ?? 1) - 1]} ${String(e.annee ?? '').slice(2)}`,
  }));

  return (
    <DashboardLayout title="Tableau de Bord IA — Intelligence Décisionnelle BAG">

      {/* ── KPI Strip ─────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        {loadingDash ? (
          Array.from({ length: 4 }).map((_, i) => <SkeletonBlock key={i} h="h-28" />)
        ) : (
          <>
            <KpiTile
              label="Demandes Totales"
              value={kpi?.totalDAs ?? '—'}
              sub="Toutes catégories"
              icon="📦"
              gradient="bg-gradient-to-br from-indigo-600 to-violet-700"
            />
            <KpiTile
              label="Montant Engagé"
              value={kpi ? fmt(Number(kpi.montantTotalEngage)) : '—'}
              sub="DAs approuvées + PO"
              icon="💰"
              gradient="bg-gradient-to-br from-emerald-500 to-teal-700"
            />
            <KpiTile
              label="En Circuit"
              value={kpi?.daEnAttente ?? '—'}
              sub={kpi ? `${kpi.daApprouvees} validées` : ''}
              icon="🔄"
              gradient="bg-gradient-to-br from-amber-500 to-orange-600"
            />
            <KpiTile
              label="Taux Approbation"
              value={kpi ? `${kpi.tauxApprobation}%` : '—'}
              sub={`${kpi?.daRejetees ?? 0} refusées`}
              icon="✅"
              gradient="bg-gradient-to-br from-sky-500 to-blue-700"
            />
          </>
        )}
      </div>

      {/* ── Row 1 : BarChart catégories + LineChart évolution ─────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <Section title="📊 Dépenses par Catégorie (MAD)">
          {loadingDash ? <SkeletonBlock /> : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={depensesCategorie} margin={{ left: 10, right: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="categorie" tick={{ fontSize: 11 }} />
                <YAxis tickFormatter={v => `${(v / 1000).toFixed(0)}K`} tick={{ fontSize: 10 }} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="montant" name="Montant" radius={[6, 6, 0, 0]}>
                  {depensesCategorie.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </Section>

        <Section title="📈 Évolution Mensuelle des Achats">
          {loadingDash ? <SkeletonBlock /> : (
            <ResponsiveContainer width="100%" height={240}>
              <LineChart data={evolutionFormatted} margin={{ left: 10, right: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis tickFormatter={v => `${(v / 1000).toFixed(0)}K`} tick={{ fontSize: 10 }} />
                <Tooltip content={<CustomTooltip />} />
                <Line
                  type="monotone" dataKey="montant" name="Montant"
                  stroke="#6366f1" strokeWidth={2.5} dot={{ r: 4, fill: '#6366f1' }}
                  activeDot={{ r: 6 }}
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </Section>
      </div>

      {/* ── Row 2 : Pie départements + Budget consommation ────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <Section title="🏢 Dépenses par Département">
          {loadingDash ? <SkeletonBlock /> : (
            <div className="flex items-center gap-4">
              <ResponsiveContainer width="55%" height={220}>
                <PieChart>
                  <Pie
                    data={depensesDept} dataKey="montant" nameKey="departement"
                    cx="50%" cy="50%" outerRadius={80} innerRadius={40}
                    paddingAngle={3}
                  >
                    {depensesDept.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v: any) => fmt(Number(v))} />
                </PieChart>
              </ResponsiveContainer>
              <ul className="flex-1 space-y-2">
                {depensesDept.map((d: any, i) => (
                  <li key={i} className="flex items-center justify-between text-xs">
                    <span className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full" style={{ background: COLORS[i % COLORS.length] }} />
                      <span className="text-slate-600 dark:text-slate-300 font-medium truncate max-w-[120px]">
                        {d.departement}
                      </span>
                    </span>
                    <span className="font-bold text-slate-800 dark:text-white ml-2">{fmt(d.montant)}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </Section>

        <Section title="💼 Consommation Budgétaire par Famille">
          {loadingDash ? <SkeletonBlock /> : (
            <div className="space-y-4">
              {budgets.map((b: any, i: number) => {
                const taux = Math.min(Math.round(b.tauxConsommation), 100);
                const color = taux >= 90 ? 'bg-red-500' : taux >= 70 ? 'bg-amber-500' : 'bg-emerald-500';
                return (
                  <div key={i}>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="font-semibold text-slate-700 dark:text-slate-200">{b.familleLibelle}</span>
                      <span className={`font-black ${taux >= 90 ? 'text-red-600' : taux >= 70 ? 'text-amber-600' : 'text-emerald-600'}`}>
                        {taux}%
                      </span>
                    </div>
                    <div className="h-2 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-700 ${color}`}
                        style={{ width: `${taux}%` }}
                      />
                    </div>
                    <p className="text-[10px] text-slate-400 mt-0.5">
                      {fmt(b.budgetEngage)} engagés / {fmt(b.budgetInitial)} initial
                    </p>
                  </div>
                );
              })}
            </div>
          )}
        </Section>
      </div>

      {/* ── Row 3 : Insights IA ───────────────────────────────────────── */}
      <div className="mb-6">
        <Section title="🤖 Insights IA — Recommandations Décisionnelles">
          {loadingInsights ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {Array.from({ length: 3 }).map((_, i) => <SkeletonBlock key={i} h="h-28" />)}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {(insights?.insights ?? []).map((ins: any, i: number) => {
                const meta = PRIORITY_META[ins.priorite] ?? PRIORITY_META.INFO;
                return (
                  <div key={i} className={`rounded-xl border p-4 ${meta.bg} ${meta.border}`}>
                    <div className="flex items-start justify-between mb-2">
                      <span className="text-xl">{ins.icone}</span>
                      <span className={`text-[10px] font-black px-2 py-0.5 rounded-full ${meta.badge}`}>
                        {ins.priorite}
                      </span>
                    </div>
                    <p className="text-sm font-bold text-slate-800 dark:text-slate-100 mb-1 leading-tight">
                      {ins.titre}
                    </p>
                    <p className="text-[11px] text-slate-600 dark:text-slate-400 leading-relaxed mb-2">
                      {ins.description}
                    </p>
                    <div className="border-t border-current border-opacity-10 pt-2">
                      <p className="text-[11px] font-bold text-slate-700 dark:text-slate-300">
                        → {ins.actionSuggeree}
                      </p>
                    </div>
                  </div>
                );
              })}
              {(insights?.insights ?? []).length === 0 && (
                <div className="col-span-full text-center py-8 text-slate-400 text-sm">
                  ✅ Aucune anomalie ni alerte — situation nominale.
                </div>
              )}
            </div>
          )}
        </Section>
      </div>

      {/* ── Row 4 : Anomalies + Délais ────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">

        {/* Anomalies */}
        <Section title="⚠️ Anomalies Financières Détectées (Z-Score)">
          {loadingAnomaly ? <SkeletonBlock h="h-64" /> : (
            <div className="overflow-auto max-h-72">
              {anomalies.length === 0 ? (
                <p className="text-sm text-slate-400 text-center py-8">✅ Aucune anomalie détectée.</p>
              ) : (
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-100 dark:border-slate-700">
                      <th className="pb-2 font-bold text-slate-400 uppercase pr-3">DA</th>
                      <th className="pb-2 font-bold text-slate-400 uppercase pr-3">Article</th>
                      <th className="pb-2 font-bold text-slate-400 uppercase pr-3">Montant</th>
                      <th className="pb-2 font-bold text-slate-400 uppercase pr-3">Score</th>
                      <th className="pb-2 font-bold text-slate-400 uppercase">Niveau</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                    {anomalies.slice(0, 8).map((a: any, i: number) => (
                      <tr key={i} className="hover:bg-slate-50 dark:hover:bg-slate-700/30 transition-colors">
                        <td className="py-2 pr-3 font-black text-indigo-600 dark:text-indigo-400">#{a.daId}</td>
                        <td className="py-2 pr-3 text-slate-700 dark:text-slate-300 max-w-[120px] truncate">
                          {a.designation}
                        </td>
                        <td className="py-2 pr-3 font-semibold">{fmt(a.montant)}</td>
                        <td className="py-2 pr-3">
                          <div className="flex items-center gap-1.5">
                            <div className="w-12 h-1.5 bg-slate-100 dark:bg-slate-700 rounded-full">
                              <div
                                className={`h-full rounded-full ${a.score >= 80 ? 'bg-red-500' : 'bg-amber-500'}`}
                                style={{ width: `${a.score}%` }}
                              />
                            </div>
                            <span className="font-black">{a.score}</span>
                          </div>
                        </td>
                        <td className="py-2">
                          <span className={`px-2 py-0.5 rounded-full text-[10px] font-black ${
                            a.niveau === 'CRITIQUE'
                              ? 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300'
                              : 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300'
                          }`}>
                            {a.niveau}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </Section>

        {/* Délais Prédictifs */}
        <Section title="⏱️ Prédiction Délais de Validation par Rôle">
          {loadingDelays ? <SkeletonBlock h="h-64" /> : (
            <div className="space-y-3">
              {(delays?.parRole ?? []).map((p: any, i: number) => {
                const chargeColor = p.niveauCharge === 'ELEVE'
                  ? 'text-red-600 bg-red-50 dark:bg-red-950/30'
                  : p.niveauCharge === 'MOYEN'
                  ? 'text-amber-600 bg-amber-50 dark:bg-amber-950/30'
                  : 'text-emerald-600 bg-emerald-50 dark:bg-emerald-950/30';
                const heures = Math.round(p.heuresPredites);
                const jours = (heures / 24).toFixed(1);
                return (
                  <div key={i} className="flex items-center gap-4 p-3 rounded-xl bg-slate-50 dark:bg-slate-700/30">
                    <div className="flex-1">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm font-bold text-slate-800 dark:text-white">{p.role}</span>
                        <span className={`text-[10px] font-black px-2 py-0.5 rounded-full ${chargeColor}`}>
                          {p.niveauCharge}
                        </span>
                      </div>
                      <div className="flex items-center gap-3">
                        <div className="flex-1 h-2 bg-slate-200 dark:bg-slate-600 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full ${
                              p.niveauCharge === 'ELEVE' ? 'bg-red-500'
                              : p.niveauCharge === 'MOYEN' ? 'bg-amber-500'
                              : 'bg-emerald-500'
                            }`}
                            style={{ width: `${Math.min((heures / 200) * 100, 100)}%` }}
                          />
                        </div>
                        <span className="text-xs font-black text-slate-700 dark:text-slate-200 whitespace-nowrap">
                          ~{heures}h ({jours}j)
                        </span>
                      </div>
                      <p className="text-[10px] text-slate-400 mt-1">
                        {p.dasEnAttente} DA(s) en attente • moy. historique: {Math.round(p.moyenneHeuresHistorique)}h
                      </p>
                    </div>
                  </div>
                );
              })}
              {delays?.bottleneck && delays.bottleneck !== 'N/A' && (
                <div className="mt-2 p-3 rounded-xl bg-red-50 dark:bg-red-950/20 border border-red-100 dark:border-red-900/30">
                  <p className="text-xs font-bold text-red-700 dark:text-red-400">
                    🎯 Goulot détecté : <span className="font-black">{delays.bottleneck}</span>
                    {' '}— délai total circuit estimé : {Math.round(delays.delaiTotalEstime)}h
                  </p>
                </div>
              )}
            </div>
          )}
        </Section>
      </div>

    </DashboardLayout>
  );
}
