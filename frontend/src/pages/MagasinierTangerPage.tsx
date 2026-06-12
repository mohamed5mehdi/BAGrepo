import React from 'react';
import TransferDashboard from '../components/TransferDashboard';

export default function MagasinierTangerPage() {
  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors">
      <div className="bg-gradient-to-r from-teal-600 to-teal-800 px-6 py-8 shadow-lg">
        <h1 className="text-3xl font-black text-white tracking-tight">Espace Magasinier — Tanger</h1>
        <p className="text-teal-100 mt-2 font-medium">Gestion des stocks et transferts inter-sites de l'entrepôt de Tanger.</p>
      </div>
      <TransferDashboard />
    </div>
  );
}
