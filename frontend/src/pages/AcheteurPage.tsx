import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import {
  getDemandesAValiderInternes, getSuppliers, traiterAchatDemandeInterne, creerPODemandeInterne, getDemandeInterneById, valoriserDemandeInterne
} from '../api/services';
import type { Supplier } from '../types';
import { formatCurrency } from '../utils/constants';

const VAT = 0.20; // 20% TVA BAG

export default function AcheteurPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<any | null>(null);
  const [selectedSupplier, setSelectedSupplier] = useState<number | null>(null);
  const [prixUnitaire, setPrixUnitaire] = useState<number>(0);
  const [search, setSearch] = useState('');

  const { data: myDAs = [], isLoading } = useQuery({
    queryKey: ['da', 'acheteur', user?.userId],
    queryFn: () => getDemandesAValiderInternes(user!.userId).then(r => r.data),
    refetchInterval: 15_000,
    enabled: !!user,
  });

  const { data: suppliers = [] } = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => getSuppliers().then(r => r.data),
  });

  const openDa = (da: any) => {
    setSelectedDa(da);
    setSelectedSupplier(da.fournisseur?.id ?? null);
    setPrixUnitaire(da.prixUnitaire ?? 0);
  };

  const totalHT  = (selectedDa?.quantite || 0) * prixUnitaire;
  const totalTax = totalHT * VAT;
  const totalTTC = totalHT + totalTax;

  const submitTreatmentMutation = useMutation({
    mutationFn: async () => {
        await valoriserDemandeInterne(selectedDa.id, prixUnitaire, selectedSupplier!);
        return traiterAchatDemandeInterne(selectedDa.id, user!.userId);
    },
    onSuccess: () => {
      toast.success('🚀 Dossier achat transmis pour validation AMG/DAF/DG !');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
    },
    onError: (err: any) => {
        toast.error(err.response?.data?.message || 'Erreur lors de la validation');
    }
  });

  const createPOMutation = useMutation({
    mutationFn: () => creerPODemandeInterne(selectedDa.id, user!.userId),
    onSuccess: () => {
      toast.success(`📜 Bon de Commande (PO) généré avec succès !`);
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
    },
    onError: (err: any) => {
        toast.error(err.response?.data?.message || 'Erreur lors de la création du PO');
    }
  });

  return (
    <DashboardLayout title="Espace Acheteur — BAG Procurement" pendingCount={myDAs.length}>
      {/* KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <KpiCard label="DA à traiter" value={myDAs.filter((d: any) => d.statut === 'EN_ATTENTE_ACHAT').length} icon="⏳" color="from-blue-600 to-indigo-700" />
        <KpiCard label="Prêt pour PO" value={myDAs.filter((d: any) => d.statut === 'A_COMMANDER').length} icon="📜" color="from-emerald-500 to-teal-600" />
        <KpiCard label="Total en cours" value={myDAs.length} icon="📦" color="from-slate-600 to-slate-800" />
      </div>

      <div className="bg-white dark:bg-slate-800 p-4 rounded-2xl border border-slate-100 dark:border-slate-700 shadow-sm mb-6 flex items-center gap-4">
        <input 
          value={search} onChange={e => setSearch(e.target.value)}
          placeholder="Rechercher une demande..."
          className="flex-1 px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-sm outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button onClick={() => qc.invalidateQueries({ queryKey: ['da'] })} className="px-4 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 font-bold text-sm">🔄 Actualiser</button>
      </div>

      <DaTable rows={myDAs} onRowClick={openDa} loading={isLoading} searchQuery={search} showRequester={true} actionLabel={(d: any) => d.statut === 'A_COMMANDER' ? '📜 Créer PO' : '✏️ Traiter'} />

      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="🛒 Traitement du Dossier Achat" wide>
          <div className="space-y-6 mt-4">
             <section className="space-y-3">
                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center text-[10px] text-slate-600">1</span>
                    Sélection du Fournisseur
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                    {suppliers.map((s: Supplier) => (
                        <div 
                            key={s.oidSupplier}
                            onClick={() => setSelectedSupplier(s.oidSupplier)}
                            className={`p-4 rounded-2xl border transition-all cursor-pointer ${selectedSupplier === s.oidSupplier ? 'bg-blue-50 border-blue-500 shadow-blue-100' : 'bg-slate-50 border-slate-100 hover:border-slate-300'}`}
                        >
                            <div className="flex items-center justify-between">
                                <p className="font-bold text-sm text-slate-800">{s.nom}</p>
                                {selectedSupplier === s.oidSupplier && <span className="text-blue-600">✔</span>}
                            </div>
                            <p className="text-[10px] text-slate-500 mt-1">{s.contact}</p>
                        </div>
                    ))}
                </div>
             </section>

             <section className="space-y-3">
                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center text-[10px] text-slate-600">2</span>
                    Valorisation & Taxes
                </h3>
                <div className="p-5 bg-slate-50 dark:bg-slate-800 rounded-2xl grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div>
                        <label className="text-[10px] font-bold text-slate-400 uppercase ml-1">Prix Unitaire HT (MAD)</label>
                        <input 
                            type="number" value={prixUnitaire} onChange={e => setPrixUnitaire(parseFloat(e.target.value) || 0)}
                            className="w-full mt-1 px-4 py-3 rounded-xl border border-slate-200 font-black text-blue-600 outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <div className="text-right">
                        <p className="text-[10px] font-bold text-slate-400 uppercase">Montant Total HT</p>
                        <p className="text-xl font-black text-slate-800">{formatCurrency(totalHT)}</p>
                    </div>
                    <div className="text-right">
                        <p className="text-[10px] font-bold text-slate-400 uppercase">Montant Total TTC (20%)</p>
                        <p className="text-xl font-black text-emerald-600">{formatCurrency(totalTTC)}</p>
                    </div>
                </div>
             </section>

             <div className="pt-6 border-t border-slate-100 dark:border-slate-800 flex justify-end gap-4">
                <button onClick={() => setSelectedDa(null)} className="px-6 py-3 rounded-xl bg-slate-100 text-slate-600 font-bold text-sm">Fermer</button>
                {selectedDa.statut === 'EN_ATTENTE_ACHAT' && (
                    <button 
                        onClick={() => submitTreatmentMutation.mutate()}
                        disabled={!selectedSupplier || prixUnitaire <= 0 || submitTreatmentMutation.isPending}
                        className="px-8 py-3 rounded-xl bg-blue-600 text-white font-black text-sm hover:bg-blue-700 shadow-xl shadow-blue-200"
                    >
                        🚀 Envoyer pour Validation Finale
                    </button>
                )}
                {selectedDa.statut === 'A_COMMANDER' && (
                    <button 
                        onClick={() => createPOMutation.mutate()}
                        className="px-8 py-3 rounded-xl bg-gradient-to-r from-indigo-600 to-blue-700 text-white font-black text-sm hover:from-indigo-700 hover:to-blue-800 shadow-xl shadow-indigo-200"
                    >
                        📜 Générer le Bon de Commande (PO)
                    </button>
                )}
             </div>
          </div>
        </DaModal>
      )}
    </DashboardLayout>
  );
}
