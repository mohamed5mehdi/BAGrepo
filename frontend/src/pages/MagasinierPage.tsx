import React from 'react';
import TransferDashboard from '../components/TransferDashboard';

export default function MagasinierPage() {
  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors">
      <div className="bg-gradient-to-r from-emerald-700 to-teal-900 px-6 py-8 shadow-lg">
        <h1 className="text-3xl font-black text-white tracking-tight">Espace Magasinier — BAG ERP</h1>
        <p className="text-emerald-100 mt-2 font-medium">Réceptions GRN, transferts inter-sites, gestion des stocks et documents LTO/LTI.</p>
      </div>
      <TransferDashboard />
    </div>
  );
}
