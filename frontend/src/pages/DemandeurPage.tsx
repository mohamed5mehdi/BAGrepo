import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import { getMesDemandesInternes, createDemandeInterne, soumettreDemandeInterne, getFamilies, getSubFamiliesByFamily } from '../api/services';
import { formatCurrency } from '../utils/constants';
import type { DemandeAchatInterne, Family, SubFamily } from '../types';

interface DaLine {
  id: string;
  itemName: string;
  subFamilyId: string;
  quantite: number;
  prix_unitaire: number;
}

export default function DemandeurPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DemandeAchatInterne | null>(null);
  const [showNewForm, setShowNewForm] = useState(false);
  const [search, setSearch] = useState('');

  // Form Header State
  const [designation, setDesignation] = useState('');
  const [categorie, setCategorie] = useState('INFORMATIQUE');
  const [quantite, setQuantite] = useState(1);
  const [justification, setJustification] = useState('');
  const [urgency, setUrgency] = useState('NORMALE');
  const [budgetFamilleId, setBudgetFamilleId] = useState<string>('');
  const [budgetSousFamilleId, setBudgetSousFamilleId] = useState<string>('');

  // Data Fetching
  const { data: daList = [], isLoading } = useQuery({
    queryKey: ['da', 'mes-demandes', user?.userId],
    queryFn: () => getMesDemandesInternes(user!.userId).then(r => r.data),
    enabled: !!user,
  });

  const { data: families = [] } = useQuery({
    queryKey: ['budget-families'],
    queryFn: () => getFamilies().then(r => r.data),
  });

  const { data: subFamilies = [] } = useQuery<SubFamily[]>({
    queryKey: ['sub-families', budgetFamilleId],
    queryFn: () => budgetFamilleId ? getSubFamiliesByFamily(Number(budgetFamilleId)).then(r => r.data) : Promise.resolve([]),
    enabled: !!budgetFamilleId
  });

  const createMutation = useMutation({
    mutationFn: (payload: any) => createDemandeInterne(payload, user!.userId),
    onSuccess: () => {
      toast.success('Demande d\'achat interne créée !');
      qc.invalidateQueries({ queryKey: ['da', 'mes-demandes'] });
      setShowNewForm(false);
      resetForm();
    },
    onError: (err: any) => {
      toast.error('Erreur lors de la création de la demande');
    },
  });

  const resetForm = () => {
    setDesignation('');
    setCategorie('INFORMATIQUE');
    setQuantite(1);
    setJustification('');
    setUrgency('NORMALE');
    setBudgetFamilleId('');
    setBudgetSousFamilleId('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    const payload = {
      designation,
      categorie,
      quantite,
      justification,
      urgence: urgency,
      budgetFamille: { id: Number(budgetFamilleId) },
      budgetSousFamille: { id: Number(budgetSousFamilleId) }
    };

    createMutation.mutate(payload);
  };

  const kpis = {
    total:   daList.length,
    pending: daList.filter((d: DemandeAchatInterne) => !['PO_CREE', 'REJETEE', 'APPROUVEE'].includes(d.statut)).length,
    valid:   daList.filter((d: DemandeAchatInterne) => d.statut === 'APPROUVEE' || d.statut === 'PO_CREE' || d.statut === 'EN_LIVRAISON').length,
    rejected:daList.filter((d: DemandeAchatInterne) => d.statut === 'REJETEE').length,
  };



  return (
    <DashboardLayout title="Espace Demandeur — Gestion des Achats BAG" pendingCount={kpis.pending}>
      {/* KPI Section */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <KpiCard label="Mes Demandes" value={kpis.total} icon="📦" color="from-blue-600 to-indigo-700" />
        <KpiCard label="En Circuit" value={kpis.pending} icon="🔄" color="from-amber-500 to-orange-600" />
        <KpiCard label="Validées / PO" value={kpis.valid} icon="✅" color="from-emerald-500 to-teal-600" />
        <KpiCard label="Refusées" value={kpis.rejected} icon="❌" color="from-rose-500 to-red-700" />
      </div>

      {/* Action Header */}
      <div className="bg-white dark:bg-slate-800 p-4 rounded-2xl border border-slate-100 dark:border-slate-700 shadow-sm mb-6 flex flex-wrap items-center gap-4">
        <div className="relative flex-1 min-w-[250px]">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">🔍</span>
          <input
            value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Rechercher une demande (Code, Objet...)"
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
          />
        </div>
        <button
          onClick={() => setShowNewForm(true)}
          className="px-6 py-2.5 rounded-xl bg-blue-600 text-white font-bold text-sm hover:bg-blue-700 transition-all shadow-lg shadow-blue-200 dark:shadow-none flex items-center gap-2"
        >
          <span>➕</span> Nouvelle Demande d'Achat
        </button>
      </div>

      {/* Main Table */}
      <DaTable
        rows={daList}
        onRowClick={setSelectedDa}
        showRequester={false}
        loading={isLoading}
        searchQuery={search}
        actionLabel={() => 'Voir Détails'}
        showPrice={false}
      />

      {/* Detail Modal */}
      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="Détails de la Demande" wide showPrice={false}>
          <div className="flex justify-end pt-4 border-t border-slate-100 dark:border-slate-800">
            <button 
              onClick={() => setSelectedDa(null)}
              className="px-6 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-semibold text-sm hover:bg-slate-200"
            >
              Fermer
            </button>
          </div>
        </DaModal>
      )}

      {/* Create DA Form Modal */}
      {showNewForm && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[60] flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-4xl max-h-[95vh] flex flex-col animate-scale-in">
            <div className="px-8 py-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-800 dark:text-white">Nouvelle Demande de Besoins Internes</h2>
                <p className="text-xs text-slate-400 mt-1">Équipements informatiques, mobilier, fournitures ou maintenance pour votre service.</p>
              </div>
              <button onClick={() => setShowNewForm(false)} className="p-2 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full text-slate-400">✕</button>
            </div>

            <form id="da-form" onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-8 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 uppercase ml-1">Catégorie *</label>
                  <select 
                    value={categorie} onChange={e => setCategorie(e.target.value)} required
                    className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                  >
                    <option value="INFORMATIQUE">💻 Informatique</option>
                    <option value="BUREAUTIQUE">📄 Bureautique</option>
                    <option value="MOBILIER">🪑 Mobilier</option>
                    <option value="CONSOMMABLE">🖊️ Consommable</option>
                    <option value="AUTRE">📦 Autre</option>
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 uppercase ml-1">Urgence</label>
                  <select 
                    value={urgency} onChange={e => setUrgency(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                  >
                    <option value="NORMALE">🟢 Normale</option>
                    <option value="URGENTE">🟠 Urgente</option>
                    <option value="CRITIQUE">🔴 Critique</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="md:col-span-2 space-y-1">
                  <label className="text-xs font-bold text-slate-500 uppercase ml-1">Désignation de l'article *</label>
                  <input 
                    value={designation} onChange={e => setDesignation(e.target.value)} required
                    placeholder="Ex: Écran Dell 24 pouces"
                    className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 uppercase ml-1">Quantité *</label>
                  <input 
                    type="number" min={1} value={quantite} onChange={e => setQuantite(Number(e.target.value))} required
                    className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 uppercase ml-1">Famille Budgétaire *</label>
                  <select 
                    value={budgetFamilleId} onChange={e => setBudgetFamilleId(e.target.value)} required
                    className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                  >
                    <option value="">Sélectionner...</option>
                    {families.map((f: any) => (
                      <option key={f.id} value={f.id}>
                        {f.name || f.libelle} — Restant: {formatCurrency(f.budget_restant || 0)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-bold text-slate-500 uppercase ml-1">Sous-Famille *</label>
                  <select 
                    value={budgetSousFamilleId} onChange={e => setBudgetSousFamilleId(e.target.value)} required
                    className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm focus:ring-2 focus:ring-blue-500 outline-none transition-all"
                  >
                    <option value="">Sélectionner...</option>
                    {subFamilies.map((sf: any) => (
                      <option key={sf.id} value={sf.id}>
                        {sf.name || sf.libelle} — Restant: {formatCurrency(sf.budget_restant || 0)}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase ml-1">Justification Métier *</label>
                <textarea 
                  value={justification} onChange={e => setJustification(e.target.value)} rows={3} required
                  placeholder="Expliquez pourquoi ce besoin est essentiel..."
                  className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm focus:ring-2 focus:ring-blue-500 outline-none transition-all resize-none"
                />
              </div>
            </form>

            {/* Footer Summary & Actions */}
            <div className="p-8 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-800/50 rounded-b-3xl">
              <div>
                <p className="text-xs text-slate-400 font-bold uppercase tracking-wider">Récapitulatif</p>
                <p className="text-sm font-medium text-slate-600 dark:text-slate-400">
                  {quantite} x {designation || 'Article'} en cours de demande.
                </p>
              </div>
              <div className="flex gap-4">
                <button 
                  type="button" onClick={() => setShowNewForm(false)}
                  className="px-6 py-3 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 font-bold text-sm hover:bg-slate-50 transition-all"
                >
                  Annuler
                </button>
                <button 
                  type="submit" form="da-form" disabled={createMutation.isPending}
                  className="px-8 py-3 rounded-2xl bg-blue-600 text-white font-black text-sm hover:bg-blue-700 disabled:opacity-50 shadow-xl shadow-blue-200 dark:shadow-none transition-all flex items-center gap-2"
                >
                  {createMutation.isPending ? (
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : '🚀 Créer ma Demande'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
