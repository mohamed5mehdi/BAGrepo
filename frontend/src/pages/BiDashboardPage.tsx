import React, { useState, useMemo } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import { useAuth } from '../context/AuthContext';
import { executeBiQuery, fetchBiOverview } from '../api/services';
import {
  BarChart, Bar,
  LineChart, Line,
  PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

interface VisualizationConfig {
  type: string;
  title: string;
  xKey: string;
  yKey: string;
}

interface BiResponse {
  data: Record<string, any>[];
  visualizations: VisualizationConfig[];
}

interface BudgetFamilleDto {
  categorie: string;
  libelle: string;
  budgetInitial: number;
  budgetRestant: number;
  tauxConsommation: number;
}

interface BiOverviewDto {
  totalDemandes: number;
  pourcentageApprouvees: number;
  pourcentageRejetees: number;
  pourcentagePoCree: number;
  pourcentageAffectee: number;
  enAttenteValidation: number;
  budgetsParFamille: BudgetFamilleDto[];
}

const COLORS = ['#6366f1', '#14b8a6', '#f59e0b', '#ec4899', '#8b5cf6', '#0ea5e9', '#10b981', '#f43f5e', '#f97316', '#84cc16', '#06b6d4', '#d946ef'];

export default function BiDashboardPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'overview' | 'assistant'>('overview');
  const [question, setQuestion] = useState('');
  
  const overviewQuery = useQuery({
    queryKey: ['biOverview'],
    queryFn: () => fetchBiOverview().then(res => res.data as BiOverviewDto)
  });

  const mutation = useMutation({
    mutationFn: (q: string) => executeBiQuery(user!.userId, q),
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erreur lors de l\'exécution de la requête BI');
    }
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;
    mutation.mutate(question);
  };

  const responsePayload = mutation.data?.data as BiResponse | undefined;
  const data = responsePayload?.data;
  const visualizations = responsePayload?.visualizations || [];

  const headers = useMemo(() => {
    if (!data || data.length === 0) return [];
    return Object.keys(data[0]);
  }, [data]);

  const renderOverview = () => {
    if (overviewQuery.isLoading) {
      return (
        <div className="flex justify-center p-12">
          <div className="animate-spin w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full" />
        </div>
      );
    }
    if (overviewQuery.isError) {
      return (
        <div className="text-rose-600 p-6 bg-rose-50 border border-rose-200 rounded-xl">
          Erreur de chargement de la vue d'ensemble.
        </div>
      );
    }
    
    const oData = overviewQuery.data;
    if (!oData) return null;

    return (
      <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
        {/* Ligne 1 : KPI principaux */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 flex flex-col justify-between">
            <h3 className="text-slate-500 text-sm font-medium">Total Demandes</h3>
            <div className="mt-4">
              <p className="text-4xl font-bold text-slate-800">{oData.totalDemandes}</p>
              <p className="text-xs text-slate-400 mt-1">Toutes catégories</p>
            </div>
          </div>

          <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 flex flex-col justify-between">
            <h3 className="text-slate-500 text-sm font-medium">En Attente de Validation</h3>
            <div className="mt-4">
              <p className="text-4xl font-bold text-slate-800">{oData.enAttenteValidation}</p>
              <p className="text-xs text-slate-400 mt-1">N1, Tech, DG, DAF</p>
            </div>
          </div>

          <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 flex flex-col justify-between">
            <h3 className="text-slate-500 text-sm font-medium">Demandes Approuvées</h3>
            <div className="mt-4">
              <p className="text-4xl font-bold text-emerald-600">{oData.pourcentageApprouvees.toFixed(1)}%</p>
              <p className="text-xs text-slate-400 mt-1">Sur les demandes finalisées</p>
            </div>
          </div>

          <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 flex flex-col justify-between">
            <h3 className="text-slate-500 text-sm font-medium">Demandes Rejetées</h3>
            <div className="mt-4">
              <p className="text-4xl font-bold text-rose-600">{oData.pourcentageRejetees.toFixed(1)}%</p>
              <p className="text-xs text-slate-400 mt-1">Sur les demandes finalisées</p>
            </div>
          </div>
        </div>
        
        {/* Ligne 2 : Budgets et autres statuts */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
           <div className="lg:col-span-2 bg-white rounded-xl p-6 shadow-sm border border-slate-200">
             <h3 className="text-lg font-bold text-slate-800 mb-6">État des Budgets par Famille</h3>
             <div className="space-y-6">
                {oData.budgetsParFamille.map((fam) => (
                  <div key={fam.categorie}>
                    <div className="flex justify-between items-end mb-2">
                      <div>
                        <span className="font-semibold text-slate-700">{fam.categorie}</span>
                        <span className="text-slate-400 text-sm ml-2">({fam.libelle})</span>
                      </div>
                      <span className="text-sm font-medium text-slate-600">{fam.tauxConsommation.toFixed(1)}% consommé</span>
                    </div>
                    <div className="w-full bg-slate-100 rounded-full h-3">
                      <div 
                        className={`h-3 rounded-full transition-all duration-1000 ${fam.tauxConsommation > 90 ? 'bg-rose-500' : fam.tauxConsommation > 75 ? 'bg-amber-500' : 'bg-emerald-500'}`} 
                        style={{ width: `${Math.min(fam.tauxConsommation, 100)}%` }}
                      ></div>
                    </div>
                    <div className="flex justify-between text-xs text-slate-500 mt-2">
                       <span>Restant: {new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'MAD' }).format(fam.budgetRestant)}</span>
                       <span>Initial: {new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'MAD' }).format(fam.budgetInitial)}</span>
                    </div>
                  </div>
                ))}
                {oData.budgetsParFamille.length === 0 && (
                  <p className="text-slate-500 text-sm">Aucune donnée de budget disponible.</p>
                )}
             </div>
           </div>
           
           <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 flex flex-col gap-6">
             <h3 className="text-lg font-bold text-slate-800">Autres Statuts</h3>
             <div className="bg-slate-50 rounded-lg p-5 border border-slate-100">
                <h4 className="text-sm font-medium text-slate-500">Transformées en PO</h4>
                <p className="text-3xl font-bold text-indigo-600 mt-2">{oData.pourcentagePoCree.toFixed(1)}%</p>
                <p className="text-xs text-slate-400 mt-1">Sur les demandes finalisées</p>
             </div>
             <div className="bg-slate-50 rounded-lg p-5 border border-slate-100">
                <h4 className="text-sm font-medium text-slate-500">Affectées</h4>
                <p className="text-3xl font-bold text-amber-600 mt-2">{oData.pourcentageAffectee.toFixed(1)}%</p>
                <p className="text-xs text-slate-400 mt-1">Sur les demandes finalisées</p>
             </div>
           </div>
        </div>
      </div>
    );
  };

  const renderVisualization = (viz: VisualizationConfig, index: number) => {
    if (!data || data.length === 0) return null;
    const availableKeys = Object.keys(data[0]);
    if (!availableKeys.includes(viz.xKey) || !availableKeys.includes(viz.yKey)) {
      return (
        <div key={index} className="bg-white border border-rose-200 rounded-xl p-6 shadow-sm flex items-center justify-center flex-col h-[400px]">
          <span className="text-rose-500 text-3xl mb-2">⚠</span>
          <p className="text-slate-700 text-center font-medium">Le graphique "{viz.title}" est impossible à tracer.</p>
          <p className="text-slate-500 text-sm text-center mt-2">Désynchronisation IA : les colonnes demandées ({viz.xKey}, {viz.yKey}) sont absentes des données réelles.</p>
        </div>
      );
    }

    const commonProps = {
      data,
      margin: { top: 20, right: 30, left: 20, bottom: 60 }
    };

    let chartComponent;

    switch (viz.type) {
      case 'BarChart':
        chartComponent = (
          <BarChart {...commonProps}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
            <XAxis dataKey={viz.xKey} stroke="#64748b" angle={-45} textAnchor="end" height={80} tick={{ fill: '#475569', fontSize: 12 }} />
            <YAxis stroke="#64748b" tick={{ fill: '#475569', fontSize: 12 }} />
            <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '0.5rem', color: '#0f172a', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
            <Legend wrapperStyle={{ paddingTop: '20px' }} />
            <Bar dataKey={viz.yKey} fill={COLORS[0]} radius={[4, 4, 0, 0]} maxBarSize={60} />
          </BarChart>
        );
        break;
      case 'LineChart':
        chartComponent = (
          <LineChart {...commonProps}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
            <XAxis dataKey={viz.xKey} stroke="#64748b" angle={-45} textAnchor="end" height={80} tick={{ fill: '#475569', fontSize: 12 }} />
            <YAxis stroke="#64748b" tick={{ fill: '#475569', fontSize: 12 }} />
            <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '0.5rem', color: '#0f172a', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
            <Legend wrapperStyle={{ paddingTop: '20px' }} />
            <Line type="monotone" dataKey={viz.yKey} stroke={COLORS[1]} strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
          </LineChart>
        );
        break;
      case 'PieChart':
        chartComponent = (
          <PieChart margin={{ top: 20, right: 30, left: 20, bottom: 20 }}>
            <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '0.5rem', color: '#0f172a', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
            <Legend />
            <Pie data={data} nameKey={viz.xKey} dataKey={viz.yKey} cx="50%" cy="50%" innerRadius={60} outerRadius={100} paddingAngle={5} label>
              {data.map((_, i) => (
                <Cell key={`cell-${i}`} fill={COLORS[i % COLORS.length]} />
              ))}
            </Pie>
          </PieChart>
        );
        break;
      default:
        return null;
    }

    return (
      <div key={index} className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
        <h3 className="text-lg font-bold text-slate-800 mb-6">{viz.title || 'Visualisation'}</h3>
        <div className="h-[400px]">
          <ResponsiveContainer width="100%" height="100%">
            {chartComponent}
          </ResponsiveContainer>
        </div>
      </div>
    );
  };

  const renderAssistant = () => {
    return (
      <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
          <form onSubmit={handleSubmit} className="flex gap-4">
            <input
              type="text"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="Ex: Montre-moi la répartition des achats par famille..."
              className="flex-1 bg-slate-50 border border-slate-300 rounded-lg px-4 py-3 text-slate-800 placeholder-slate-400 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
              disabled={mutation.isPending}
            />
            <button
              type="submit"
              disabled={mutation.isPending || !question.trim()}
              className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 disabled:text-slate-500 text-white font-medium rounded-lg shadow-sm transition-all flex items-center justify-center gap-2 min-w-[140px]"
            >
              {mutation.isPending ? (
                <>
                  <div className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                  <span>Analyse...</span>
                </>
              ) : (
                <span>Générer</span>
              )}
            </button>
          </form>
        </div>

        {data && data.length > 0 && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            {visualizations.map((viz, idx) => renderVisualization(viz, idx))}
            
            <div className={`bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden flex flex-col lg:col-span-2`}>
              <div className="p-6 border-b border-slate-200 bg-slate-50">
                <h3 className="text-lg font-bold text-slate-800">Données Détaillées</h3>
              </div>
              <div className="overflow-x-auto flex-1 p-6">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr>
                      {headers.map(h => (
                        <th key={h} className="pb-4 px-4 text-xs font-semibold text-slate-500 border-b border-slate-200 uppercase tracking-wider">
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {data.map((row, i) => (
                      <tr key={i} className="hover:bg-slate-50 transition-colors">
                        {headers.map(h => (
                          <td key={`${i}-${h}`} className="py-4 px-4 text-sm text-slate-700">
                            {typeof row[h] === 'number' ? new Intl.NumberFormat('fr-FR').format(row[h]) : row[h]}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {data && data.length === 0 && (
          <div className="bg-white border border-slate-200 rounded-xl p-12 text-center shadow-sm">
            <h3 className="text-lg font-medium text-slate-800">Aucun résultat</h3>
            <p className="text-slate-500 mt-2">La requête SQL générée n'a retourné aucune ligne.</p>
          </div>
        )}
      </div>
    );
  };

  return (
    <DashboardLayout title="Intelligence d'Affaires">
      <div className="space-y-6 max-w-7xl mx-auto">
        
        {/* En-tête et Tabs */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-slate-200 pb-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Intelligence d'Affaires (BI)</h1>
            <p className="text-sm text-slate-500 mt-1">Supervisez et interrogez vos données d'achats</p>
          </div>
          <div className="flex bg-slate-100 p-1 rounded-lg">
            <button
              onClick={() => setActiveTab('overview')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-all ${
                activeTab === 'overview' 
                  ? 'bg-white text-indigo-600 shadow-sm' 
                  : 'text-slate-600 hover:text-slate-800 hover:bg-slate-200/50'
              }`}
            >
              Vue d'ensemble
            </button>
            <button
              onClick={() => setActiveTab('assistant')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-all ${
                activeTab === 'assistant' 
                  ? 'bg-white text-indigo-600 shadow-sm' 
                  : 'text-slate-600 hover:text-slate-800 hover:bg-slate-200/50'
              }`}
            >
              Assistant IA
            </button>
          </div>
        </div>

        {/* Contenu de l'onglet actif */}
        {activeTab === 'overview' ? renderOverview() : renderAssistant()}
        
      </div>
    </DashboardLayout>
  );
}
