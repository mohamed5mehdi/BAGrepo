import React from 'react';
import TransferDashboard from '../components/TransferDashboard';

export default function MagasinierCasaPage() {
  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors">
      <div className="bg-gradient-to-r from-blue-700 to-blue-900 px-6 py-8 shadow-lg">
        <h1 className="text-3xl font-black text-white tracking-tight">Espace Magasinier — Casablanca</h1>
        <p className="text-blue-100 mt-2 font-medium">Gestion des stocks et transferts inter-sites de l'entrepôt de Casablanca.</p>
      </div>
      <TransferDashboard />
    </div>
  );
}
