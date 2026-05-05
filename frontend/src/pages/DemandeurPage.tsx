import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import { getMesDemandesInternes, createDemandeInterne, getFamilies, getSubFamiliesByFamily, downloadPOByDA } from '../api/services';
import { formatCurrency } from '../utils/constants';
import type { DemandeAchatInterne, SubFamily, DaDetails } from '../types';

interface ItemRow {
  id: string;
  itemName: string;
  description: string;
  quantite: number;
  justification: string;
}

export default function DemandeurPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DemandeAchatInterne | null>(null);
  const [showNewForm, setShowNewForm] = useState(false);
  const [search, setSearch] = useState('');

  // Form Global State
  const [urgency, setUrgency] = useState('NORMALE');
  const [budgetFamilleId, setBudgetFamilleId] = useState<string>('');
  const [budgetSousFamilleId, setBudgetSousFamilleId] = useState<string>('');
  const [globalJustification, setGlobalJustification] = useState('');

  // Form Items State (Multi-items logic)
  const [items, setItems] = useState<ItemRow[]>([
    { id: crypto.randomUUID(), itemName: '', description: '', quantite: 1, justification: '' }
  ]);
  const [submissionToken, setSubmissionToken] = useState(crypto.randomUUID());

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
      toast.success('Demande d\'achat créée avec succès !');
      qc.invalidateQueries({ queryKey: ['da', 'mes-demandes'] });
      setShowNewForm(false);
      resetForm();
    },
    onError: () => {
      toast.error('Erreur lors de la création de la demande');
    },
  });

  const resetForm = () => {
    setUrgency('NORMALE');
    setBudgetFamilleId('');
    setBudgetSousFamilleId('');
    setGlobalJustification('');
    setItems([{ id: crypto.randomUUID(), itemName: '', description: '', quantite: 1, justification: '' }]);
    setSubmissionToken(crypto.randomUUID());
  };

  const addItem = () => {
    setItems([...items, { id: crypto.randomUUID(), itemName: '', description: '', quantite: 1, justification: '' }]);
  };

  const removeItem = (id: string) => {
    if (items.length > 1) {
      setItems(items.filter(i => i.id !== id));
    }
  };

  const handleItemChange = (id: string, field: keyof ItemRow, value: any) => {
    setItems(items.map(item => item.id === id ? { ...item, [field]: value } : item));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    // Transformation pour le backend (DemandeAchatInterne)
    const payload = {
      designation: items[0]?.itemName || 'Nouvelle Demande', 
      quantite: items.reduce((acc, it) => acc + it.quantite, 0),
      justification: globalJustification || items[0]?.justification,
      urgence: urgency,
      budgetFamille: { id: Number(budgetFamilleId) },
      budgetSousFamille: { id: Number(budgetSousFamilleId) },
      // On garde les détails au cas où le backend évolue, mais le root est ce qui compte pour l'entité actuelle
      details: items.map(item => ({
        itemName: item.itemName,
        description: item.description,
        quantite: item.quantite,
        justification: item.justification,
        subFamily: { id: Number(budgetSousFamilleId) },
        prix_unitaire: 0 
      })),
      submissionToken
    };

    createMutation.mutate(payload);
  };

  const handleDownloadPo = async (daId: number) => {
    try {
      toast.loading('Génération du PDF...', { id: 'download-po' });
      const response = await downloadPOByDA(daId);
      
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Bon_Commande_${daId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      
      toast.success('Bon de Commande téléchargé !', { id: 'download-po' });
    } catch (error) {
      console.error('Download error:', error);
      toast.error('Erreur lors du téléchargement du BC', { id: 'download-po' });
    }
  };

  const kpis = {
    total:   daList.length,
    pending: daList.filter((d: any) => !['PO_CREE', 'REJETEE', 'APPROUVEE'].includes(d.statut)).length,
    valid:   daList.filter((d: any) => d.statut === 'APPROUVEE' || d.statut === 'PO_CREE').length,
    rejected:daList.filter((d: any) => d.statut === 'REJETEE').length,
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
          onClick={() => { resetForm(); setShowNewForm(true); }}
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
          <div className="flex justify-end items-center gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
            {selectedDa.statut === 'PO_CREE' && (
              <button 
                onClick={() => handleDownloadPo(selectedDa.oid_da || selectedDa.id)}
                className="px-6 py-2.5 rounded-xl bg-indigo-600 text-white font-bold text-sm hover:bg-indigo-700 transition-all shadow-lg shadow-indigo-100 dark:shadow-none flex items-center gap-2"
              >
                <span>📄</span> Télécharger le BC
              </button>
            )}
            <button 
              onClick={() => setSelectedDa(null)}
              className="px-6 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-semibold text-sm hover:bg-slate-200 transition-all"
            >
              Fermer
            </button>
          </div>
        </DaModal>
      )}

      {/* Create DA Form Modal - MULTI-ITEMS VERSION */}
      {showNewForm && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[60] flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-6xl max-h-[95vh] flex flex-col animate-scale-in">
            {/* Header */}
            <div className="px-8 py-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-800 dark:text-white">Nouvelle Demande de Besoins Internes</h2>
                <p className="text-xs text-slate-400 mt-1">Équipements informatiques, mobilier, fournitures ou maintenance pour votre service.</p>
              </div>
              <button onClick={() => setShowNewForm(false)} className="p-2 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full text-slate-400">✕</button>
            </div>

            <form id="da-form" onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-10 space-y-10">
              {/* Global Settings */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-10 bg-slate-50 dark:bg-slate-800/50 p-8 rounded-3xl border border-slate-100 dark:border-slate-700 shadow-inner">
                <div className="space-y-2">
                  <label className="text-[11px] font-bold text-slate-400 uppercase tracking-widest ml-1">Urgence *</label>
                  <select 
                    value={urgency} onChange={e => setUrgency(e.target.value)}
                    className="w-full px-5 py-3.5 rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm font-medium focus:ring-2 focus:ring-blue-500 outline-none transition-all shadow-sm"
                  >
                    <option value="NORMALE">🟢 Normale</option>
                    <option value="URGENTE">🟠 Urgente</option>
                    <option value="CRITIQUE">🔴 Critique</option>
                  </select>
                </div>
                <div className="space-y-2">
                  <label className="text-[11px] font-bold text-slate-400 uppercase tracking-widest ml-1">Catégorie / Famille *</label>
                  <select 
                    value={budgetFamilleId} onChange={e => setBudgetFamilleId(e.target.value)} required
                    className="w-full px-5 py-3.5 rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm font-medium focus:ring-2 focus:ring-blue-500 outline-none transition-all shadow-sm"
                  >
                    <option value="">Sélectionner une catégorie...</option>
                    {families.map((f: any) => (
                      <option key={f.id} value={f.id}>
                        {f.name || f.libelle} (Dispo: {formatCurrency(f.budget_restant || 0)})
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-2">
                  <label className="text-[11px] font-bold text-slate-400 uppercase tracking-widest ml-1">Sous-Famille *</label>
                  <select 
                    value={budgetSousFamilleId} onChange={e => setBudgetSousFamilleId(e.target.value)} required
                    className="w-full px-5 py-3.5 rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm font-medium focus:ring-2 focus:ring-blue-500 outline-none transition-all shadow-sm"
                  >
                    <option value="">Sélectionner une sous-famille...</option>
                    {subFamilies.map((sf: any) => (
                      <option key={sf.id} value={sf.id}>
                        {sf.name || sf.libelle} (Dispo: {formatCurrency(sf.budget_restant || 0)})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Items Section */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-800 dark:text-white flex items-center gap-2">
                    📦 3 — ITEMS (DETAIL LINES)
                  </h3>
                </div>

                {/* Info Note */}
                <div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-100 dark:border-amber-900/30 p-4 rounded-xl flex items-start gap-3">
                  <span className="text-amber-500">ℹ️</span>
                  <p className="text-xs text-amber-800 dark:text-amber-200 leading-relaxed">
                    <span className="font-bold">Note:</span> Unit prices are <span className="font-bold text-amber-600 dark:text-amber-400">not filled by the requester</span>. 
                    They will be negotiated and defined by the Buyer (AMG) during the purchase treatment phase.
                  </p>
                </div>

                {/* Items Table */}
                <div className="overflow-x-auto border border-slate-100 dark:border-slate-800 rounded-2xl">
                  <table className="w-full text-left border-collapse">
                    <thead className="bg-slate-50 dark:bg-slate-800/80">
                      <tr>
                        <th className="px-4 py-3 text-[10px] font-bold text-slate-400 uppercase">Item Code (Auto)</th>
                        <th className="px-4 py-3 text-[10px] font-bold text-slate-400 uppercase">Item Name *</th>
                        <th className="px-4 py-3 text-[10px] font-bold text-slate-400 uppercase">Details / Specs</th>
                        <th className="px-4 py-3 text-[10px] font-bold text-slate-400 uppercase w-20">Qty *</th>
                        <th className="px-4 py-3 text-[10px] font-bold text-slate-400 uppercase">Justification</th>
                        <th className="px-4 py-3 w-10"></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                      {items.map((item, index) => (
                        <tr key={item.id} className="group">
                          <td className="px-4 py-3">
                            <span className="text-[10px] font-black text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/40 px-2.5 py-1.5 rounded-lg border border-blue-100 dark:border-blue-800 shadow-sm">
                              REQ-{(index + 1).toString().padStart(4, '0')}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            <input 
                              value={item.itemName} onChange={e => handleItemChange(item.id, 'itemName', e.target.value)}
                              placeholder="e.g. Laptop HP..." required
                              className="w-full bg-transparent text-sm outline-none focus:text-blue-600 dark:focus:text-blue-400"
                            />
                          </td>
                          <td className="px-4 py-3">
                            <input 
                              value={item.description} onChange={e => handleItemChange(item.id, 'description', e.target.value)}
                              placeholder="Technical specs..."
                              className="w-full bg-transparent text-sm outline-none focus:text-blue-600 dark:focus:text-blue-400"
                            />
                          </td>
                          <td className="px-4 py-3">
                            <input 
                              type="number" min={1} value={item.quantite} onChange={e => handleItemChange(item.id, 'quantite', Number(e.target.value))} required
                              className="w-full bg-transparent text-sm outline-none font-bold"
                            />
                          </td>
                          <td className="px-4 py-3">
                            <input 
                              value={item.justification} onChange={e => handleItemChange(item.id, 'justification', e.target.value)}
                              placeholder="Why this item?"
                              className="w-full bg-transparent text-sm outline-none focus:text-blue-600 dark:focus:text-blue-400"
                            />
                          </td>
                          <td className="px-4 py-3 text-right">
                            <button 
                              type="button" onClick={() => removeItem(item.id)}
                              className="p-1.5 text-slate-300 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-all"
                            >
                              ✕
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <button 
                  type="button" onClick={addItem}
                  className="w-full py-3 border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-2xl text-slate-400 text-sm font-bold hover:border-blue-400 hover:text-blue-500 transition-all"
                >
                  + Add Item
                </button>
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider ml-1">Justification Globale (Optionnelle)</label>
                <textarea 
                  value={globalJustification} onChange={e => setGlobalJustification(e.target.value)} rows={2}
                  placeholder="Informations complémentaires sur l'ensemble de la demande..."
                  className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm focus:ring-2 focus:ring-blue-500 outline-none resize-none"
                />
              </div>
            </form>

            {/* Footer */}
            <div className="p-8 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-800/50 rounded-b-3xl">
              <div className="flex gap-4">
                <button 
                  type="button" onClick={() => setShowNewForm(false)}
                  className="px-8 py-3 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 font-bold text-sm hover:bg-slate-50 transition-all"
                >
                  Cancel
                </button>
                <button 
                  type="submit" form="da-form" disabled={createMutation.isPending}
                  className="px-10 py-3 rounded-2xl bg-blue-600 text-white font-black text-sm hover:bg-blue-700 disabled:opacity-50 shadow-xl shadow-blue-200 dark:shadow-none transition-all flex items-center gap-2"
                >
                  {createMutation.isPending ? '...' : '💾 Submit Request'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
