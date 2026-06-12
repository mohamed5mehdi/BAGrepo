import React from 'react';
import TransferDashboard from '../components/TransferDashboard';

export default function MagasinierRabatPage() {
  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors">
      <div className="bg-gradient-to-r from-emerald-600 to-emerald-800 px-6 py-8 shadow-lg">
        <h1 className="text-3xl font-black text-white tracking-tight">Espace Magasinier — Rabat</h1>
        <p className="text-emerald-100 mt-2 font-medium">Gestion des stocks et transferts inter-sites de l'entrepôt de Rabat.</p>
      </div>
      <TransferDashboard />
    </div>
  );
}
