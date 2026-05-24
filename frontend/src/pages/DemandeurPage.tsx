import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import ChatbotWidget from '../components/ChatbotWidget';
import { useAuth } from '../context/AuthContext';
import { getMesDemandesInternes, createDemandeInterne, soumettreDemandeInterne, getFamilies, getSubFamiliesByFamily, downloadPOByDA, getStockItems, submitTransfer, getMyTransfers, getAvailableStock, getWarehouses } from '../api/services';
import { formatCurrency } from '../utils/constants';
import type { DemandeAchatInterne, SubFamily, DaDetails } from '../types';

interface ItemRow {
  id: string;
  itemName: string;
  description: string;
  quantite: number;
  justification: string;
}

export default function DemandeurPage({ defaultTab = 'DA' }: { defaultTab?: 'DA' | 'TRANSFERT' }) {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DemandeAchatInterne | null>(null);
  const [showNewForm, setShowNewForm] = useState(false);
  const [showPieceForm, setShowPieceForm] = useState(false);
  const [showChatbot, setShowChatbot] = useState(false);
  const [search, setSearch] = useState('');
  const [activeTab, setActiveTab] = useState<'DA' | 'TRANSFERT'>(defaultTab);

  // Synchronise state with prop changes if navigation occurs
  useEffect(() => {
    setActiveTab(defaultTab);
  }, [defaultTab]);

  const [showTransferForm, setShowTransferForm] = useState(false);

  // Pièce Form State
  const [selectedPieceCode, setSelectedPieceCode] = useState('');
  const [pieceQty, setPieceQty] = useState(1);

  // Form Global State
  const [urgency, setUrgency] = useState('NORMALE');
  const [budgetFamilleId, setBudgetFamilleId] = useState<string>('');
  const [budgetSousFamilleId, setBudgetSousFamilleId] = useState<string>('');
  const [globalJustification, setGlobalJustification] = useState('');
  const shouldSubmitRef = useRef<boolean>(false);

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

  const { data: myTransfers = [], isLoading: isLoadingTransfers } = useQuery({
    queryKey: ['transfers', 'my', user?.userId],
    queryFn: () => getMyTransfers(user!.userId).then(r => r.data),
    enabled: !!user,
  });

  const { data: availableStock = [] } = useQuery({
    queryKey: ['transfers', 'stock'],
    queryFn: () => getAvailableStock().then(r => r.data),
  });

  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => getWarehouses().then(r => r.data),
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

  const { data: stockItems = [] } = useQuery({
    queryKey: ['stock-catalog'],
    queryFn: () => getStockItems().then(r => r.data),
  });

  const selectedPiece = stockItems.find((s: any) => s.itemCode === selectedPieceCode);

  const createMutation = useMutation({
    mutationFn: (payload: any) => createDemandeInterne(payload, user!.userId),
    onSuccess: (res) => {
      const newDa = res.data;
      qc.invalidateQueries({ queryKey: ['da', 'mes-demandes'] });
      
      if (shouldSubmitRef.current) {
        shouldSubmitRef.current = false;
        submitMutation.mutate(newDa.id);
      } else {
        toast.success('Demande enregistrée en brouillon !');
        setShowNewForm(false);
        resetForm();
      }
    },
    onError: () => {
      shouldSubmitRef.current = false;
      toast.error('Erreur lors de la création de la demande');
    },
  });

  const submitMutation = useMutation({
    mutationFn: (id: number) => soumettreDemandeInterne(id, user!.userId),
    onSuccess: (res) => {
      const updatedDa = res.data;
      if (updatedDa.statut === 'DISPONIBLE_STOCK') {
        toast.success('🎉 Succès ! Pièce disponible en stock. Vous pouvez la récupérer au magasin.', { duration: 6000 });
      } else if (updatedDa.isPieceRechange) {
        toast.success('🛠️ Flux SAV : Demande envoyée directement au Bureau des Achats (Bypass N1).');
      } else {
        toast.success('🚀 Flux Standard : Demande soumise avec succès au workflow N1.');
      }
      qc.invalidateQueries({ queryKey: ['da'] });
      setShowNewForm(false);
      setSelectedDa(null);
      resetForm();
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la soumission');
    },
  });

  const [transferDestId, setTransferDestId] = useState('');
  const [transferLines, setTransferLines] = useState<{stockItemId: number, qty: number, maxQty: number, itemCode: string, itemName: string, warehouseSourceId: number}[]>([]);

  const submitTransferMut = useMutation({
    mutationFn: (payload: any) => submitTransfer(payload, user!.userId),
    onSuccess: () => {
      toast.success('Demande de transfert soumise avec succès !');
      qc.invalidateQueries({ queryKey: ['transfers', 'my'] });
      setShowTransferForm(false);
      setTransferLines([]);
      setTransferDestId('');
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erreur lors de la soumission du transfert');
    },
  });

  const handleSubmitTransfer = (e: React.FormEvent) => {
    e.preventDefault();
    if (!transferDestId) return toast.error("Veuillez sélectionner un entrepôt de destination");
    if (transferLines.length === 0) return toast.error("Veuillez ajouter au moins un article");
    
    const sourceIds = new Set(transferLines.map(l => l.warehouseSourceId));
    if (sourceIds.size > 1) return toast.error("Tous les articles doivent provenir du même entrepôt source");
    const sourceId = Array.from(sourceIds)[0];
    
    if (sourceId.toString() === transferDestId.toString()) {
        return toast.error("L'entrepôt source et destination doivent être différents");
    }

    const payload = {
      warehouseSource: { id: sourceId },
      warehouseDest: { id: parseInt(transferDestId) },
      lines: transferLines.map(l => ({
        stockItem: { id: l.stockItemId },
        quantityRequested: l.qty
      }))
    };
    submitTransferMut.mutate(payload);
  };

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

    const payload = {
      designation: items[0]?.itemName || 'Nouvelle Demande', 
      quantite: items.reduce((acc, it) => acc + it.quantite, 0),
      justification: globalJustification || items[0]?.justification,
      urgence: urgency,
      budgetFamille: { idFamily: budgetFamilleId ? Number(budgetFamilleId) : null },
      budgetSousFamille: { oidSub: budgetSousFamilleId ? Number(budgetSousFamilleId) : null },
      details: items.map(item => ({
        itemName: item.itemName,
        description: item.description,
        quantite: item.quantite,
        justification: item.justification,
        subFamily: { oidSub: budgetSousFamilleId ? Number(budgetSousFamilleId) : null },
        prix_unitaire: 0 
      })),
      submissionToken
    };

    createMutation.mutate(payload);
  };

  const handlePieceSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !selectedPiece) return;

    const payload = {
      designation: selectedPiece.itemName,
      itemCode: selectedPiece.itemCode,
      quantite: pieceQty,
      justification: `Demande de pièce SAV / Maintenance : ${selectedPiece.itemName}`,
      isPieceRechange: true,
      submissionToken: crypto.randomUUID()
    };

    shouldSubmitRef.current = true;
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
    total: daList.length,
    pending: daList.filter((d: any) => !['VALIDE_DG', 'REJETEE'].includes(d.statut)).length,
    valid: daList.filter((d: any) => d.statut === 'VALIDE_DG').length,
    rejected: daList.filter((d: any) => d.statut === 'REJETEE').length,
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

      {/* Tabs */}
      <div className="flex space-x-2 mb-6 bg-white dark:bg-slate-800 p-1 rounded-xl w-max shadow-sm border border-slate-100 dark:border-slate-700">
        <button
          onClick={() => setActiveTab('DA')}
          className={`px-6 py-2 rounded-lg font-semibold text-sm transition-all ${
            activeTab === 'DA' 
              ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300' 
              : 'text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-700/50'
          }`}
        >
          Mes Demandes d'Achat
        </button>
        <button
          onClick={() => setActiveTab('TRANSFERT')}
          className={`px-6 py-2 rounded-lg font-semibold text-sm transition-all ${
            activeTab === 'TRANSFERT' 
              ? 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/40 dark:text-indigo-300' 
              : 'text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-700/50'
          }`}
        >
          Mes Transferts Inter-Sites
        </button>
      </div>

      {/* Action Header */}
      <div className="bg-white dark:bg-slate-800 p-4 rounded-2xl border border-slate-100 dark:border-slate-700 shadow-sm mb-6 flex flex-wrap items-center gap-4">
        <div className="relative flex-1 min-w-[250px]">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">🔍</span>
          <input
            value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Rechercher..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
          />
        </div>
        <div className="flex gap-2">
          {activeTab === 'DA' ? (
            <>
              <button
                onClick={() => setShowChatbot(true)}
                className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 text-white font-bold text-sm hover:from-violet-700 hover:to-indigo-700 transition-all shadow-lg flex items-center gap-2"
              >
                <span>🤖</span> Créer par IA
              </button>
              <button
                onClick={() => { setSelectedPieceCode(''); setPieceQty(1); setShowPieceForm(true); }}
                className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-blue-700 text-white font-bold text-sm hover:from-indigo-700 hover:to-blue-800 transition-all shadow-lg flex items-center gap-2"
              >
                <span>🛠️</span> Demande de Pièces
              </button>
              <button
                onClick={() => { resetForm(); setShowNewForm(true); }}
                className="px-4 py-2.5 rounded-xl bg-blue-600 text-white font-bold text-sm hover:bg-blue-700 transition-all shadow-lg flex items-center gap-2"
              >
                <span>➕</span> Nouvelle DA
              </button>
            </>
          ) : (
            <button
              onClick={() => { setTransferLines([]); setTransferDestId(''); setShowTransferForm(true); }}
              className="px-6 py-2.5 rounded-xl bg-indigo-600 text-white font-bold text-sm hover:bg-indigo-700 transition-all shadow-lg flex items-center gap-2"
            >
              <span>🔄</span> Nouveau Transfert
            </button>
          )}
        </div>
      </div>

      {/* Main Table */}
      {activeTab === 'DA' ? (
        <DaTable
          rows={daList}
          onRowClick={setSelectedDa}
          showRequester={false}
          loading={isLoading}
          searchQuery={search}
          renderExtraBadge={(d: any) => d.isPieceRechange && (
            <span className="ml-2 px-2 py-0.5 text-[9px] font-black bg-rose-100 text-rose-600 border border-rose-200 rounded uppercase">SAV / Maintenance</span>
          )}
          actionLabel={() => 'Voir Détails'}
          showPrice={false}
        />
      ) : (
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 overflow-hidden">
          {isLoadingTransfers ? (
            <div className="p-8 text-center text-slate-500">Chargement des transferts...</div>
          ) : myTransfers.length === 0 ? (
            <div className="p-8 text-center text-slate-500">Aucun transfert inter-sites trouvé.</div>
          ) : (
            <table className="w-full text-left text-sm text-slate-600 dark:text-slate-300">
              <thead className="bg-slate-50 dark:bg-slate-900/50 text-slate-500 dark:text-slate-400 font-semibold border-b border-slate-100 dark:border-slate-700">
                <tr>
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Source → Dest</th>
                  <th className="px-4 py-3">Lignes</th>
                  <th className="px-4 py-3">Statut</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700/50">
                {myTransfers.filter((t:any) => search === '' || t.id.toString().includes(search)).map((t: any) => (
                  <tr key={t.id} className="hover:bg-slate-50 dark:hover:bg-slate-750 transition-colors">
                    <td className="px-4 py-3 font-medium">TRF-{t.id}</td>
                    <td className="px-4 py-3">{new Date(t.createdAt).toLocaleDateString()}</td>
                    <td className="px-4 py-3 font-medium text-slate-900 dark:text-white">
                      {t.warehouseSource?.name} <span className="text-slate-400 mx-1">→</span> {t.warehouseDest?.name}
                    </td>
                    <td className="px-4 py-3">{t.lines?.length || 0} art.</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 rounded text-xs font-bold ${
                        t.status === 'PENDING' ? 'bg-amber-100 text-amber-700' :
                        t.status === 'IN_TRANSIT' ? 'bg-blue-100 text-blue-700' :
                        t.status === 'RECEIVED' ? 'bg-emerald-100 text-emerald-700' :
                        'bg-rose-100 text-rose-700'
                      }`}>
                        {t.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* Detail Modal */}
      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="Détails de la Demande" wide showPrice={false}>
          <div className="space-y-4">
            {selectedDa.statut === 'DISPONIBLE_STOCK' && (
              <div className="bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800 p-4 rounded-2xl flex items-center gap-4 animate-pulse">
                <span className="text-2xl">📦</span>
                <div>
                  <p className="text-sm font-bold text-emerald-800 dark:text-emerald-300">Disponible en Stock (Prêt)</p>
                  <p className="text-xs text-emerald-600 dark:text-emerald-400">Cette pièce est disponible. Veuillez vous présenter au magasinier pour le retrait.</p>
                </div>
              </div>
            )}

            <div className="flex justify-end items-center gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
              {selectedDa.statut === 'BROUILLON' && (
                <button 
                  onClick={() => submitMutation.mutate(selectedDa.id)}
                  disabled={submitMutation.isPending}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-bold text-sm hover:from-blue-700 hover:to-indigo-700 transition-all shadow-lg shadow-blue-100 dark:shadow-none flex items-center gap-2"
                >
                  {submitMutation.isPending ? '...' : <><span>🚀</span> Soumettre la demande</>}
                </button>
              )}
              {(selectedDa.statut === 'PO_CREE' || selectedDa.statut === 'APPROUVEE') && (
                <button 
                onClick={() => handleDownloadPo((selectedDa as any).id_po || selectedDa.id)}
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
          </div>
        </DaModal>
      )}

      {/* Create DA Form Modal - MULTI-ITEMS VERSION */}
      {/* Piece Form Modal */}
      {showPieceForm && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[60] flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-2xl animate-scale-in overflow-hidden">
            <div className="px-8 py-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-indigo-50/50 dark:bg-indigo-900/10">
              <div>
                <h2 className="text-xl font-bold text-slate-800 dark:text-white flex items-center gap-2">🛠️ Demande de Pièces & SAV</h2>
                <p className="text-xs text-slate-400 mt-1">Sélectionnez une pièce dans le catalogue des rayons.</p>
              </div>
              <button onClick={() => setShowPieceForm(false)} className="p-2 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full text-slate-400">✕</button>
            </div>

            <form onSubmit={handlePieceSubmit} className="p-10 space-y-8">
              <div className="space-y-2">
                <label className="text-[11px] font-bold text-slate-400 uppercase tracking-widest ml-1">Pièce / Article *</label>
                <select 
                  value={selectedPieceCode} onChange={e => setSelectedPieceCode(e.target.value)} required
                  className="w-full px-5 py-4 rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm font-medium focus:ring-2 focus:ring-indigo-500 outline-none transition-all shadow-sm"
                >
                  <option value="">Chercher une pièce (Huile, Roue, Batterie...)</option>
                  {stockItems.map((s: any) => (
                    <option key={s.id} value={s.itemCode}>
                      {s.itemName} ({s.itemCode}) — {s.locationCode}
                    </option>
                  ))}
                </select>
              </div>

              {selectedPiece && (
                <div className={`p-4 rounded-2xl border flex items-center gap-4 transition-all ${
                  selectedPiece.quantityAvailable > 0 
                    ? 'bg-emerald-50 border-emerald-100 dark:bg-emerald-900/20 dark:border-emerald-800' 
                    : 'bg-rose-50 border-rose-100 dark:bg-rose-900/20 dark:border-rose-800'
                }`}>
                  <span className="text-2xl">{selectedPiece.quantityAvailable > 0 ? '✅' : '⚠️'}</span>
                  <div>
                    <p className={`text-sm font-bold ${selectedPiece.quantityAvailable > 0 ? 'text-emerald-800 dark:text-emerald-300' : 'text-rose-800 dark:text-rose-300'}`}>
                      {selectedPiece.quantityAvailable > 0 ? `Disponible en Stock (${selectedPiece.quantityAvailable})` : 'Hors Stock (Nécessite Achat)'}
                    </p>
                    <p className="text-xs opacity-70">Emplacement : {selectedPiece.locationCode}</p>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="text-[11px] font-bold text-slate-400 uppercase tracking-widest ml-1">Quantité *</label>
                  <input 
                    type="number" min={1} value={pieceQty} onChange={e => setPieceQty(Number(e.target.value))}
                    className="w-full px-5 py-3.5 rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm font-bold focus:ring-2 focus:ring-indigo-500 outline-none"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[11px] font-bold text-slate-400 uppercase tracking-widest ml-1">Unité</label>
                  <div className="px-5 py-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700 text-sm font-semibold text-slate-500 italic">
                    Pièce / Unité
                  </div>
                </div>
              </div>

              <div className="pt-6 border-t border-slate-50 dark:border-slate-800 flex justify-end gap-3">
                <button 
                  type="button" onClick={() => setShowPieceForm(false)}
                  className="px-8 py-3 rounded-2xl bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-bold text-sm hover:bg-slate-100 transition-all"
                >
                  Annuler
                </button>
                <button 
                  type="submit"
                  disabled={!selectedPieceCode || createMutation.isPending}
                  className="px-10 py-3 rounded-2xl bg-indigo-600 text-white font-black text-sm hover:bg-indigo-700 disabled:opacity-50 shadow-xl shadow-indigo-100 dark:shadow-none transition-all flex items-center gap-2"
                >
                  {createMutation.isPending ? 'Envoi...' : '🚀 Envoyer à l\'Acheteur'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

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
                      <option key={f.idFamily || f.id} value={f.idFamily || f.id}>
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
                      <option key={sf.oidSub || sf.id} value={sf.oidSub || sf.id}>
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
                  Annuler
                </button>
                <div className="flex gap-2">
                  <button 
                    type="submit" form="da-form" 
                    onClick={() => { shouldSubmitRef.current = false; }}
                    disabled={createMutation.isPending || submitMutation.isPending}
                    className="px-6 py-3 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-bold text-sm hover:bg-slate-200 transition-all border border-slate-200 dark:border-slate-700"
                  >
                    💾 Enregistrer Brouillon
                  </button>
                  <button 
                    type="submit" form="da-form"
                    onClick={() => { shouldSubmitRef.current = true; }}
                    disabled={createMutation.isPending || submitMutation.isPending}
                    className="px-10 py-3 rounded-2xl bg-blue-600 text-white font-black text-sm hover:bg-blue-700 disabled:opacity-50 shadow-xl shadow-blue-200 dark:shadow-none transition-all flex items-center gap-2"
                  >
                    {createMutation.isPending || submitMutation.isPending ? 'Traitement...' : '🚀 Soumettre la demande'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {showTransferForm && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[60] flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-4xl flex flex-col overflow-hidden">
            <div className="px-8 py-6 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center bg-indigo-50 dark:bg-indigo-900/20">
              <h2 className="text-xl font-bold text-indigo-700 dark:text-indigo-400">Nouveau Transfert Inter-Sites</h2>
              <button onClick={() => setShowTransferForm(false)} className="text-slate-400 hover:text-slate-600">✕</button>
            </div>
            <form onSubmit={handleSubmitTransfer} className="p-8 space-y-6">
              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-500 uppercase">Entrepôt de destination (Votre entrepôt)</label>
                <select 
                  value={transferDestId} onChange={e => setTransferDestId(e.target.value)} required
                  className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="">Sélectionner une destination...</option>
                  {warehouses.map((w: any) => (
                    <option key={w.id} value={w.id}>
                      {w.name} {w.location ? `- ${w.location}` : ''}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-4">
                <h3 className="font-semibold text-slate-700 dark:text-slate-300">Articles disponibles en stock</h3>
                <div className="max-h-60 overflow-y-auto border border-slate-200 dark:border-slate-700 rounded-xl">
                  <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 dark:bg-slate-800 sticky top-0">
                      <tr>
                        <th className="px-4 py-2 font-semibold">Article</th>
                        <th className="px-4 py-2 font-semibold">Source</th>
                        <th className="px-4 py-2 font-semibold">Disponible</th>
                        <th className="px-4 py-2 font-semibold text-orange-500">Réservé</th>
                        <th className="px-4 py-2 font-semibold w-32">Demander</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                      {availableStock.filter((s:any) => s.quantityAvailable > 0).map((stock: any) => {
                        const line = transferLines.find(l => l.stockItemId === stock.id);
                        return (
                          <tr key={stock.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                            <td className="px-4 py-3 font-medium">{stock.itemCode} - {stock.itemName}</td>
                            <td className="px-4 py-3">{stock.warehouse?.name} {stock.warehouse?.location ? `- ${stock.warehouse.location}` : ''}</td>
                            <td className="px-4 py-3 text-emerald-600 font-bold">{stock.quantityAvailable}</td>
                            <td className="px-4 py-3 text-orange-500 font-bold">{stock.quantityReserved || 0}</td>
                            <td className="px-4 py-3">
                              <input 
                                type="number" min="0" max={stock.quantityAvailable}
                                value={line?.qty || ''}
                                onChange={e => {
                                  const val = parseInt(e.target.value) || 0;
                                  if (val > 0) {
                                    setTransferLines(prev => {
                                      if (prev.length > 0 && prev.some(p => p.warehouseSourceId !== stock.warehouse?.id)) {
                                        toast.error("Règle Métier : Un transfert ne peut provenir que d'un seul site source.");
                                        return prev;
                                      }
                                      const filtered = prev.filter(p => p.stockItemId !== stock.id);
                                      return [...filtered, { stockItemId: stock.id, qty: Math.min(val, stock.quantityAvailable), maxQty: stock.quantityAvailable, itemCode: stock.itemCode, itemName: stock.itemName, warehouseSourceId: stock.warehouse?.id }];
                                    });
                                  } else {
                                    setTransferLines(prev => prev.filter(p => p.stockItemId !== stock.id));
                                  }
                                }}
                                className="w-full p-2 border rounded text-center dark:bg-slate-700 dark:border-slate-600"
                                placeholder="0"
                              />
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
              
              <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex justify-end gap-3">
                <button type="button" onClick={() => setShowTransferForm(false)} className="px-6 py-2 rounded-xl text-slate-600 dark:text-slate-300 font-semibold hover:bg-slate-100 dark:hover:bg-slate-800">Annuler</button>
                <button type="submit" disabled={submitTransferMut.isPending} className="px-8 py-2 rounded-xl bg-indigo-600 text-white font-bold hover:bg-indigo-700">
                  {submitTransferMut.isPending ? 'Envoi...' : 'Soumettre Transfert'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showChatbot && (
        <ChatbotWidget
          onClose={() => setShowChatbot(false)}
          onDaCreated={() => {
            qc.invalidateQueries({
              queryKey: ['da', 'mes-demandes']
            });
            setShowChatbot(false);
          }}
        />
      )}
    </DashboardLayout>
  );
}
