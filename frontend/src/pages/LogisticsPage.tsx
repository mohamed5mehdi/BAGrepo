import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import DashboardLayout from '../components/DashboardLayout';
import KpiCard from '../components/KpiCard';
import { getPurchaseOrders, createGrn, validateGrn, createGrc, validateGrc, createInvoice, matchInvoice, getSuppliers, getStockItems } from '../api/services';
import type { PurchaseOrder, GrnHeader, GrcHeader, Invoice, Supplier, GrnDetails, GrcDetails } from '../types';
import { formatDA, formatCurrency } from '../utils/constants';

type LogisticsTab = 'PO' | 'GRN' | 'GRC' | 'INVOICE' | 'STOCK';

export default function LogisticsPage() {
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<LogisticsTab>('PO');
  const [selectedPo, setSelectedPo] = useState<PurchaseOrder | null>(null);
  
  // Data
  const { data: pos = [], isLoading: loadingPo } = useQuery({ queryKey: ['pos'], queryFn: () => getPurchaseOrders().then(r => r.data) });
  const { data: stocks = [], isLoading: loadingStock } = useQuery({ 
    queryKey: ['stocks'], 
    queryFn: () => getStockItems().then(r => r.data),
    enabled: activeTab === 'STOCK'
  });
  const { data: suppliers = [] } = useQuery({ queryKey: ['suppliers'], queryFn: () => getSuppliers().then(r => r.data) });

  // Mutations
  const grnMutation = useMutation({
    mutationFn: (payload: Partial<GrnHeader>) => createGrn(payload),
    onSuccess: (res) => {
      validateGrn(res.data.id).then(() => {
        toast.success('✅ Réception validée et stock mis à jour !');
        qc.invalidateQueries({ queryKey: ['pos'] });
        setSelectedPo(null);
        setActiveTab('GRN');
      });
    }
  });

  const grcMutation = useMutation({
    mutationFn: (payload: Partial<GrcHeader>) => createGrc(payload),
    onSuccess: (res) => {
      validateGrc(res.data.id).then(() => {
        toast.success('💰 Valorisation financière effectuée !');
        setActiveTab('GRC');
      });
    }
  });

  const invoiceMutation = useMutation({
    mutationFn: (payload: Partial<Invoice>) => createInvoice(payload),
    onSuccess: (res) => {
      matchInvoice(res.data.id).then(() => {
        toast.success('📑 Facture rapprochée et approuvée !');
        setActiveTab('INVOICE');
      });
    }
  });

  const handleCreateGrn = (po: PurchaseOrder) => {
    const payload: Partial<GrnHeader> = {
      purchaseOrder: { id_po: po.id_po } as any,
      supplier: po.daHeader?.details?.[0]?.fournisseur || undefined,
      deliveryNoteNumber: `BL-${Math.floor(Math.random()*10000)}`,
      receiptDate: new Date().toISOString().split('T')[0],
      status: 'DRAFT',
      details: po.daHeader?.details?.map(d => ({
        itemCode: d.itemCode || 'ART-BAG',
        itemName: d.itemName || d.description,
        orderedQuantity: d.quantite,
        receivedQuantity: d.quantite,
        acceptedQuantity: d.quantite,
        qualityStatus: 'APPROVED'
      })) as any
    };
    grnMutation.mutate(payload);
  };

  const handleCreateGrc = (po: PurchaseOrder) => {
    const payload: Partial<GrcHeader> = {
      grnHeader: { id: po.id_po } as any, 
      costingDate: new Date().toISOString().split('T')[0],
      status: 'VALIDATED',
      totalAmount: po.montant_total,
      devise: 'MAD',
      details: po.daHeader?.details?.map(d => ({
        itemCode: d.itemCode,
        acceptedQuantity: d.quantite,
        unitCost: Number(d.prix_unitaire)
      })) as any
    };
    grcMutation.mutate(payload);
  };

  const handleCreateInvoice = (po: PurchaseOrder) => {
    const payload: Partial<Invoice> = {
      purchaseOrder: { id_po: po.id_po } as any,
      invoiceNumber: `INV-${po.id_po}-${Math.floor(Math.random()*1000)}`,
      invoiceDate: new Date().toISOString().split('T')[0],
      montantHT: po.montant_total / 1.20,
      montantTTC: po.montant_total,
      status: 'RECEIVED'
    };
    invoiceMutation.mutate(payload);
  };

  return (
    <DashboardLayout title="Logistique & Finance — Post-Achat">
      {/* Tabs */}
      <div className="flex gap-1 p-1 bg-slate-100 dark:bg-slate-800 rounded-2xl mb-8 w-fit">
        {(['PO', 'GRN', 'GRC', 'INVOICE', 'STOCK'] as LogisticsTab[]).map(t => (
          <button
            key={t}
            onClick={() => setActiveTab(t)}
            className={`px-6 py-2.5 rounded-xl text-sm font-bold transition-all ${activeTab === t ? 'bg-white dark:bg-slate-700 text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
          >
            {t === 'PO' ? '📦 Bons de Commande' : t === 'GRN' ? '🚚 Réceptions' : t === 'GRC' ? '💰 Valorisation' : t === 'STOCK' ? '📦 Stock' : '📑 Factures'}
          </button>
        ))}
      </div>

      <div className="space-y-6">
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-xl overflow-hidden">
          {activeTab !== 'STOCK' ? (
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
                <tr>
                  <th className="px-6 py-4">{activeTab === 'PO' ? 'N° PO' : activeTab === 'GRN' ? 'N° Réception' : 'N° Document'}</th>
                  <th className="px-6 py-4">Fournisseur</th>
                  <th className="px-6 py-4">Montant</th>
                  <th className="px-6 py-4">Statut</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                {pos.map(po => (
                  <tr key={po.id_po} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4 font-mono font-bold text-blue-600">
                      {activeTab === 'PO' ? `PO-${po.id_po}` : activeTab === 'GRN' ? `GRN-${po.id_po}` : `DOC-${po.id_po}`}
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-700 dark:text-slate-200">
                      {po.daHeader?.details?.[0]?.fournisseur?.nom ?? '—'}
                    </td>
                    <td className="px-6 py-4 font-bold text-slate-900 dark:text-white">
                      {formatCurrency(po.montant_total)}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${
                        activeTab === 'PO' ? 'bg-blue-50 text-blue-600' : 
                        activeTab === 'GRN' ? 'bg-amber-50 text-amber-600' :
                        'bg-emerald-50 text-emerald-600'
                      }`}>
                        {activeTab === 'PO' ? po.statut : 'TRAITÉ'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      {activeTab === 'PO' && (
                        <button onClick={() => handleCreateGrn(po)} className="px-4 py-2 rounded-xl bg-blue-600 text-white text-xs font-bold hover:bg-blue-700 transition-all shadow-lg shadow-blue-100">
                          🚚 Réceptionner
                        </button>
                      )}
                      {activeTab === 'GRN' && (
                        <button onClick={() => handleCreateGrc(po)} className="px-4 py-2 rounded-xl bg-amber-500 text-white text-xs font-bold hover:bg-amber-600 transition-all">
                          💰 Valoriser
                        </button>
                      )}
                      {activeTab === 'GRC' && (
                        <button onClick={() => handleCreateInvoice(po)} className="px-4 py-2 rounded-xl bg-emerald-500 text-white text-xs font-bold hover:bg-emerald-600 transition-all">
                          📑 Facturer
                        </button>
                      )}
                      {activeTab === 'INVOICE' && (
                        <span className="text-emerald-500 font-black text-xs">✅ TERMINÉ</span>
                      )}
                    </td>
                  </tr>
                ))}
                {pos.length === 0 && (
                  <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Aucun élément à afficher dans cet onglet.</td></tr>
                )}
              </tbody>
            </table>
          ) : (
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-50 dark:bg-slate-800 text-slate-500 font-bold uppercase text-[10px] tracking-widest">
                <tr>
                  <th className="px-6 py-4">Code Article</th>
                  <th className="px-6 py-4">Désignation</th>
                  <th className="px-6 py-4">Quantité Disponible</th>
                  <th className="px-6 py-4">Coût Unitaire (Valo)</th>
                  <th className="px-6 py-4 text-right">Valeur Totale</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                {stocks.map(s => (
                  <tr key={s.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4 font-mono font-bold text-slate-700 dark:text-slate-200">{s.itemCode}</td>
                    <td className="px-6 py-4 font-semibold">{s.itemName}</td>
                    <td className="px-6 py-4 font-bold text-blue-600">{s.quantityAvailable}</td>
                    <td className="px-6 py-4 font-bold text-emerald-600">
                      {s.unitCost ? formatCurrency(s.unitCost) : '—'}
                    </td>
                    <td className="px-6 py-4 text-right font-black">
                      {s.unitCost ? formatCurrency(s.unitCost * s.quantityAvailable) : '—'}
                    </td>
                  </tr>
                ))}
                {stocks.length === 0 && (
                  <tr><td colSpan={5} className="px-6 py-20 text-center text-slate-400 italic">Le stock est vide. Réceptionnez des articles (GRN) pour les voir ici.</td></tr>
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
