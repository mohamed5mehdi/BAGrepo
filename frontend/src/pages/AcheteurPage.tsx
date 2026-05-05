import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import {
  getDemandesAValiderInternes, getSuppliers, traiterAchatDemandeInterne, creerPODemandeInterne, getDemandeInterneById, valoriserDemandeInterne, solliciterAjustementDemandeInterne, getDemandeOffres, getSubFamiliesByFamily
} from '../api/services';
import type { Supplier, SupplierOffer, SubFamily } from '../types';
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

  const { data: offers = [], isLoading: loadingOffers } = useQuery({
    queryKey: ['da-offres', selectedDa?.id],
    queryFn: () => getDemandeOffres(selectedDa.id).then(r => r.data),
    enabled: !!selectedDa,
  });

  const { data: allSubFamilies = [] } = useQuery({
    queryKey: ['sub-families', selectedDa?.budgetFamille?.id],
    queryFn: () => getSubFamiliesByFamily(selectedDa.budgetFamille.id).then(r => r.data),
    enabled: !!selectedDa?.budgetFamille?.id,
  });

  const openDa = (da: any) => {
    setSelectedDa(da);
    setSelectedSupplier(da.fournisseur?.id ?? null);
    setPrixUnitaire(da.prixUnitaire ?? 0);
  };


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

  const requestAdjustmentMutation = useMutation({
    mutationFn: (type: 'SOUS_FAMILLE' | 'FAMILLE') => solliciterAjustementDemandeInterne(selectedDa.id, type, user!.userId),
    onSuccess: () => {
      toast.success('🏗️ Demande d\'ajustement budgétaire envoyée !');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la demande d\'ajustement');
    }
  });

  // Unification du flux : supporte DaHeader et DemandeAchatInterne (BAG)
  const firstDetail = selectedDa?.details?.[0];
  
  // Chercher la sous-famille partout où elle peut être cachée
  const currentSubFamily = selectedDa?.budgetSousFamille || firstDetail?.subFamily || selectedDa?.subFamily;
  const currentFamily = selectedDa?.budgetFamille || firstDetail?.subFamily?.family || currentSubFamily?.family;

  const totalHT  = (selectedDa?.quantite || 0) * prixUnitaire;
  const totalTax = totalHT * VAT;
  const totalTTC = Number((totalHT + totalTax).toFixed(2));

  // Normalisation des noms et montants
  const sfName = currentSubFamily?.name || currentSubFamily?.libelle || '—';
  const fName = currentFamily?.name || currentFamily?.libelle || '—';
  
  // Utilisation du nouveau champ budget_disponible corrigé (Back-end)
  const budgetRestant = currentSubFamily?.budget_disponible ?? 0;
  
  const isBudgetExceeded = totalTTC > budgetRestant;

  return (
    <DashboardLayout title="Espace Acheteur — BAG Procurement" pendingCount={myDAs.length}>
      {/* KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <KpiCard label="DA à traiter" value={myDAs.filter((d: any) => d.statut === 'VALIDEE_TECH').length} icon="⏳" color="from-blue-600 to-indigo-700" />
        <KpiCard label="Prêt pour PO" value={myDAs.filter((d: any) => d.statut === 'APPROUVEE').length} icon="📜" color="from-emerald-500 to-teal-600" />
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

      <DaTable rows={myDAs} onRowClick={openDa} loading={isLoading} searchQuery={search} showRequester={true} actionLabel={(d: any) => d.statut === 'APPROUVEE' ? '📜 Créer PO' : '✏️ Traiter'} />

      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="🛒 Traitement du Dossier Achat" wide>
          <div className="space-y-6 mt-4">
             <section className="space-y-3">
                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center text-[10px] text-slate-600">1</span>
                    Sélection du Fournisseur
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                    {offers.length > 0 ? (
                        offers.map((o: SupplierOffer) => {
                            const isBestPrice = o.prixPropose === Math.min(...offers.map(off => off.prixPropose));
                            const s = o.fournisseur;
                            return (
                                <div 
                                    key={o.id}
                                    onClick={() => {
                                        setSelectedSupplier(s.oidSupplier);
                                        setPrixUnitaire(o.prixPropose);
                                    }}
                                    className={`p-4 rounded-2xl border transition-all cursor-pointer relative overflow-hidden flex flex-col justify-between min-h-[160px] ${selectedSupplier === s.oidSupplier ? 'bg-blue-50 border-blue-500 shadow-blue-100 ring-2 ring-blue-200' : 'bg-slate-50 border-slate-100 hover:border-slate-300'}`}
                                >
                                    {isBestPrice && (
                                        <div className="absolute top-0 right-0 bg-emerald-600 text-white text-[8px] font-black px-2 py-0.5 rounded-bl-lg uppercase z-20">
                                            Meilleur Prix
                                        </div>
                                    )}
                                    
                                    <div>
                                        <div className="flex items-center justify-between">
                                            <div>
                                                <p className="font-bold text-sm text-slate-800">{s.nom}</p>
                                                {s.ice && (
                                                    <p className="text-[10px] font-mono text-indigo-500 font-bold bg-indigo-50 dark:bg-indigo-900/30 px-1.5 py-0.5 rounded mt-0.5 w-fit">
                                                        ICE: {s.ice}
                                                    </p>
                                                )}
                                            </div>
                                            {selectedSupplier === s.oidSupplier && <span className="text-blue-600 font-bold">✓</span>}
                                        </div>
                                        <div className="flex items-center gap-1.5 mt-0.5">
                                            <span className="text-amber-500 text-[10px]">{'★'.repeat(s.rating || 0)}</span>
                                            <span className="text-[9px] text-slate-400 uppercase font-bold tracking-tighter">{s.sector}</span>
                                        </div>
                                        
                                        <p className="text-[10px] text-slate-600 dark:text-slate-400 mt-2 font-medium bg-white/80 dark:bg-black/20 p-2 rounded-xl border border-slate-100 dark:border-slate-800 leading-relaxed italic">
                                            <span className="text-indigo-500 mr-1 font-bold">📄</span> {o.conditions}
                                        </p>
                                    </div>

                                    <div className="mt-3 pt-2 border-t border-slate-100 dark:border-slate-800 flex justify-between items-center">
                                        <div>
                                            <p className="text-[8px] text-slate-400 uppercase">Proposition HT</p>
                                            <p className="text-base font-black text-blue-600">{formatCurrency(o.prixPropose)}</p>
                                        </div>
                                        <p className="text-[9px] text-slate-400">🚚 {o.delaiLivraisonOffert || s.averageLeadTime}j</p>
                                    </div>
                                </div>
                            );
                        })
                    ) : (
                        suppliers.map((s: Supplier) => (
                            <div 
                                key={s.oidSupplier}
                                onClick={() => setSelectedSupplier(s.oidSupplier)}
                                className={`p-4 rounded-2xl border transition-all cursor-pointer relative overflow-hidden ${selectedSupplier === s.oidSupplier ? 'bg-blue-50 border-blue-500 shadow-blue-100' : 'bg-slate-50 border-slate-100 hover:border-slate-300'}`}
                            >
                                {s.isCertified && (
                                    <div className="absolute top-0 right-0 bg-blue-600 text-white text-[8px] font-black px-2 py-0.5 rounded-bl-lg uppercase">
                                        Certifié
                                    </div>
                                )}
                                <div className="flex items-center justify-between">
                                     <div>
                                        <p className="font-bold text-sm text-slate-800">{s.nom || 'Fournisseur Inconnu'}</p>
                                        <p className={`text-[9px] font-mono font-bold px-1.5 py-0.5 rounded mt-0.5 w-fit ${s.ice ? 'text-indigo-600 bg-indigo-50 border border-indigo-100' : 'text-slate-400 bg-slate-100 italic'}`}>
                                            ICE: {s.ice || 'Non renseigné'}
                                        </p>
                                     </div>
                                    {selectedSupplier === s.oidSupplier && <span className="text-blue-600">✔</span>}
                                </div>
                                <div className="flex items-center gap-2 mt-1">
                                    <span className="text-amber-500 text-xs">
                                        {'★'.repeat(s.rating || 0)}{'☆'.repeat(5 - (s.rating || 0))}
                                    </span>
                                    <span className="text-[10px] text-slate-400">({s.sector})</span>
                                </div>
                                <p className="text-[10px] text-slate-500 mt-1">Contact: {s.contact || 'Pas de contact'}</p>
                                <p className="text-[9px] text-slate-400 mt-1 flex items-center gap-1">
                                    <span>🚚 Délai: {s.averageLeadTime}j</span>
                                    <span>•</span>
                                    <span className="truncate">{s.adresse}</span>
                                </p>
                            </div>
                        ))
                    )}
                </div>
             </section>

             <section className="space-y-3">
                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center text-[10px] text-slate-600">2</span>
                    Surveillance Budget
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className={`p-4 rounded-2xl border ${isBudgetExceeded ? 'bg-rose-50 border-rose-200' : 'bg-emerald-50 border-emerald-200'}`}>
                        <div className="flex justify-between items-center mb-2">
                            <p className="text-xs font-bold text-slate-500 uppercase">Sous-Famille: {sfName}</p>
                            <span className={`px-2 py-0.5 rounded-full text-[10px] font-black uppercase ${isBudgetExceeded ? 'bg-rose-500 text-white' : 'bg-emerald-500 text-white'}`}>
                                {isBudgetExceeded ? '⚠️ Dépassement' : '✅ OK'}
                            </span>
                        </div>
                        <div className="flex justify-between items-end">
                            <div>
                                <p className="text-[10px] text-slate-400">Budget Disponible</p>
                                <p className="text-lg font-black text-slate-700">{formatCurrency(budgetRestant)}</p>
                            </div>
                            <div className="text-right">
                                <p className="text-[10px] text-slate-400">Montant DA (TTC)</p>
                                <p className={`text-lg font-black ${isBudgetExceeded ? 'text-rose-600' : 'text-slate-700'}`}>{formatCurrency(totalTTC)}</p>
                            </div>
                        </div>
                        <div className="mt-3 w-full h-1.5 bg-slate-200 rounded-full overflow-hidden">
                            <div 
                                className={`h-full transition-all duration-500 ${isBudgetExceeded ? 'bg-rose-500' : 'bg-emerald-500'}`} 
                                style={{ width: `${Math.min(100, (totalTTC / (budgetRestant || 1)) * 100)}%` }}
                            />
                        </div>
                    </div>

                    <div className="p-4 rounded-2xl border bg-slate-50 border-slate-200">
                        <p className="text-xs font-bold text-slate-500 uppercase mb-2">Famille: {fName}</p>
                        <div className="flex justify-between items-end">
                            <div>
                                <p className="text-[10px] text-slate-400">Budget Restant (Famille)</p>
                                <p className="text-lg font-black text-slate-700">{formatCurrency(currentFamily?.budget_restant || 0)}</p>
                            </div>
                            <div className="text-right">
                                <p className="text-[10px] text-slate-400">Impact Global</p>
                                <p className="text-lg font-black text-slate-600">{formatCurrency((currentFamily?.budget_restant || 0) - totalTTC)}</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Consultation autres sous-familles */}
                <div className="mt-4 p-4 rounded-2xl bg-slate-50/50 border border-slate-100 dark:bg-slate-900/20 dark:border-slate-700">
                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3 flex items-center gap-2">
                         🔍 Référentiel Budgétaire (Toutes les Sous-Familles)
                    </p>
                    {allSubFamilies.length > 0 ? (
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                            {allSubFamilies.map((sf: SubFamily) => (
                                <div key={sf.id} className={`p-2.5 rounded-xl border transition-all ${sf.id === selectedDa.budgetSousFamille?.id ? 'bg-blue-50 border-blue-500 ring-1 ring-blue-100' : 'bg-white border-slate-100 shadow-sm'}`}>
                                    <p className="text-[9px] font-bold text-slate-500 truncate uppercase">{sf.name}</p>
                                    <p className={`text-xs font-black ${(sf.budget_disponible || sf.budget_restant || 0) > 0 ? 'text-slate-800' : 'text-rose-500'}`}>
                                        {formatCurrency(sf.budget_disponible || sf.budget_restant || 0)}
                                    </p>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p className="text-[10px] text-slate-400 italic">Chargement des données budgétaires ou aucune autre sous-famille...</p>
                    )}
                </div>
             </section>

             <section className="space-y-3">
                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center text-[10px] text-slate-600">3</span>
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
                {selectedDa.statut === 'VALIDEE_TECH' && !isBudgetExceeded && (
                    <button 
                        onClick={() => submitTreatmentMutation.mutate()}
                        disabled={!selectedSupplier || prixUnitaire <= 0 || submitTreatmentMutation.isPending}
                        className="px-8 py-3 rounded-xl bg-blue-600 text-white font-black text-sm hover:bg-blue-700 shadow-xl shadow-blue-200"
                    >
                        🚀 Envoyer pour Validation Finale
                    </button>
                )}
                {selectedDa.statut === 'VALIDEE_TECH' && isBudgetExceeded && (
                    <div className="flex gap-2">
                        <button 
                            onClick={() => requestAdjustmentMutation.mutate('SOUS_FAMILLE')}
                            className="px-6 py-3 rounded-xl bg-amber-500 text-white font-black text-sm hover:bg-amber-600 shadow-xl shadow-amber-200"
                        >
                            ⚖️ Ajustement Sous-Famille
                        </button>
                        <button 
                            onClick={() => requestAdjustmentMutation.mutate('FAMILLE')}
                            className="px-6 py-3 rounded-xl bg-rose-600 text-white font-black text-sm hover:bg-rose-700 shadow-xl shadow-rose-200"
                        >
                            🏛️ Ajustement Famille
                        </button>
                    </div>
                )}
                {selectedDa.statut === 'APPROUVEE' && (
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
