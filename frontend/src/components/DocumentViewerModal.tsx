import React from 'react';
import { formatCurrency } from '../utils/formatters';

interface DocumentViewerModalProps {
  documentType: 'PO' | 'GRN' | 'GRC' | 'INVOICE' | 'LTO' | 'LTI' | 'DEVIS';
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
      
      {data.demandeInterne && (
         <div className="p-4 border border-indigo-100 bg-indigo-50/50 rounded-xl">
             <p className="text-[10px] font-black text-indigo-400 uppercase tracking-widest mb-1">Lié à la Demande d'Achat</p>
             <p className="font-bold text-indigo-900">{data.demandeInterne.objet || data.demandeInterne.designation || 'Demande Interne'}</p>
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
      {data.status && (
         <div className="mt-4 p-4 border border-rose-100 bg-rose-50/50 rounded-xl">
             <p className="text-[10px] font-black text-rose-400 uppercase tracking-widest mb-1">État d'approbation</p>
             <p className="font-bold text-rose-900">{data.status}</p>
         </div>
      )}
    </div>
  );

  const renderLTO = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-start border-b border-slate-100 pb-4">
        <div>
          <h3 className="text-xl font-black text-slate-800">Bon de Sortie (LTO)</h3>
          <p className="text-sm font-bold text-slate-500">{data.ltoNumber || `LTO-${data.id}`}</p>
        </div>
        <div className="text-right">
          <span className="px-3 py-1 bg-amber-100 text-amber-700 font-bold text-xs rounded-lg uppercase">
            {data.status}
          </span>
          <p className="text-xs text-slate-400 mt-2">Expédié le : {data.shippedAt?.substring(0, 10) || '-'}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6 bg-slate-50 p-4 rounded-xl">
        <div>
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Source</p>
          <p className="font-bold text-slate-800">{data.warehouseSource?.name || 'N/A'}</p>
        </div>
        <div className="text-right">
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Destination</p>
          <p className="font-bold text-slate-800">{data.warehouseDest?.name || 'N/A'}</p>
        </div>
      </div>
      
      {data.lines && data.lines.length > 0 && (
         <div className="mt-4">
            <h4 className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-3">Lignes de Transfert</h4>
            <div className="overflow-x-auto rounded-xl border border-slate-100">
               <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 text-slate-500 text-xs">
                     <tr>
                        <th className="p-3">Article</th>
                        <th className="p-3 text-right">Qté Demandée</th>
                        <th className="p-3 text-right">Qté Expédiée</th>
                     </tr>
                  </thead>
                  <tbody>
                     {data.lines.map((d: any, i: number) => (
                        <tr key={i} className="border-t border-slate-50">
                           <td className="p-3 font-medium text-slate-700">{d.stockItem?.itemName || d.stockItem?.itemCode}</td>
                           <td className="p-3 text-right text-slate-500">{d.quantityRequested || '-'}</td>
                           <td className="p-3 text-right font-bold text-amber-600">{d.quantityShipped || '-'}</td>
                        </tr>
                     ))}
                  </tbody>
               </table>
            </div>
         </div>
      )}
    </div>
  );

  const renderLTI = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-start border-b border-slate-100 pb-4">
        <div>
          <h3 className="text-xl font-black text-slate-800">Bon d'Entrée (LTI)</h3>
          <p className="text-sm font-bold text-slate-500">{data.ltiNumber || `LTI-${data.id}`}</p>
        </div>
        <div className="text-right">
          <span className="px-3 py-1 bg-teal-100 text-teal-700 font-bold text-xs rounded-lg uppercase">
            {data.status}
          </span>
          <p className="text-xs text-slate-400 mt-2">Reçu le : {data.receivedAt?.substring(0, 10) || '-'}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6 bg-slate-50 p-4 rounded-xl">
        <div>
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Source (Expéditeur)</p>
          <p className="font-bold text-slate-800">{data.warehouseSource?.name || 'N/A'}</p>
        </div>
        <div className="text-right">
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Destination (Récepteur)</p>
          <p className="font-bold text-slate-800">{data.warehouseDest?.name || 'N/A'}</p>
        </div>
      </div>
      
      {data.lines && data.lines.length > 0 && (
         <div className="mt-4">
            <h4 className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-3">Lignes de Transfert</h4>
            <div className="overflow-x-auto rounded-xl border border-slate-100">
               <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 text-slate-500 text-xs">
                     <tr>
                        <th className="p-3">Article</th>
                        <th className="p-3 text-right">Qté Expédiée</th>
                        <th className="p-3 text-right">Qté Reçue</th>
                     </tr>
                  </thead>
                  <tbody>
                     {data.lines.map((d: any, i: number) => (
                        <tr key={i} className="border-t border-slate-50">
                           <td className="p-3 font-medium text-slate-700">{d.stockItem?.itemName || d.stockItem?.itemCode}</td>
                           <td className="p-3 text-right text-slate-500">{d.quantityShipped || '-'}</td>
                           <td className="p-3 text-right font-bold text-teal-600">{d.quantityReceived || '-'}</td>
                        </tr>
                     ))}
                  </tbody>
               </table>
            </div>
         </div>
      )}
    </div>
  );

  const renderDEVIS = () => (
    <div className="space-y-6">
      <div className="flex justify-between items-start border-b border-slate-100 pb-4">
        <div>
          <h3 className="text-xl font-black text-slate-800">Devis / Offre</h3>
          <p className="text-sm font-bold text-slate-500">Offre #{data.id}</p>
        </div>
        <div className="text-right">
          <span className="px-3 py-1 bg-blue-100 text-blue-700 font-bold text-xs rounded-lg uppercase">
            REÇU
          </span>
          <p className="text-xs text-slate-400 mt-2">Le : {data.dateOffre?.substring(0, 10) || '-'}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6 bg-slate-50 p-4 rounded-xl">
        <div>
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Fournisseur</p>
          <p className="font-bold text-slate-800">{data.fournisseur?.name || 'N/A'}</p>
          <p className="text-xs text-slate-500 mt-1">Délai: <span className="font-bold text-slate-700">{data.delai || '-'} jours</span></p>
        </div>
        <div className="text-right">
          <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Montant Proposé</p>
          <p className="text-xl font-black text-blue-600">{formatCurrency(data.prixPropose || 0)}</p>
        </div>
      </div>
      
      {data.conditions && (
         <div className="mt-4 p-4 border border-blue-100 bg-blue-50/50 rounded-xl">
             <p className="text-[10px] font-black text-blue-400 uppercase tracking-widest mb-1">Conditions</p>
             <p className="font-medium text-slate-700 whitespace-pre-wrap">{data.conditions}</p>
         </div>
      )}

      {data.demandeInterne && (
         <div className="p-4 border border-indigo-100 bg-indigo-50/50 rounded-xl mt-4">
             <p className="text-[10px] font-black text-indigo-400 uppercase tracking-widest mb-1">Lié à la Demande d'Achat</p>
             <p className="font-bold text-indigo-900">{data.demandeInterne.objet || data.demandeInterne.designation || 'Demande Interne'}</p>
         </div>
      )}
    </div>
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden animate-slide-up">
        
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b border-slate-100 bg-slate-50/50">
          <h2 className="text-lg font-black text-slate-800 uppercase tracking-widest flex items-center gap-3">
            <span className="p-2 bg-indigo-100 text-indigo-600 rounded-lg">📄</span>
            Visionneuse de Document
          </h2>
          <button 
            onClick={onClose}
            className="w-10 h-10 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 sm:p-8">
          {documentType === 'PO' && renderPO()}
          {documentType === 'GRN' && renderGRN()}
          {documentType === 'GRC' && renderGRC()}
          {documentType === 'INVOICE' && renderInvoice()}
          {documentType === 'LTO' && renderLTO()}
          {documentType === 'LTI' && renderLTI()}
          {documentType === 'DEVIS' && renderDEVIS()}
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-slate-100 bg-slate-50 flex justify-end gap-3">
          <button 
            onClick={onClose}
            className="px-6 py-2.5 rounded-xl font-bold text-slate-600 hover:bg-slate-200 transition-colors"
          >
            Fermer
          </button>
          {documentType !== 'DEVIS' && (
            <button 
              onClick={onDownload}
              className="px-6 py-2.5 rounded-xl font-bold text-white bg-indigo-600 hover:bg-indigo-700 shadow-lg shadow-indigo-200 transition-all flex items-center gap-2"
            >
              ⬇️ Télécharger PDF
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
