import React from 'react';
import { formatCurrency } from '../utils/formatters';

interface DocumentViewerModalProps {
  documentType: 'PO' | 'GRN' | 'GRC' | 'INVOICE';
  data: any;
  onClose: () => void;
  onDownload: () => void;
}

export default function DocumentViewerModal({ documentType, data, onClose, onDownload }: DocumentViewerModalProps) {
  if (!data) return null;

  const renderPO = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-start border-b border-slate-100 pb-4">
        <div>
          <h3 className="text-xl font-black text-slate-800">Bon de Commande</h3>
          <p className="text-sm font-bold text-slate-500">{data.poNumber || `PO-${data.id_po || data.idPo}`}</p>
        </div>
        <div className="text-right">
          <span className="px-3 py-1 bg-slate-100 text-slate-600 font-bold text-xs rounded-lg uppercase">
            {data.statut}
          </span>
          <p className="text-xs text-slate-400 mt-2">Créé le : {data.date_creation?.substring(0, 10)}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6 bg-slate-50 p-4 rounded-xl">
        <div>
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Fournisseur</p>
          <p className="font-bold text-slate-800">{data.fournisseur?.name || data.fournisseur?.nom || 'Interne'}</p>
          {data.fournisseur?.ice && <p className="text-xs text-slate-500">ICE: {data.fournisseur.ice}</p>}
        </div>
        <div className="text-right">
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Montant Total</p>
          <p className="text-xl font-black text-indigo-600">{formatCurrency(data.montantTotal || data.montant_total || 0)}</p>
        </div>
      </div>
      
      {data.daHeader && (
         <div className="p-4 border border-indigo-100 bg-indigo-50/50 rounded-xl">
             <p className="text-[10px] font-black text-indigo-400 uppercase tracking-widest mb-1">Lié à la Demande d'Achat</p>
             <p className="font-bold text-indigo-900">{data.daHeader.objet || data.daHeader.designation || 'Demande Interne'}</p>
         </div>
      )}
    </div>
  );

  const renderGRN = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-start border-b border-slate-100 pb-4">
        <div>
          <h3 className="text-xl font-black text-slate-800">Bon de Réception</h3>
          <p className="text-sm font-bold text-slate-500">{data.grnNumber || `GRN-${data.id}`}</p>
        </div>
        <div className="text-right">
          <span className="px-3 py-1 bg-emerald-100 text-emerald-700 font-bold text-xs rounded-lg uppercase">
            {data.status}
          </span>
          <p className="text-xs text-slate-400 mt-2">Reçu le : {data.receiptDate?.substring(0, 10)}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6 bg-slate-50 p-4 rounded-xl">
        <div>
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Fournisseur</p>
          <p className="font-bold text-slate-800">{data.supplier?.name || data.supplier?.nom || 'Interne'}</p>
          <p className="text-xs text-slate-500 mt-1">BL: <span className="font-bold text-slate-700">{data.deliveryNoteNumber || 'N/A'}</span></p>
        </div>
        <div className="text-right">
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Entrepôt / Magasinier</p>
          <p className="font-bold text-slate-800">{data.warehouse?.name || data.warehouse?.nom || 'Magasin Central'}</p>
          <p className="text-xs text-slate-500">{data.receivedBy?.name || data.receivedBy?.nom || 'Magasinier'}</p>
        </div>
      </div>
      
      {data.details && data.details.length > 0 && (
         <div className="mt-4">
            <h4 className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-3">Lignes de Réception</h4>
            <div className="overflow-x-auto rounded-xl border border-slate-100">
               <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 text-slate-500 text-xs">
                     <tr>
                        <th className="p-3">Article</th>
                        <th className="p-3">Cmd.</th>
                        <th className="p-3">Reçu</th>
                        <th className="p-3">Qualité</th>
                     </tr>
                  </thead>
                  <tbody>
                     {data.details.map((d: any, i: number) => (
                        <tr key={i} className="border-t border-slate-50">
                           <td className="p-3 font-medium text-slate-700">{d.itemName || d.itemCode}</td>
                           <td className="p-3 text-slate-500">{d.orderedQuantity || d.quantityOrdered || '-'}</td>
                           <td className="p-3 font-bold text-emerald-600">{d.receivedQuantity || d.quantityReceived || '-'}</td>
                           <td className="p-3">
                              <span className="px-2 py-1 text-[10px] font-bold rounded bg-slate-100 text-slate-600">
                                 {d.qualityStatus || d.condition || 'GOOD'}
                              </span>
                           </td>
                        </tr>
                     ))}
                  </tbody>
               </table>
            </div>
         </div>
      )}
    </div>
  );

  const renderGRC = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-start border-b border-slate-100 pb-4">
        <div>
          <h3 className="text-xl font-black text-slate-800">Valorisation (GRC)</h3>
          <p className="text-sm font-bold text-slate-500">GRC-{data.id}</p>
        </div>
        <div className="text-right">
          <span className="px-3 py-1 bg-amber-100 text-amber-700 font-bold text-xs rounded-lg uppercase">
            {data.status}
          </span>
          <p className="text-xs text-slate-400 mt-2">Valorisé le : {data.costingDate?.substring(0, 10)}</p>
        </div>
      </div>

      <div className="bg-amber-50/50 p-4 rounded-xl border border-amber-100 flex justify-between items-center">
         <div>
            <p className="text-[10px] font-black text-amber-600 uppercase tracking-widest mb-1">Lié au Bon de Réception</p>
            <p className="font-bold text-amber-900">{data.grnHeader?.grnNumber || 'N/A'}</p>
         </div>
         <div className="text-right">
            <p className="text-[10px] font-black text-amber-600 uppercase tracking-widest mb-1">Montant Valorisé</p>
            <p className="text-xl font-black text-amber-600">{formatCurrency(data.totalAmount || 0)}</p>
         </div>
      </div>
    </div>
  );

  const renderInvoice = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-start border-b border-slate-100 pb-4">
        <div>
          <h3 className="text-xl font-black text-slate-800">Facture Fournisseur</h3>
          <p className="text-sm font-bold text-slate-500">{data.invoiceNumber || `INV-${data.id}`}</p>
        </div>
        <div className="text-right">
          <span className="px-3 py-1 bg-rose-100 text-rose-700 font-bold text-xs rounded-lg uppercase">
            {data.status}
          </span>
          <p className="text-xs text-slate-400 mt-2">Date Facture : {data.invoiceDate?.substring(0, 10)}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6 bg-slate-50 p-4 rounded-xl">
        <div>
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Fournisseur</p>
          <p className="font-bold text-slate-800">{data.supplier?.name || data.supplier?.nom || 'Interne'}</p>
        </div>
        <div className="text-right">
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Montant Total TTC</p>
          <p className="text-xl font-black text-rose-600">{formatCurrency(data.totalAmount || 0)}</p>
        </div>
      </div>
    </div>
  );

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white dark:bg-slate-800 rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[90vh]">
        
        <div className="p-6 overflow-y-auto custom-scrollbar flex-1">
          {documentType === 'PO' && renderPO()}
          {documentType === 'GRN' && renderGRN()}
          {documentType === 'GRC' && renderGRC()}
          {documentType === 'INVOICE' && renderInvoice()}
        </div>

        <div className="p-4 bg-slate-50 dark:bg-slate-900 border-t border-slate-100 dark:border-slate-800 flex justify-end gap-3 rounded-b-3xl">
          <button 
            onClick={onClose}
            className="px-6 py-2.5 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 font-bold text-sm hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors shadow-sm"
          >
            Fermer
          </button>
          <button 
            onClick={onDownload}
            className="px-6 py-2.5 rounded-xl bg-indigo-600 text-white font-black text-sm hover:bg-indigo-700 transition-colors shadow-lg shadow-indigo-200 dark:shadow-none flex items-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
            Télécharger PDF
          </button>
        </div>

      </div>
    </div>
  );
}
