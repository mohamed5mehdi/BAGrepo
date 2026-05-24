import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import DashboardLayout from '../components/DashboardLayout';
import DocumentViewerModal from '../components/DocumentViewerModal';
import { useAuth } from '../context/AuthContext';
import { 
  getAllPurchaseOrders, getAllGrns, getAllGrcs, getInvoices,
  downloadPoPdf, downloadGrnPdf, downloadGrcPdf, downloadInvoicePdf
} from '../api/services';
import toast from 'react-hot-toast';

type TabType = 'PO' | 'GRN' | 'GRC' | 'INVOICE';

export default function DocumentsPage() {
  const { user } = useAuth();
  
  // Onglets autorisés par rôle
  const allowedTabs: TabType[] = useMemo(() => {
    const role = user?.role || '';
    if (role === 'MAGASINIER') return ['GRN'];
    if (role === 'COMPTABLE') return ['GRN', 'GRC', 'INVOICE'];
    return ['PO', 'GRN', 'GRC', 'INVOICE']; // ACHETEUR, ADMINISTRATEUR
  }, [user]);

  const [activeTab, setActiveTab] = useState<TabType>(allowedTabs[0]);
  const [searchTerm, setSearchTerm] = useState('');
  const [dateFilter, setDateFilter] = useState('');
  const [selectedDoc, setSelectedDoc] = useState<any>(null);
  const [selectedDocType, setSelectedDocType] = useState<TabType | null>(null);

  // Fetch Data
  const { data: pos = [], isLoading: loadPO } = useQuery({
    queryKey: ['docs-po'],
    queryFn: () => getAllPurchaseOrders().then(r => r.data.filter((p: any) => p.statut !== 'DRAFT')),
    enabled: allowedTabs.includes('PO'),
  });

  const { data: grns = [], isLoading: loadGRN } = useQuery({
    queryKey: ['docs-grn'],
    queryFn: () => getAllGrns().then(r => r.data),
    enabled: allowedTabs.includes('GRN'),
  });

  const { data: grcs = [], isLoading: loadGRC } = useQuery({
    queryKey: ['docs-grc'],
    queryFn: () => getAllGrcs().then(r => r.data),
    enabled: allowedTabs.includes('GRC'),
  });

  const { data: invoices = [], isLoading: loadInv } = useQuery({
    queryKey: ['docs-invoices'],
    queryFn: () => getInvoices().then(r => r.data),
    enabled: allowedTabs.includes('INVOICE'),
  });

  // Handler Download
  const handleDownload = async (id: number, type: TabType) => {
    try {
      const loadingToast = toast.loading('Génération du PDF en cours...');
      let res;
      let filename = '';
      if (type === 'PO') { res = await downloadPoPdf(id); filename = `BC_${id}.pdf`; }
      else if (type === 'GRN') { res = await downloadGrnPdf(id); filename = `GRN_${id}.pdf`; }
      else if (type === 'GRC') { res = await downloadGrcPdf(id); filename = `GRC_${id}.pdf`; }
      else if (type === 'INVOICE') { res = await downloadInvoicePdf(id); filename = `Facture_${id}.pdf`; }
      
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      toast.dismiss(loadingToast);
      toast.success('Document téléchargé !');
    } catch (err) {
      toast.dismiss();
      toast.error('Erreur lors du téléchargement du PDF.');
    }
  };

  // Filtrage
  const filterData = (data: any[], dateField: string, searchFields: string[]) => {
    return data.filter(item => {
      // Filtre Date
      if (dateFilter) {
        const itemDate = item[dateField] ? item[dateField].substring(0, 10) : '';
        if (itemDate !== dateFilter) return false;
      }
      // Filtre Recherche
      if (searchTerm) {
        const term = searchTerm.toLowerCase();
        return searchFields.some(field => {
          const val = field.split('.').reduce((o, i) => (o ? o[i] : null), item);
          return val && String(val).toLowerCase().includes(term);
        });
      }
      return true;
    });
  };

  const getFilteredData = () => {
    if (activeTab === 'PO') return filterData(pos, 'date_creation', ['poNumber', 'fournisseur.name', 'statut']);
    if (activeTab === 'GRN') return filterData(grns, 'receiptDate', ['grnNumber', 'deliveryNoteNumber', 'supplier.name', 'status']);
    if (activeTab === 'GRC') return filterData(grcs, 'costingDate', ['grnHeader.grnNumber', 'status']);
    if (activeTab === 'INVOICE') return filterData(invoices, 'invoiceDate', ['invoiceNumber', 'supplier.name', 'status']);
    return [];
  };

  const currentData = getFilteredData();
  const isLoading = loadPO || loadGRN || loadGRC || loadInv;

  return (
    <DashboardLayout title="Centre de Documents & Archives" pendingCount={0}>
      <div className="flex flex-col gap-6 max-w-6xl mx-auto w-full">
        
        {/* En-tête : Onglets et Filtres */}
        <div className="bg-white dark:bg-slate-800 rounded-3xl p-6 shadow-xl shadow-slate-200/50 dark:shadow-none flex flex-col xl:flex-row justify-between items-start xl:items-center gap-4">
          
          <div className="flex gap-2 p-1 bg-slate-100 dark:bg-slate-900 rounded-2xl overflow-x-auto w-full xl:w-auto">
            {allowedTabs.includes('PO') && (
              <button 
                onClick={() => setActiveTab('PO')} 
                className={`px-5 py-2.5 rounded-xl font-bold text-sm transition-all whitespace-nowrap ${activeTab === 'PO' ? 'bg-white dark:bg-slate-800 text-indigo-600 shadow-md' : 'text-slate-500 hover:text-slate-700'}`}
              >
                📜 Bons de Commande (BC)
              </button>
            )}
            {allowedTabs.includes('GRN') && (
              <button 
                onClick={() => setActiveTab('GRN')} 
                className={`px-5 py-2.5 rounded-xl font-bold text-sm transition-all whitespace-nowrap ${activeTab === 'GRN' ? 'bg-white dark:bg-slate-800 text-emerald-600 shadow-md' : 'text-slate-500 hover:text-slate-700'}`}
              >
                📦 Bons de Réception (GRN)
              </button>
            )}
            {allowedTabs.includes('GRC') && (
              <button 
                onClick={() => setActiveTab('GRC')} 
                className={`px-5 py-2.5 rounded-xl font-bold text-sm transition-all whitespace-nowrap ${activeTab === 'GRC' ? 'bg-white dark:bg-slate-800 text-amber-600 shadow-md' : 'text-slate-500 hover:text-slate-700'}`}
              >
                💰 Valorisations (GRC)
              </button>
            )}
            {allowedTabs.includes('INVOICE') && (
              <button 
                onClick={() => setActiveTab('INVOICE')} 
                className={`px-5 py-2.5 rounded-xl font-bold text-sm transition-all whitespace-nowrap ${activeTab === 'INVOICE' ? 'bg-white dark:bg-slate-800 text-rose-600 shadow-md' : 'text-slate-500 hover:text-slate-700'}`}
              >
                🧾 Factures (INV)
              </button>
            )}
          </div>

          <div className="flex gap-3 w-full xl:w-auto">
            <input 
              type="date" 
              value={dateFilter}
              onChange={e => setDateFilter(e.target.value)}
              className="px-4 py-2.5 bg-slate-50 dark:bg-slate-900 border-none rounded-xl text-sm font-medium focus:ring-2 focus:ring-indigo-500"
            />
            <div className="relative flex-1 xl:w-64">
              <span className="absolute left-3 top-2.5 text-slate-400">🔍</span>
              <input 
                type="text" 
                placeholder="Rechercher (Réf, Frns, Statut)..."
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-4 py-2.5 bg-slate-50 dark:bg-slate-900 border-none rounded-xl text-sm font-medium focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
        </div>

        {/* Tableau */}
        <div className="bg-white dark:bg-slate-800 rounded-3xl p-6 shadow-xl shadow-slate-200/50 dark:shadow-none overflow-hidden flex flex-col">
          {isLoading ? (
            <div className="py-20 text-center font-black text-slate-300 animate-pulse text-xl">Chargement des archives...</div>
          ) : currentData.length === 0 ? (
            <div className="py-20 text-center text-slate-400 font-medium">Aucun document trouvé pour ces critères.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b-2 border-slate-100 dark:border-slate-700/50 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                    <th className="p-4">Référence</th>
                    <th className="p-4">Date</th>
                    <th className="p-4">Détails (Fournisseur/Infos)</th>
                    <th className="p-4">Statut</th>
                    <th className="p-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {currentData.map((item, idx) => {
                    // Mapping dynamique selon l'onglet
                    let ref = '', date = '', details = '', status = '', id = 0;
                    if (activeTab === 'PO') {
                      id = item.id_po || item.idPo;
                      ref = item.poNumber || `PO-${id}`;
                      date = item.date_creation;
                      details = item.fournisseur?.name || 'Flux Interne';
                      status = item.statut;
                    } else if (activeTab === 'GRN') {
                      id = item.id;
                      ref = item.grnNumber || `GRN-${id}`;
                      date = item.receiptDate;
                      details = `Fournisseur: ${item.supplier?.name || 'Interne'} - BL: ${item.deliveryNoteNumber || 'N/A'}`;
                      status = item.status;
                    } else if (activeTab === 'GRC') {
                      id = item.id;
                      ref = `GRC-${id}`;
                      date = item.costingDate;
                      details = `Lié au GRN: ${item.grnHeader?.grnNumber || 'N/A'}`;
                      status = item.status;
                    } else if (activeTab === 'INVOICE') {
                      id = item.id;
                      ref = item.invoiceNumber || `INV-${id}`;
                      date = item.invoiceDate;
                      details = `Fournisseur: ${item.supplier?.name || 'Interne'} - ${item.totalAmount} MAD`;
                      status = item.status;
                    }

                    return (
                      <tr key={idx} className="border-b border-slate-50 dark:border-slate-700/30 hover:bg-slate-50/50 dark:hover:bg-slate-700/20 transition-colors">
                        <td className="p-4 font-bold text-slate-800 dark:text-white">{ref}</td>
                        <td className="p-4 text-sm font-medium text-slate-500">{date ? String(date).substring(0, 10) : '-'}</td>
                        <td className="p-4 text-sm text-slate-500 truncate max-w-[200px]">{details}</td>
                        <td className="p-4">
                          <span className="px-3 py-1 bg-slate-100 dark:bg-slate-900 text-slate-600 font-bold text-xs rounded-lg">
                            {status}
                          </span>
                        </td>
                        <td className="p-4 text-right">
                          <button 
                            onClick={() => { setSelectedDoc(item); setSelectedDocType(activeTab); }}
                            className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 text-sm font-bold rounded-xl hover:bg-indigo-100 dark:hover:bg-indigo-900/50 transition-colors"
                          >
                            👀 Visualiser
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

      </div>

      {selectedDoc && selectedDocType && (
        <DocumentViewerModal 
          documentType={selectedDocType}
          data={selectedDoc}
          onClose={() => { setSelectedDoc(null); setSelectedDocType(null); }}
          onDownload={() => {
            const docId = selectedDocType === 'PO' ? (selectedDoc.id_po || selectedDoc.idPo) : selectedDoc.id;
            handleDownload(docId, selectedDocType);
          }}
        />
      )}
    </DashboardLayout>
  );
}
