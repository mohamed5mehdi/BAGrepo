import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import {
  getAllDA, checkBudget, requestAdjustment,
  createPO, validateWorkflow, getSuppliers, updateDA,
} from '../api/services';
import type { DaHeader, Supplier } from '../types';
import { formatCurrency } from '../utils/constants';

const VAT = 0.19;

interface ItemRow { name: string; desc: string; qty: number; ht: number; }

export default function AcheteurPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DaHeader | null>(null);
  const [selectedSupplier, setSelectedSupplier] = useState<number | null>(null);
  const [items, setItems] = useState<ItemRow[]>([]);
  const [comment, setComment] = useState('');
  const [adjComment, setAdjComment] = useState('');
  const [budgetResult, setBudgetResult] = useState<'SUFFISANT' | 'INSUFFISANT' | null>(null);
  const [search, setSearch] = useState('');

  const { data: all = [], isLoading } = useQuery({
    queryKey: ['da', 'all'],
    queryFn: () => getAllDA().then(r => r.data),
    refetchInterval: 30_000,
  });

  const { data: suppliers = [] } = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => getSuppliers().then(r => r.data),
  });

  const myDAs = all.filter(d => d.statut === 'EN_ATTENTE_ACHAT' || d.statut === 'VALIDEE');

  const kpis = {
    total:   all.length,
    mine:    myDAs.length,
    done:    all.filter(d => d.statut === 'PO_CREE').length,
    rejected:all.filter(d => d.statut === 'REJETEE').length,
    inProgress: all.filter(d => !['PO_CREE','REJETEE'].includes(d.statut)).length,
  };

  const openDa = (da: DaHeader) => {
    setSelectedDa(da);
    setSelectedSupplier(null);
    setBudgetResult(null);
    setComment('');
    setAdjComment('');
    const rows: ItemRow[] = da.details?.map(d => ({
      name: d.itemName ?? '',
      desc: d.description ?? '',
      qty: d.quantite ?? 1,
      ht: Number(d.prix_unitaire) ?? 0,
    })) ?? [{ name: '', desc: '', qty: 1, ht: 0 }];
    setItems(rows.length > 0 ? rows : [{ name: '', desc: '', qty: 1, ht: 0 }]);
  };

  const addItem = () => setItems(prev => [...prev, { name: '', desc: '', qty: 1, ht: 0 }]);
  const removeItem = (i: number) => setItems(prev => prev.filter((_, idx) => idx !== i));
  const updateItem = <K extends keyof ItemRow>(i: number, key: K, val: ItemRow[K]) =>
    setItems(prev => prev.map((r, idx) => idx === i ? { ...r, [key]: val } : r));

  const totalHT  = items.reduce((s, r) => s + r.qty * r.ht, 0);
  const totalTax = totalHT * VAT;
  const totalTTC = totalHT + totalTax;

  const checkBudgetMutation = useMutation({
    mutationFn: () => checkBudget(selectedDa!.oid_da, user!.userId),
    onSuccess: ({ data }) => {
      setBudgetResult(data);
      if (data === 'SUFFISANT') toast.success('✅ Budget suffisant — vous pouvez soumettre');
      else toast.error('⚠️ Budget insuffisant — définissez un ajustement');
    },
    onError: () => toast.error('Erreur lors de la vérification budget'),
  });

  const submitTreatmentMutation = useMutation({
    mutationFn: async () => {
      if (!selectedDa || !user) return;
      // 1. Save updated details
      const updatedDetails = items.map((r, i) => ({
        oid_detail: selectedDa.details?.[i]?.oid_detail ?? null,
        itemName: r.name,
        description: r.desc,
        quantite: r.qty,
        prix_unitaire: r.ht,
        fournisseur: selectedSupplier ? { oidSupplier: selectedSupplier } : null,
        subFamily: selectedDa.details?.[i]?.subFamily ?? null,
      }));
      await updateDA(selectedDa.oid_da, { ...selectedDa, details: updatedDetails } as any);
      // 2. Validate workflow
      await validateWorkflow(selectedDa.oid_da, user.userId, 'ACCEPTE', comment || 'Traitement acheteur');
    },
    onSuccess: () => {
      toast.success('Traitement soumis avec succès !');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
    },
    onError: () => toast.error('Erreur lors de la soumission'),
  });

  const requestAdjMutation = useMutation({
    mutationFn: (type: 'SUBFAMILY' | 'FAMILY') =>
      requestAdjustment(selectedDa!.oid_da, user!.userId, type, adjComment),
    onSuccess: (_, type) => {
      toast.success(`Demande d'ajustement ${type === 'SUBFAMILY' ? 'DAF' : 'DG'} envoyée`);
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
    },
    onError: () => toast.error('Erreur lors de la demande d\'ajustement'),
  });

  const createPOMutation = useMutation({
    mutationFn: () => createPO(selectedDa!.oid_da, user!.userId),
    onSuccess: ({ data }) => {
      toast.success(`📜 Bon de commande PO-${data.id_po} créé !`);
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
    },
    onError: () => toast.error('Erreur lors de la création du PO'),
  });

  return (
    <DashboardLayout title="Tableau de Bord — Acheteur" pendingCount={kpis.mine}>
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
        <KpiCard label="Toutes les DA"   value={kpis.total}      icon="📄" color="from-indigo-500 to-blue-600" />
        <KpiCard label="À traiter"       value={kpis.mine}       icon="⏳" color="from-indigo-500 to-blue-700" />
        <KpiCard label="PO Créé"         value={kpis.done}       icon="✅" color="from-emerald-500 to-green-600" />
        <KpiCard label="Rejetées"        value={kpis.rejected}   icon="❌" color="from-red-500 to-rose-600" />
        <KpiCard label="En cours"        value={kpis.inProgress} icon="🔄" color="from-amber-500 to-orange-600" />
      </div>

      <div className="flex gap-3 mb-5">
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="🔍 Rechercher..."
          className="flex-1 px-4 py-2 rounded-xl border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" />
        <button onClick={() => qc.invalidateQueries({ queryKey: ['da'] })}
          className="px-4 py-2 rounded-xl bg-slate-100 text-slate-600 text-sm font-medium hover:bg-slate-200 transition-colors">🔄 Actualiser</button>
      </div>

      <DaTable
        rows={myDAs}
        onRowClick={openDa}
        loading={isLoading}
        searchQuery={search}
        actionLabel={da => da.statut === 'VALIDEE' ? '📜 Créer PO' : '✏️ Traiter'}
      />

      {/* Acheteur modal */}
      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="🛒 Traitement Acheteur" wide>
          {/* Supplier comparison */}
          <div className="mt-4">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">1 — Comparaison Fournisseurs</p>
            <div className="overflow-x-auto rounded-xl border border-slate-100 dark:border-slate-700">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 dark:bg-slate-900/60 text-xs text-slate-500 uppercase">
                  <tr>
                    <th className="px-3 py-2 text-left">Fournisseur</th>
                    <th className="px-3 py-2 text-left">Contact</th>
                    <th className="px-3 py-2 text-left">Adresse</th>
                    <th className="px-3 py-2 text-center">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {(suppliers as Supplier[]).map(s => (
                    <tr
                      key={s.oidSupplier}
                      className={`border-t border-slate-50 dark:border-slate-700 cursor-pointer transition-colors ${selectedSupplier === s.oidSupplier ? 'bg-indigo-50 dark:bg-indigo-900/20' : 'hover:bg-slate-50 dark:hover:bg-slate-800'}`}
                      onClick={() => setSelectedSupplier(s.oidSupplier)}
                    >
                      <td className="px-3 py-2 font-semibold">{s.nom}</td>
                      <td className="px-3 py-2 text-slate-500">{s.contact}</td>
                      <td className="px-3 py-2 text-slate-500">{s.adresse}</td>
                      <td className="px-3 py-2 text-center">
                        <button
                          className={`px-3 py-1 rounded-full text-xs font-semibold border transition-colors ${selectedSupplier === s.oidSupplier ? 'bg-indigo-600 text-white border-indigo-600' : 'border-indigo-300 text-indigo-600 hover:bg-indigo-50'}`}
                          onClick={e => { e.stopPropagation(); setSelectedSupplier(s.oidSupplier); }}
                        >
                          {selectedSupplier === s.oidSupplier ? '✔ Sélectionné' : 'Choisir'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Item lines */}
          <div className="mt-4">
            <div className="flex items-center justify-between mb-2">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">2 — Lignes Articles (TVA {(VAT*100).toFixed(0)}%)</p>
            </div>
            <div className="overflow-x-auto rounded-xl border border-slate-100 dark:border-slate-700">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 dark:bg-slate-900/60 text-xs text-slate-500 uppercase">
                  <tr>
                    <th className="px-2 py-2 text-left">Article</th>
                    <th className="px-2 py-2 text-left">Description</th>
                    <th className="px-2 py-2 text-center w-16">Qté</th>
                    <th className="px-2 py-2 text-right w-28">P.U. HT (€)</th>
                    <th className="px-2 py-2 text-right w-24">TVA (€)</th>
                    <th className="px-2 py-2 text-right w-28">TTC (€)</th>
                    <th className="px-2 py-2 w-8"></th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((r, i) => {
                    const lineHT = r.qty * r.ht;
                    const lineTax = lineHT * VAT;
                    return (
                      <tr key={i} className="border-t border-slate-50 dark:border-slate-700">
                        <td className="px-2 py-1"><input value={r.name} onChange={e => updateItem(i,'name',e.target.value)} placeholder="Nom article" className="w-full px-2 py-1 rounded-lg border border-slate-200 text-xs" /></td>
                        <td className="px-2 py-1"><input value={r.desc} onChange={e => updateItem(i,'desc',e.target.value)} placeholder="Description" className="w-full px-2 py-1 rounded-lg border border-slate-200 text-xs" /></td>
                        <td className="px-2 py-1"><input type="number" min={1} value={r.qty} onChange={e => updateItem(i,'qty',Number(e.target.value))} className="w-16 px-2 py-1 rounded-lg border border-slate-200 text-xs text-center" /></td>
                        <td className="px-2 py-1"><input type="number" min={0} step="0.01" value={r.ht || ''} onChange={e => updateItem(i,'ht',parseFloat(e.target.value)||0)} placeholder="0.00" className="w-28 px-2 py-1 rounded-lg border border-slate-200 text-xs text-right" /></td>
                        <td className="px-2 py-1 text-right text-slate-400 text-xs">{lineTax.toFixed(2)}</td>
                        <td className="px-2 py-1 text-right font-semibold text-emerald-600 text-xs">{(lineHT+lineTax).toFixed(2)}</td>
                        <td className="px-2 py-1"><button onClick={() => removeItem(i)} className="w-6 h-6 text-red-400 hover:text-red-600 font-bold">✕</button></td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <button onClick={addItem} className="mt-2 px-4 py-1.5 rounded-lg border border-dashed border-indigo-300 text-indigo-600 text-xs font-medium hover:bg-indigo-50 transition-colors">+ Ajouter ligne</button>

            {/* Totals */}
            <div className="grid grid-cols-3 gap-3 mt-3">
              {[['Total HT', formatCurrency(totalHT), 'text-slate-700'], ['TVA', formatCurrency(totalTax), 'text-amber-600'], ['Total TTC', formatCurrency(totalTTC), 'text-emerald-600']].map(([l,v,c]) => (
                <div key={l} className="bg-slate-50 dark:bg-slate-800 rounded-xl p-3 text-center">
                  <p className="text-xs text-slate-400 mb-1">{l}</p>
                  <p className={`font-bold text-lg ${c}`}>{v}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Buyer notes */}
          <div className="mt-4">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">3 — Notes Acheteur</p>
            <textarea value={comment} onChange={e => setComment(e.target.value)} rows={2} placeholder="Notes de négociation, remarques..."
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300 resize-none" />
          </div>

          {/* Budget adjustment section */}
          {budgetResult === 'INSUFFISANT' && (
            <div className="mt-4 p-4 rounded-xl border-2 border-dashed border-amber-300 bg-amber-50 dark:bg-amber-900/20">
              <h4 className="font-semibold text-amber-800 dark:text-amber-300 mb-1">⚠️ Budget Insuffisant — Définir l'Ajustement</h4>
              <p className="text-xs text-amber-700 dark:text-amber-400 mb-3">Choisissez le circuit d'ajustement selon le guide métier :</p>
              <textarea value={adjComment} onChange={e => setAdjComment(e.target.value)} rows={2} placeholder="Justification de l'ajustement..."
                className="w-full px-4 py-2 rounded-xl border border-amber-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-amber-300 resize-none mb-3" />
              <div className="flex gap-3">
                <button onClick={() => requestAdjMutation.mutate('SUBFAMILY')}
                  disabled={requestAdjMutation.isPending}
                  className="flex-1 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white text-sm font-semibold transition-colors disabled:opacity-60">
                  📂 Ajustement Sous-Famille<br /><span className="text-xs opacity-80">(Circuit DAF)</span>
                </button>
                <button onClick={() => requestAdjMutation.mutate('FAMILY')}
                  disabled={requestAdjMutation.isPending}
                  className="flex-1 py-2.5 rounded-xl bg-red-500 hover:bg-red-600 text-white text-sm font-semibold transition-colors disabled:opacity-60">
                  🏢 Ajustement Famille<br /><span className="text-xs opacity-80">(Circuit DG)</span>
                </button>
              </div>
            </div>
          )}

          {/* Actions footer */}
          <div className="mt-5 pt-4 border-t border-slate-100 dark:border-slate-700 flex flex-wrap gap-3 justify-end">
            <button onClick={() => setSelectedDa(null)} className="px-5 py-2 rounded-xl bg-slate-100 text-slate-700 text-sm font-medium hover:bg-slate-200 transition-colors">
              Fermer
            </button>
            {selectedDa.statut === 'EN_ATTENTE_ACHAT' && (
              <>
                <button
                  id="btn-check-budget"
                  onClick={() => checkBudgetMutation.mutate()}
                  disabled={checkBudgetMutation.isPending}
                  className="px-5 py-2 rounded-xl bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 text-indigo-700 text-sm font-semibold transition-colors disabled:opacity-60">
                  {checkBudgetMutation.isPending ? '⏳ Vérification...' : '🔍 Vérifier Budget'}
                </button>
                {budgetResult === 'SUFFISANT' && (
                  <button
                    id="btn-submit-treatment"
                    onClick={() => submitTreatmentMutation.mutate()}
                    disabled={!selectedSupplier || submitTreatmentMutation.isPending}
                    className="px-5 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-green-600 text-white text-sm font-semibold hover:from-emerald-600 hover:to-green-700 transition-all shadow-md disabled:opacity-60">
                    {submitTreatmentMutation.isPending ? 'Soumission...' : '🛒 Soumettre Traitement'}
                  </button>
                )}
              </>
            )}
            {selectedDa.statut === 'VALIDEE' && (
              <button
                id="btn-create-po"
                onClick={() => createPOMutation.mutate()}
                disabled={createPOMutation.isPending}
                className="px-5 py-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 text-white text-sm font-semibold hover:from-indigo-700 hover:to-violet-700 transition-all shadow-md shadow-indigo-200 disabled:opacity-60">
                {createPOMutation.isPending ? 'Génération...' : '📜 Créer Bon de Commande (PO)'}
              </button>
            )}
          </div>
        </DaModal>
      )}
    </DashboardLayout>
  );
}
