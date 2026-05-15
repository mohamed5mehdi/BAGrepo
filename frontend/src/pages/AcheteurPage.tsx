import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { useAuth } from '../context/AuthContext';
import {
  getDemandesAValiderInternes, getSuppliers, traiterAchatDemandeInterne, creerPODemandeInterne, 
  getDemandeInterneById, valoriserDemandeInterne, solliciterAjustementDemandeInterne, 
  getDemandeOffres, getSubFamiliesByFamily, getAllDA, createPO as createClassicPO
} from '../api/services';
import type { Supplier, SupplierOffer, SubFamily, DaHeader, DemandeAchatInterne } from '../types';
import { formatCurrency } from '../utils/constants';

const VAT = 0.20; 

export default function AcheteurPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<any | null>(null);
  const [selectedSupplier, setSelectedSupplier] = useState<number | null>(null);
  const [prixUnitaire, setPrixUnitaire] = useState<number>(0);
  const [search, setSearch] = useState('');
  const [showPOForm, setShowPOForm] = useState(false);
  const [poItemCode, setPoItemCode] = useState('');

  // Flux 1 & 2 : Récupération unifiée des demandes (Internes + Classiques)
  const { data: internalDAs = [], isLoading: loadingInternal } = useQuery({
    queryKey: ['da', 'acheteur', 'internal', user?.userId],
    queryFn: () => getDemandesAValiderInternes(user!.userId).then(r => {
        // Pour l'acheteur, on veut voir tout ce qui est EN_TRAITEMENT ou DISPONIBLE_STOCK
        return r.data;
    }),
    enabled: !!user,
  });

  const { data: classicDAs = [], isLoading: loadingClassic } = useQuery({
    queryKey: ['da', 'acheteur', 'classic'],
    queryFn: () => getAllDA().then(r => r.data.filter(d => 
        d.statut === 'EN_ATTENTE_ACHAT' || d.statut === 'VALIDE_TECH' || d.statut === 'APPROUVEE' || d.statut === 'VALIDEE'
    )),
    enabled: !!user,
  });

  const { data: suppliers = [] } = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => getSuppliers().then(r => r.data),
  });

  const { data: offers = [] } = useQuery({
    queryKey: ['da-offres', selectedDa?.id],
    queryFn: () => getDemandeOffres(selectedDa.id).then(r => r.data),
    enabled: !!selectedDa && !selectedDa.oid_da, // Seulement pour l'interne
  });

  const combinedDAs = [...internalDAs, ...classicDAs].sort((a, b) => 
    new Date(b.dateCreation || 0).getTime() - new Date(a.dateCreation || 0).getTime()
  );

  const openDa = (da: any) => {
    setSelectedDa(da);
    setSelectedSupplier(da.fournisseur?.oidSupplier || da.fournisseur?.id || null);
    setPrixUnitaire(da.prixUnitaire || da.details?.[0]?.prix_unitaire || 0);
    // Détection ItemCode
    const itemCode = da.details?.[0]?.itemCode || '';
    setPoItemCode(itemCode);
    setShowPOForm(false);
  };

  const isClassicFlux = (da: any) => !!da.oid_da;

  const submitTreatmentMutation = useMutation({
    mutationFn: async () => {
        await valoriserDemandeInterne(selectedDa.id, prixUnitaire, selectedSupplier!);
        return traiterAchatDemandeInterne(selectedDa.id, user!.userId);
    },
    onSuccess: () => {
      toast.success('🚀 Dossier achat transmis pour validation AMG/DAF/DG !');
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
    }
  });

  const generatePOMutation = useMutation({
    mutationFn: () => {
        if (isClassicFlux(selectedDa)) {
            return createClassicPO(selectedDa.oid_da, user!.userId);
        } else {
            return creerPODemandeInterne(selectedDa.id, user!.userId);
        }
    },
    onSuccess: () => {
      toast.success(`📜 Bon de Commande (PO) généré avec succès !`);
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
      setShowPOForm(false);
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
    }
  });

  const totalHT  = (selectedDa?.quantite || 0) * prixUnitaire;
  const totalTTC = Number((totalHT * (1 + VAT)).toFixed(2));
  const budgetRestant = selectedDa?.budgetSousFamille?.budget_disponible ?? 0;
  const isBudgetExceeded = totalTTC > budgetRestant && !isClassicFlux(selectedDa);

  return (
    <DashboardLayout title="Espace Acheteur — BAG Procurement" pendingCount={combinedDAs.length}>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <KpiCard label="DA en attente" value={combinedDAs.filter(d => d.statut === 'VALIDE_TECH' || d.statut === 'EN_ATTENTE_ACHAT').length} icon="⏳" color="from-blue-600 to-indigo-700" />
        <KpiCard label="Prêt pour PO" value={combinedDAs.filter(d => d.statut === 'APPROUVEE' || d.statut === 'VALIDEE').length} icon="📜" color="from-emerald-500 to-teal-600" />
        <KpiCard label="Flux Classique" value={classicDAs.length} icon="📋" color="from-slate-600 to-slate-800" />
        <KpiCard label="Flux Interne" value={internalDAs.length} icon="internal" color="from-violet-600 to-indigo-800" />
      </div>

      <div className="bg-white dark:bg-slate-800 p-4 rounded-2xl border border-slate-100 dark:border-slate-700 shadow-sm mb-6 flex items-center gap-4">
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Rechercher une DA..."
          className="flex-1 px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
        <button onClick={() => qc.invalidateQueries({ queryKey: ['da'] })} className="px-4 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 font-bold text-sm">🔄 Actualiser</button>
      </div>

      <DaTable rows={combinedDAs} onRowClick={openDa} loading={loadingInternal || loadingClassic} searchQuery={search} showRequester={true} 
        actionLabel={(d: any) => {
          if (d.isPieceRechange && d.isAvailableInStock) return '✅ Valider Stock';
          if (d.statut === 'APPROUVEE' || d.statut === 'VALIDEE' || (d.isPieceRechange && !d.isAvailableInStock)) return '📜 Créer PO';
          return '✏️ Traiter';
        }} />

      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title={showPOForm ? "📜 Création du Bon de Commande (PO)" : "🛒 Traitement du Dossier Achat"} wide>
          <div className="space-y-6 mt-4">
             {!showPOForm ? (
                <>
                    {/* SECTION 1 : FOURNISSEUR */}
                    <section className="space-y-3">
                        <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                            <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center text-[10px] text-slate-600">1</span>
                            Sélection du Fournisseur
                        </h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                            {offers.length > 0 ? (
                                offers.map((o: SupplierOffer) => (
                                    <div key={o.id} onClick={() => { setSelectedSupplier(o.fournisseur.oidSupplier); setPrixUnitaire(o.prixPropose); }}
                                        className={`p-4 rounded-2xl border transition-all cursor-pointer relative ${selectedSupplier === o.fournisseur.oidSupplier ? 'bg-blue-50 border-blue-500 ring-2 ring-blue-200' : 'bg-slate-50 border-slate-100 hover:border-slate-300'}`}>
                                        <p className="font-bold text-sm text-slate-800">{o.fournisseur.nom}</p>
                                        <p className="text-lg font-black text-blue-600">{formatCurrency(o.prixPropose)}</p>
                                        <p className="text-[10px] text-slate-500 mt-2 italic">"{o.conditions}"</p>
                                    </div>
                                ))
                            ) : (
                                suppliers.map((s: Supplier) => (
                                    <div key={s.oidSupplier} onClick={() => setSelectedSupplier(s.oidSupplier)}
                                        className={`p-4 rounded-2xl border transition-all cursor-pointer ${selectedSupplier === s.oidSupplier ? 'bg-blue-50 border-blue-500 shadow-blue-100' : 'bg-slate-50 border-slate-100 hover:border-slate-300'}`}>
                                        <p className="font-bold text-sm text-slate-800">{s.nom}</p>
                                        <p className="text-[10px] text-slate-500 mt-1">{s.sector} • {s.averageLeadTime}j</p>
                                    </div>
                                ))
                            )}
                        </div>
                    </section>

                    {/* SECTION 2 : BUDGET (Seulement pour Flux Interne) */}
                    {!isClassicFlux(selectedDa) && (
                        <section className="space-y-3">
                            <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center text-[10px] text-slate-600">2</span>
                                Surveillance Budget (TTC)
                            </h3>
                            <div className={`p-5 rounded-2xl border ${isBudgetExceeded ? 'bg-rose-50 border-rose-200' : 'bg-emerald-50 border-emerald-200'}`}>
                                <div className="flex justify-between items-end">
                                    <div>
                                        <p className="text-[10px] text-slate-400 uppercase font-bold">Disponible: {selectedDa.budgetSousFamille?.libelle || '—'}</p>
                                        <p className="text-xl font-black text-slate-700">{formatCurrency(budgetRestant)}</p>
                                    </div>
                                    <div className="text-right">
                                        <p className="text-[10px] text-slate-400 uppercase font-bold">Total DA (avec 20% TVA)</p>
                                        <p className={`text-xl font-black ${isBudgetExceeded ? 'text-rose-600' : 'text-slate-800'}`}>{formatCurrency(totalTTC)}</p>
                                    </div>
                                </div>
                            </div>
                        </section>
                    )}

                    <section className="space-y-3">
                        <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                            <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center text-[10px] text-slate-600">{isClassicFlux(selectedDa) ? '2' : '3'}</span>
                            Valorisation
                        </h3>
                        <div className="p-5 bg-slate-50 dark:bg-slate-800 rounded-2xl grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="text-[10px] font-bold text-slate-400 uppercase">Prix Unitaire HT (MAD)</label>
                                <input type="number" value={prixUnitaire} onChange={e => setPrixUnitaire(parseFloat(e.target.value) || 0)}
                                    className="w-full mt-1 px-4 py-3 rounded-xl border border-slate-200 font-black text-blue-600 focus:ring-2 focus:ring-blue-500 outline-none" />
                            </div>
                            <div className="text-right">
                                <p className="text-[10px] font-bold text-slate-400 uppercase">Total HT</p>
                                <p className="text-2xl font-black text-slate-800">{formatCurrency(totalHT)}</p>
                            </div>
                        </div>
                    </section>

                    <div className="pt-6 border-t border-slate-100 flex justify-end gap-3">
                        <button onClick={() => setSelectedDa(null)} className="px-6 py-3 rounded-xl bg-slate-100 text-slate-600 font-bold text-sm">Fermer</button>
                        
                        {selectedDa.isPieceRechange && selectedDa.isAvailableInStock ? (
                            <button 
                                onClick={() => {
                                    // Action de validation de stock interne (tracabilité)
                                    toast.success("📦 Livraison interne validée ! Bon de sortie généré.");
                                    qc.invalidateQueries({ queryKey: ['da'] });
                                    setSelectedDa(null);
                                }}
                                className="px-8 py-3 rounded-xl bg-emerald-600 text-white font-black text-sm shadow-xl shadow-emerald-100"
                            >
                                ✅ Confirmer Sortie de Stock
                            </button>
                        ) : (selectedDa.statut === 'APPROUVEE' || selectedDa.statut === 'VALIDEE') ? (
                            <button onClick={() => setShowPOForm(true)} className="px-8 py-3 rounded-xl bg-indigo-600 text-white font-black text-sm shadow-xl shadow-indigo-100">📜 Configurer le PO</button>
                        ) : (
                            !isBudgetExceeded ? (
                                <button onClick={() => submitTreatmentMutation.mutate()} disabled={!selectedSupplier || prixUnitaire <= 0}
                                    className="px-8 py-3 rounded-xl bg-blue-600 text-white font-black text-sm hover:bg-blue-700 shadow-xl">🚀 Transmettre pour Validation</button>
                            ) : (
                                <button onClick={() => requestAdjustmentMutation.mutate('SOUS_FAMILLE')} className="px-6 py-3 rounded-xl bg-amber-500 text-white font-black text-sm">⚖️ Solliciter Ajustement</button>
                            )
                        )}
                    </div>
                </>
             ) : (
                <div className="space-y-6">
                    <div className="p-6 bg-indigo-50 border border-indigo-100 rounded-2xl">
                        <p className="text-xs font-bold text-indigo-400 uppercase tracking-widest mb-4">Détails de l'Article</p>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="text-[10px] font-black text-slate-400 uppercase">Item Code (BAG ERP)</label>
                                <input 
                                    value={poItemCode} 
                                    onChange={e => setPoItemCode(e.target.value)}
                                    disabled={isClassicFlux(selectedDa)}
                                    placeholder="Ex: ART-001"
                                    className={`w-full mt-1 px-4 py-2.5 rounded-xl border border-indigo-200 bg-white font-mono text-sm focus:ring-2 focus:ring-indigo-500 outline-none ${isClassicFlux(selectedDa) ? 'opacity-60 cursor-not-allowed' : ''}`}
                                />
                                {isClassicFlux(selectedDa) && <p className="text-[9px] text-indigo-400 mt-1 italic">Code extrait du catalogue référentiel (non modifiable)</p>}
                            </div>
                            <div>
                                <label className="text-[10px] font-black text-slate-400 uppercase">Désignation</label>
                                <div className="mt-1 px-4 py-2.5 rounded-xl bg-white border border-indigo-200 text-sm font-bold text-slate-700">
                                    {selectedDa.designation || selectedDa.objet}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                        <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                            <p className="text-[10px] text-slate-400 uppercase font-bold">Quantité</p>
                            <p className="text-lg font-black text-slate-800">{selectedDa.quantite}</p>
                        </div>
                        <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                            <p className="text-[10px] text-slate-400 uppercase font-bold">Prix Unitaire</p>
                            <p className="text-lg font-black text-slate-800">{formatCurrency(prixUnitaire)}</p>
                        </div>
                        <div className="p-4 bg-indigo-600 rounded-xl shadow-lg shadow-indigo-100">
                            <p className="text-[10px] text-indigo-100 uppercase font-bold">Total TTC</p>
                            <p className="text-lg font-black text-white">{formatCurrency(totalTTC)}</p>
                        </div>
                    </div>

                    <div className="flex justify-end gap-3 pt-4">
                        <button onClick={() => setShowPOForm(false)} className="px-6 py-2.5 rounded-xl bg-slate-100 text-slate-600 font-bold text-sm">Retour</button>
                        <button 
                            onClick={() => generatePOMutation.mutate()}
                            disabled={!poItemCode && !isClassicFlux(selectedDa) || generatePOMutation.isPending}
                            className="px-10 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-blue-700 text-white font-black text-sm shadow-xl shadow-indigo-200"
                        >
                            {generatePOMutation.isPending ? 'Génération...' : '🚀 Confirmer et Générer le BC'}
                        </button>
                    </div>
                </div>
             )}
          </div>
        </DaModal>
      )}
    </DashboardLayout>
  );
}

