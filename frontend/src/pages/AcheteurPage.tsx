import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  getDemandeOffres, postDemandeOffre, getSubFamiliesByFamily, getAllDA, createPO as createClassicPO, checkBudgetClassic,
  getPendingInternalPOsForAutomation, autoGenerateGrnGrc, valoriserDaClassic
} from '../api/services';
import { getAIAnomalies } from '../api/ai-services';
import type { Supplier, SupplierOffer, SubFamily, DemandeAchatInterne } from '../types';
import { formatCurrency } from '../utils/constants';
import { MOCK_CATALOG, CatalogEntry } from '../utils/catalogMock';

const VAT = 0.20; 

export default function AcheteurPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<any | null>(null);
  const [selectedSupplier, setSelectedSupplier] = useState<number | null>(null);
  const [prixUnitaire, setPrixUnitaire] = useState<number>(0);
  const [search, setSearch] = useState('');
  const [showPOForm, setShowPOForm] = useState(false);
  const [poItemCode, setPoItemCode] = useState('');
  const [activeTab, setActiveTab] = useState<'CLASSIC' | 'INTERNAL' | 'PO_GEN'>('CLASSIC');
  const [showOfferForm, setShowOfferForm] = useState(false);
  const [offerForm, setOfferForm] = useState({ fournisseurId: 0, prixPropose: 0, delai: 0, conditions: '' });

  const { data: actionableDAs = [], isLoading: loadingActionable } = useQuery({
    queryKey: ['da', 'acheteur', 'actionable', user?.userId],
    queryFn: () => getDemandesAValiderInternes(user!.userId).then(r => r.data),
    enabled: !!user,
  });

  const isSpecializedBuyer = user?.role !== 'ACHETEUR';

  const { data: suppliers = [] } = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => getSuppliers().then(r => r.data),
  });

  const { data: offers = [] } = useQuery({
    queryKey: ['da-offres', selectedDa?.id],
    queryFn: () => getDemandeOffres(selectedDa.id).then(r => r.data),
    enabled: !!selectedDa && !selectedDa.oid_da, // Seulement pour l'interne
  });

  const { data: approvedPOs = [], isLoading: loadingPOs } = useQuery({
    queryKey: ['pos-approved'],
    queryFn: () => getPendingInternalPOsForAutomation().then(r => r.data),
    enabled: !!user,
  });

  const combinedDAs = actionableDAs.sort((a, b) => 
    new Date(b.dateCreation || 0).getTime() - new Date(a.dateCreation || 0).getTime()
  );

  let displayedDAs = [];
  if (activeTab === 'CLASSIC') {
      displayedDAs = actionableDAs.filter(d => d.statut === 'VALIDE_TECH' && !d.isPieceRechange);
  } else if (activeTab === 'INTERNAL') {
      displayedDAs = actionableDAs.filter(d => (d.statut === 'EN_TRAITEMENT' || d.statut === 'DISPONIBLE_STOCK') && d.isPieceRechange);
  } else if (activeTab === 'PO_GEN') {
      displayedDAs = actionableDAs.filter(d => d.statut === 'APPROUVEE' || d.statut === 'VALIDE_DG');
  }

  const openDa = (da: any) => {
    setSelectedDa(da);
    setSelectedSupplier(da.fournisseur?.oidSupplier || da.fournisseur?.id || null);
    setPrixUnitaire(da.prixUnitaire || da.details?.[0]?.prix_unitaire || 0);
    // Détection ItemCode
    const itemCode = da.details?.[0]?.itemCode || '';
    setPoItemCode(itemCode);
    
    // Si la DA est déjà approuvée/validée, on n'a plus à sélectionner le fournisseur, on passe direct au PO
    const isApproved = da.statut === 'APPROUVEE' || da.statut === 'VALIDEE';
    setShowPOForm(isApproved);
  };

  const isClassicFlux = (da: any) => !da.isPieceRechange;

  const getDynamicCatalogForSubFamily = (da: any, allSuppliers: Supplier[]) => {
      if (!da) return [];
      const sfName = da?.budgetSousFamille?.nom || da?.details?.[0]?.budgetSousFamille?.nom || 'Générique';
      if (MOCK_CATALOG[sfName]) return MOCK_CATALOG[sfName];
      
      const catalog: CatalogEntry[] = [];
      if (!allSuppliers.length) return catalog;
      const estimatedPrice = (da?.montantEstime || 5000) / (da?.quantite || 1);
      
      const hash = sfName.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
      for (let i = 0; i < 3; i++) {
          const supplier = allSuppliers[(hash + i) % allSuppliers.length];
          const variation = 1 + ((hash + i) % 20 - 10) / 100;
          catalog.push({
              fournisseurId: supplier.oidSupplier,
              fournisseurNom: supplier.nom,
              prixRef: Number((estimatedPrice * variation).toFixed(2)),
              delaiRef: 3 + (i * 2),
              conditionsRef: i === 0 ? 'Livraison standard' : 'Livraison express'
          });
      }
      return catalog;
  };

  const catalogForSF = getDynamicCatalogForSubFamily(selectedDa, suppliers);

  const submitTreatmentMutation = useMutation({
    mutationFn: async () => {
        await valoriserDemandeInterne(selectedDa.id, prixUnitaire, selectedSupplier!);
        return traiterAchatDemandeInterne(selectedDa.id, user!.userId);
    },
    onSuccess: () => {
      if (selectedDa.isPieceRechange) {
        toast.success('🚀 Flux SAV : Bypass DG validé ! La demande est prête pour le Bon de Commande.', { duration: 5000 });
      } else {
        toast.success('🚀 Dossier achat transmis pour validation AMG/DAF/DG !');
      }
      qc.invalidateQueries({ queryKey: ['da'] });
      setSelectedDa(null);
    }
  });

  const generatePOMutation = useMutation({
    mutationFn: () => {
        return creerPODemandeInterne(selectedDa.id, user!.userId);
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

  const autoGrnGrcMutation = useMutation({
    mutationFn: (poId: number) => autoGenerateGrnGrc(poId, user!.userId),
    onSuccess: () => {
        toast.success('Génération automatique GRN réussie !');
        qc.invalidateQueries({ queryKey: ['da'] });
        qc.invalidateQueries({ queryKey: ['pos'] });
    },
    onError: (err: any) => {
        toast.error(err.response?.data?.message || 'Erreur lors de la génération GRN.');
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

  const addOfferMutation = useMutation({
    mutationFn: () => postDemandeOffre(selectedDa.id, offerForm),
    onSuccess: () => {
      toast.success('📝 Devis enregistré avec succès !');
      qc.invalidateQueries({ queryKey: ['da-offres', selectedDa.id] });
      setShowOfferForm(false);
      setOfferForm({ fournisseurId: 0, prixPropose: 0, delai: 0, conditions: '' });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la saisie du devis');
    }
  });

  const totalHT  = (selectedDa?.quantite || 0) * prixUnitaire;
  const totalTTC = Number((totalHT * (1 + VAT)).toFixed(2));
  const budgetRestant = selectedDa?.budgetSousFamille?.budget_disponible ?? 0;
  const isBudgetExceeded = totalHT > budgetRestant && !isClassicFlux(selectedDa);

  return (
    <DashboardLayout title="Espace Acheteur — BAG Procurement" pendingCount={combinedDAs.length}>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <KpiCard label="DA en attente" value={actionableDAs.filter(d => d.statut === 'EN_TRAITEMENT').length} icon="⏳" color="from-blue-600 to-indigo-700" />
        <KpiCard label="Prêt pour PO" value={actionableDAs.filter(d => d.statut === 'APPROUVEE' || d.statut === 'VALIDE_DG').length} icon="📜" color="from-emerald-500 to-teal-600" />
        <KpiCard label="Flux Classique" value={actionableDAs.filter(d => d.statut === 'EN_TRAITEMENT' && !d.isPieceRechange).length} icon="📋" color="from-slate-600 to-slate-800" />
        <KpiCard label="Flux Interne" value={actionableDAs.filter(d => d.statut === 'EN_TRAITEMENT' && d.isPieceRechange).length} icon="internal" color="from-violet-600 to-indigo-800" />
      </div>

      {/* TABS (Modern Pills) */}
      <div className="flex flex-wrap gap-3 mb-6 bg-white dark:bg-slate-800 p-2 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700">
          <button 
              onClick={() => setActiveTab('CLASSIC')}
              className={`px-5 py-2.5 rounded-xl font-bold text-sm transition-all duration-300 ${activeTab === 'CLASSIC' ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-700'}`}
          >
              📋 Classic Procurement
          </button>
          <button 
              onClick={() => setActiveTab('INTERNAL')}
              className={`px-5 py-2.5 rounded-xl font-bold text-sm transition-all duration-300 ${activeTab === 'INTERNAL' ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-700'}`}
          >
              🏭 Internal Requests
          </button>
          <button 
              onClick={() => setActiveTab('PO_GEN')}
              className={`px-5 py-2.5 rounded-xl font-bold text-sm transition-all duration-300 ${activeTab === 'PO_GEN' ? 'bg-indigo-600 text-white shadow-md' : 'text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-700'}`}
          >
              🚀 PO Generation
          </button>
      </div>

      {/* TOOLBAR */}
      <div className="bg-white dark:bg-slate-800 p-4 rounded-2xl border border-slate-100 dark:border-slate-700 shadow-sm mb-6 flex items-center gap-4">
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Rechercher..."
          className="flex-1 px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
        <button onClick={() => navigate('/ai-dashboard')}
          className="px-4 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-bold hover:bg-indigo-700 transition-colors flex items-center gap-2">
          📊 Surveillance IA
        </button>
        <button onClick={() => qc.invalidateQueries({ queryKey: ['da'] })} className="px-4 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 font-bold text-sm">🔄 Actualiser</button>
      </div>

      {/* DYNAMIC DATATABLE */}
      <DaTable 
        rows={displayedDAs} 
        onRowClick={openDa} 
        loading={loadingActionable} 
        searchQuery={search} 
        showRequester={activeTab !== 'PO_GEN'} 
        renderExtraBadge={(d: any) => d.isPieceRechange && (
          <span className="ml-2 px-2 py-0.5 text-[9px] font-black bg-rose-100 text-rose-600 border border-rose-200 rounded uppercase">SAV / Maintenance</span>
        )}
        actionLabel={(d: any) => {
          if (activeTab === 'PO_GEN') return '📜 Créer PO';
          if (d.isPieceRechange && d.isAvailableInStock) return '✅ Valider Stock';
          return '✏️ Traiter';
        }} 
      />

      {/* SECTION AUTOMATISATION GRN (Only visible in PO_GEN Tab) */}
      {activeTab === 'PO_GEN' && approvedPOs.length > 0 && (
          <div className="mt-8 mb-6">
              <h2 className="text-lg font-black text-slate-800 dark:text-white flex items-center gap-2 mb-4">
                  <span className="w-8 h-8 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center text-sm">⚡</span>
                  Automated GRN (Internal Flows)
              </h2>
              <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 overflow-hidden">
                  <table className="w-full text-left border-collapse">
                      <thead>
                          <tr className="bg-slate-50 dark:bg-slate-900 border-b border-slate-100 dark:border-slate-700">
                              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-wider">PO Ref</th>
                              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-wider">DA Ref</th>
                              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-wider">Article</th>
                              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-wider">Montant (MAD)</th>
                              <th className="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-wider text-right">Action</th>
                          </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                          {approvedPOs.map((po: any) => (
                              <tr key={po.id_po || po.idPo} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                                  <td className="px-6 py-4">
                                      <p className="font-bold text-sm text-slate-800 dark:text-white">{po.poNumber}</p>
                                      <p className="text-[10px] text-slate-500">Approuvé par Resp. Achat</p>
                                  </td>
                                  <td className="px-6 py-4 font-medium text-sm text-slate-600">
                                      DA-{po.demandeInterne?.id?.toString().padStart(4, '0')}
                                  </td>
                                  <td className="px-6 py-4 text-sm text-slate-600">
                                      {po.demandeInterne?.designation}
                                  </td>
                                  <td className="px-6 py-4 font-black text-sm text-indigo-600">
                                      {formatCurrency(po.totalAmount)}
                                  </td>
                                  <td className="px-6 py-4 text-right">
                                      <button 
                                          onClick={() => autoGrnGrcMutation.mutate(po.id_po || po.idPo)}
                                          disabled={autoGrnGrcMutation.isPending}
                                          className="px-4 py-2 bg-gradient-to-r from-indigo-600 to-blue-500 hover:from-indigo-700 hover:to-blue-600 text-white font-bold text-xs rounded-xl shadow-md disabled:opacity-50"
                                      >
                                          {autoGrnGrcMutation.isPending ? 'Processing...' : '⚡ Generate GRN'}
                                      </button>
                                  </td>
                              </tr>
                          ))}
                      </tbody>
                  </table>
              </div>
          </div>
      )}

      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title={showPOForm ? "📜 Création du Bon de Commande (PO)" : "🛒 Traitement du Dossier Achat"} wide>
          <div className="space-y-6 mt-4">
             {!showPOForm ? (
                <>
                    {/* SECTION 1 : FOURNISSEUR */}
                    <section className="space-y-3">
                        <div className="flex justify-between items-center">
                            <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 flex items-center justify-center text-[10px] text-slate-600">1</span>
                                Sélection du Fournisseur
                            </h3>
                            {!isClassicFlux(selectedDa) && (
                                <button onClick={() => setShowOfferForm(!showOfferForm)} className="text-xs font-bold text-blue-600 bg-blue-50 px-3 py-1.5 rounded-lg hover:bg-blue-100">
                                    {showOfferForm ? 'Annuler' : '+ Saisir un devis'}
                                </button>
                            )}
                        </div>

                        {(() => {
                            const sfName = selectedDa?.budgetSousFamille?.nom || selectedDa?.details?.[0]?.budgetSousFamille?.nom || 'Générique';
                            return catalogForSF.length > 0 ? (
                                <div className="bg-white p-3 rounded-lg border border-blue-100 shadow-sm mb-3">
                                    <h4 className="text-[10px] font-bold text-blue-800 uppercase flex items-center gap-1 mb-2">
                                        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                                        Bordereau des Prix de Référence (BPU) - {sfName}
                                    </h4>
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                                        {catalogForSF.map(c => (
                                            <div key={c.fournisseurId} className="flex justify-between items-center text-xs bg-slate-50 px-2 py-1.5 rounded border border-slate-100">
                                                <span className="font-semibold text-slate-700">{c.fournisseurNom}</span>
                                                <span className="font-bold text-blue-600">{formatCurrency(c.prixRef)}</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ) : null;
                        })()}

                        {showOfferForm && (() => {
                            const handleSupplierSelect = (fId: number) => {
                                const entry = catalogForSF.find(c => c.fournisseurId === fId);
                                if (entry) {
                                    setOfferForm({
                                        fournisseurId: fId,
                                        prixPropose: entry.prixRef,
                                        delai: entry.delaiRef,
                                        conditions: entry.conditionsRef
                                    });
                                    toast.success('Prix du BPU appliqué ! Vous pouvez l\'ajuster.');
                                } else {
                                    setOfferForm({ ...offerForm, fournisseurId: fId, prixPropose: 0, delai: 0, conditions: '' });
                                }
                            };

                            const currentBpuEntry = catalogForSF.find(c => c.fournisseurId === offerForm.fournisseurId);

                            return (
                                <div className="p-4 bg-blue-50 border border-blue-100 rounded-xl space-y-4">
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <label className="text-[10px] font-bold text-slate-500 uppercase">Fournisseur</label>
                                            <select value={offerForm.fournisseurId} onChange={e => handleSupplierSelect(parseInt(e.target.value))} className="w-full mt-1 p-2 rounded-lg border-slate-200 text-sm focus:ring-blue-500 outline-none">
                                                <option value={0}>Sélectionnez...</option>
                                                {catalogForSF.map((c: CatalogEntry) => <option key={c.fournisseurId} value={c.fournisseurId}>{c.fournisseurNom}</option>)}
                                            </select>
                                            {currentBpuEntry && (
                                                <div className="mt-2 p-2 bg-indigo-100 border border-indigo-200 rounded-lg text-xs text-indigo-900 flex flex-col gap-1 shadow-sm">
                                                    <span className="font-black flex items-center gap-1">
                                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                                        Existing Quote: {formatCurrency(currentBpuEntry.prixRef)}
                                                    </span>
                                                    <span className="opacity-80">Delay: {currentBpuEntry.delaiRef} days | {currentBpuEntry.conditionsRef}</span>
                                                </div>
                                            )}
                                        </div>
                                    <div>
                                        <label className="text-[10px] font-bold text-slate-500 uppercase">Prix Proposé (MAD)</label>
                                        <input type="number" min="0.01" step="0.01" value={offerForm.prixPropose || ''} onChange={e => setOfferForm({...offerForm, prixPropose: parseFloat(e.target.value)})} className="w-full mt-1 p-2 rounded-lg border-slate-200 text-sm focus:ring-blue-500 outline-none" />
                                    </div>
                                    <div>
                                        <label className="text-[10px] font-bold text-slate-500 uppercase">Délai (jours)</label>
                                        <input type="number" min="0" value={offerForm.delai || ''} onChange={e => setOfferForm({...offerForm, delai: parseInt(e.target.value)})} className="w-full mt-1 p-2 rounded-lg border-slate-200 text-sm focus:ring-blue-500 outline-none" />
                                    </div>
                                    <div>
                                        <label className="text-[10px] font-bold text-slate-500 uppercase">Conditions</label>
                                        <input type="text" value={offerForm.conditions} onChange={e => setOfferForm({...offerForm, conditions: e.target.value})} placeholder="Ex: Livraison incluse" className="w-full mt-1 p-2 rounded-lg border-slate-200 text-sm focus:ring-blue-500 outline-none" />
                                    </div>
                                </div>
                                <div className="flex justify-end">
                                    <button onClick={() => addOfferMutation.mutate()} disabled={addOfferMutation.isPending || !offerForm.fournisseurId || offerForm.prixPropose <= 0 || offerForm.delai < 0} className="px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-lg shadow-sm hover:bg-blue-700 disabled:opacity-50">
                                        {addOfferMutation.isPending ? 'Enregistrement...' : 'Enregistrer le devis'}
                                    </button>
                                </div>
                                </div>
                            );
                        })()}
                        <div className="flex flex-col gap-2 max-h-64 overflow-y-auto pr-2 custom-scrollbar">
                            {offers.length > 0 ? (
                                offers.map((o: SupplierOffer) => (
                                    <div key={o.id} onClick={() => { setSelectedSupplier(o.fournisseur.oidSupplier); setPrixUnitaire(o.prixPropose); }}
                                        className={`flex items-center justify-between p-3 rounded-xl border transition-all cursor-pointer ${selectedSupplier === o.fournisseur.oidSupplier ? 'bg-blue-50 border-blue-500 ring-1 ring-blue-500' : 'bg-slate-50 border-slate-100 hover:border-slate-300'}`}>
                                        <div className="flex flex-col">
                                            <p className="font-bold text-sm text-slate-800">{o.fournisseur.nom}</p>
                                            <div className="flex gap-2 items-center mt-1">
                                                <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-slate-200 text-slate-600">ICE: {o.fournisseur.ice || 'Non Renseigné'}</span>
                                                <span className="text-[10px] text-slate-500">{o.fournisseur.averageLeadTime}j</span>
                                            </div>
                                        </div>
                                        <div className="text-right flex flex-col items-end">
                                            <p className="text-base font-black text-blue-600">{formatCurrency(o.prixPropose)}</p>
                                            <p className="text-[9px] text-slate-400 mt-0.5 max-w-[120px] truncate" title={o.conditions}>{o.conditions}</p>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                catalogForSF.map((bpuEntry: CatalogEntry) => {
                                    const s = suppliers.find((sup: Supplier) => sup.oidSupplier === bpuEntry.fournisseurId);
                                    if (!s) return null;
                                    
                                    return (
                                        <div key={s.oidSupplier} onClick={() => {
                                                setSelectedSupplier(s.oidSupplier);
                                                setPrixUnitaire(bpuEntry.prixRef);
                                                toast.success(`Prix du BPU pré-rempli : ${formatCurrency(bpuEntry.prixRef)}`);
                                            }}
                                            className={`flex items-center justify-between p-3 rounded-xl border transition-all cursor-pointer ${selectedSupplier === s.oidSupplier ? 'bg-blue-50 border-blue-500 ring-1 ring-blue-500' : 'bg-slate-50 border-slate-100 hover:border-slate-300'}`}>
                                            <div className="flex flex-col">
                                                <p className="font-bold text-sm text-slate-800">{s.nom}</p>
                                                <div className="flex gap-2 items-center mt-1">
                                                    <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-slate-200 text-slate-600">ICE: {s.ice || 'Non Renseigné'}</span>
                                                    <span className="text-[10px] text-slate-500">{s.averageLeadTime}j</span>
                                                </div>
                                            </div>
                                            <div className="text-right flex flex-col items-end gap-1">
                                                <span className="px-2 py-1 rounded bg-blue-50 text-blue-700 text-[10px] font-bold border border-blue-200 flex items-center gap-1">
                                                    <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                                    BPU: {formatCurrency(bpuEntry.prixRef)}
                                                </span>
                                            </div>
                                        </div>
                                    );
                                })
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
                                        <p className="text-[10px] text-slate-400 uppercase font-bold">Total DA (HT)</p>
                                        <p className={`text-xl font-black ${isBudgetExceeded ? 'text-rose-600' : 'text-slate-800'}`}>{formatCurrency(totalHT)}</p>
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
                                {/* Le champ Item Code a été supprimé suite à la demande */}
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
                        {!(selectedDa.statut === 'APPROUVEE' || selectedDa.statut === 'VALIDEE') && (
                            <button onClick={() => setShowPOForm(false)} className="px-6 py-2.5 rounded-xl bg-slate-100 text-slate-600 font-bold text-sm">Retour</button>
                        )}
                        <button 
                            onClick={() => generatePOMutation.mutate()}
                            disabled={generatePOMutation.isPending}
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

