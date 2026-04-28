import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import { getDAByDemandeur, createDA, getFamilies, getSubFamilies, getSubFamiliesByFamily } from '../api/services';
import type { DaHeader, Family, SubFamily } from '../types';

export default function DemandeurPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DaHeader | null>(null);
  const [showNewForm, setShowNewForm] = useState(false);
  const [search, setSearch] = useState('');

  // Form state
  const [objet, setObjet] = useState('');
  const [justification, setJustification] = useState('');
  const [urgency, setUrgency] = useState('NORMAL');
  const [familyId, setFamilyId] = useState('');
  const [subFamilyId, setSubFamilyId] = useState('');
  const [quantity, setQuantity] = useState(1);

  const { data: daList = [], isLoading } = useQuery({
    queryKey: ['da', 'demandeur', user?.userId],
    queryFn: () => getDAByDemandeur(user!.userId).then(r => r.data),
    enabled: !!user,
  });

  const { data: families = [] } = useQuery({
    queryKey: ['families'],
    queryFn: () => getFamilies().then(r => r.data),
  });

  const { data: subFamilies = [] } = useQuery({
    queryKey: ['sub-families', familyId],
    queryFn: () => familyId ? getSubFamiliesByFamily(Number(familyId)).then(r => r.data) : getSubFamilies().then(r => r.data),
  });

  const createMutation = useMutation({
    mutationFn: (payload: Partial<DaHeader>) => createDA(payload),
    onSuccess: () => {
      toast.success('Demande créée et soumise !');
      qc.invalidateQueries({ queryKey: ['da', 'demandeur'] });
      setShowNewForm(false);
      resetForm();
    },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const resetForm = () => {
    setObjet(''); setJustification(''); setUrgency('NORMAL');
    setFamilyId(''); setSubFamilyId(''); setQuantity(1);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    const sf = subFamilies.find((s: SubFamily) => String(s.id) === subFamilyId);
    createMutation.mutate({
      objet,
      justification,
      urgencyLevel: urgency,
      demandeur: { oid_user: user.userId } as any,
      details: sf ? [{ subFamily: sf, quantite: quantity, prix_unitaire: 0, description: objet }] : [],
    });
  };

  const kpis = {
    total:   daList.length,
    pending: daList.filter(d => !['PO_CREE','REJETEE'].includes(d.statut)).length,
    done:    daList.filter(d => d.statut === 'PO_CREE').length,
    rejected:daList.filter(d => d.statut === 'REJETEE').length,
  };

  return (
    <DashboardLayout title="Mon Espace — Suivi des Demandes" pendingCount={kpis.pending}>
      {/* KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <KpiCard label="Mes demandes"     value={kpis.total}    icon="📄" color="from-indigo-500 to-blue-600" />
        <KpiCard label="En cours"         value={kpis.pending}  icon="⏳" color="from-amber-500 to-orange-600" />
        <KpiCard label="PO Créé"          value={kpis.done}     icon="✅" color="from-emerald-500 to-green-600" />
        <KpiCard label="Rejetées"         value={kpis.rejected} icon="❌" color="from-red-500 to-rose-600" />
      </div>

      {/* Action bar */}
      <div className="flex gap-3 mb-5">
        <input
          value={search} onChange={e => setSearch(e.target.value)}
          placeholder="🔍 Rechercher..."
          className="flex-1 px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300"
        />
        <button
          id="btn-new-da"
          onClick={() => setShowNewForm(true)}
          className="px-5 py-2 rounded-xl bg-gradient-to-r from-indigo-500 to-violet-600 text-white font-semibold text-sm hover:from-indigo-600 hover:to-violet-700 transition-all shadow-lg shadow-indigo-200"
        >
          ➕ Nouvelle DA
        </button>
      </div>

      {/* Table */}
      <DaTable
        rows={daList}
        onRowClick={setSelectedDa}
        showRequester={false}
        loading={isLoading}
        searchQuery={search}
        actionLabel={() => '👁 Voir'}
      />

      {/* Detail modal (read-only for demandeur) */}
      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="📋 Suivi de ma Demande">
          <div className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-700 flex justify-end">
            <button onClick={() => setSelectedDa(null)} className="px-6 py-2 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-200 text-sm font-medium hover:bg-slate-200 transition-colors">
              Fermer
            </button>
          </div>
        </DaModal>
      )}

      {/* New DA form modal */}
      {showNewForm && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl w-full max-w-lg animate-fade-in-up">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-700">
              <h2 className="font-bold text-slate-800 dark:text-white">➕ Nouvelle Demande d'Achat</h2>
              <button onClick={() => { setShowNewForm(false); resetForm(); }} className="text-slate-400 hover:text-slate-600 text-xl">✕</button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="text-xs font-semibold text-slate-500 mb-1 block">Objet *</label>
                <input value={objet} onChange={e => setObjet(e.target.value)} required
                  className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" placeholder="Ex: Achat de PC portables" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-500 mb-1 block">Famille *</label>
                  <select value={familyId} onChange={e => { setFamilyId(e.target.value); setSubFamilyId(''); }} required
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300">
                    <option value="">Toutes les familles...</option>
                    {families.map((f: Family) => (
                      <option key={f.id} value={f.id}>{f.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-500 mb-1 block">Sous-famille *</label>
                  <select value={subFamilyId} onChange={e => setSubFamilyId(e.target.value)} required
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300">
                    <option value="">Choisir...</option>
                    {subFamilies.map((sf: SubFamily) => (
                      <option key={sf.id} value={sf.id}>{sf.name}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-500 mb-1 block">Quantité *</label>
                  <input type="number" min={1} value={quantity} onChange={e => setQuantity(Number(e.target.value))} required
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300" />
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-500 mb-1 block">Urgence</label>
                  <select value={urgency} onChange={e => setUrgency(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300">
                    <option value="NORMAL">Normal</option>
                    <option value="URGENT">Urgent</option>
                    <option value="CRITIQUE">Critique</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="text-xs font-semibold text-slate-500 mb-1 block">Justification</label>
                <textarea value={justification} onChange={e => setJustification(e.target.value)} rows={3}
                  className="w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300 resize-none"
                  placeholder="Pourquoi cette demande est-elle nécessaire ?" />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => { setShowNewForm(false); resetForm(); }}
                  className="px-5 py-2 rounded-xl bg-slate-100 text-slate-700 text-sm font-medium hover:bg-slate-200 transition-colors">
                  Annuler
                </button>
                <button type="submit" disabled={createMutation.isPending}
                  className="px-6 py-2 rounded-xl bg-gradient-to-r from-indigo-500 to-violet-600 text-white font-semibold text-sm disabled:opacity-60 transition-all hover:from-indigo-600 hover:to-violet-700">
                  {createMutation.isPending ? 'Envoi...' : '✅ Soumettre la DA'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
