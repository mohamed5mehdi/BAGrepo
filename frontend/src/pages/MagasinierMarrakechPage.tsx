import React from 'react';
import TransferDashboard from '../components/TransferDashboard';

export default function MagasinierMarrakechPage() {
  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors">
      <div className="bg-gradient-to-r from-orange-600 to-orange-800 px-6 py-8 shadow-lg">
        <h1 className="text-3xl font-black text-white tracking-tight">Espace Magasinier — Marrakech</h1>
        <p className="text-orange-100 mt-2 font-medium">Gestion des stocks et transferts inter-sites de l'entrepôt de Marrakech.</p>
      </div>
      <TransferDashboard />
    </div>
  );
}
