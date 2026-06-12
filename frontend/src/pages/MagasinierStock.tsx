import { useQuery } from '@tanstack/react-query';
import DashboardLayout from '../components/DashboardLayout';
import { getAvailableStock } from '../api/services';
import { useState } from 'react';

export default function MagasinierStock() {
  const { data: stockItems = [], isLoading } = useQuery({
    queryKey: ['available-stock'],
    queryFn: async () => {
      const res = await getAvailableStock();
      return res.data;
    }
  });

  const [search, setSearch] = useState('');

  const filteredStock = stockItems.filter((item: any) => {
    const q = search.toLowerCase();
    return (
      item.locationCode?.toLowerCase().includes(q) ||
      item.locationName?.toLowerCase().includes(q) ||
      item.itemCode?.toLowerCase().includes(q) ||
      item.itemName?.toLowerCase().includes(q)
    );
  });

  return (
    <DashboardLayout title="Espace Magasin — Vue Stock">
      <div className="max-w-7xl mx-auto space-y-6">
        <header className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white dark:bg-slate-800 p-6 rounded-2xl border border-slate-100 dark:border-slate-700 shadow-sm">
          <div>
            <h1 className="text-2xl font-bold text-slate-800 dark:text-white">Vue Globale du Stock</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Consultez les quantités disponibles sur les 4 sites logistiques.</p>
          </div>
          <div className="flex items-center gap-3">
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">🔍</span>
              <input 
                type="text" 
                placeholder="Rechercher (Code, Site, Article)..." 
                className="pl-9 pr-4 py-2 w-72 rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-900 focus:ring-2 focus:ring-indigo-500 text-sm outline-none dark:text-white transition-all"
                value={search}
                onChange={e => setSearch(e.target.value)}
              />
            </div>
          </div>
        </header>

        <div className="bg-white dark:bg-slate-800 rounded-2xl border border-slate-100 dark:border-slate-700 shadow-sm overflow-hidden animate-fade-in-up">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-50 dark:bg-slate-900/60 border-b border-slate-100 dark:border-slate-700">
                  <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Code Site</th>
                  <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Nom Site</th>
                  <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Code Article</th>
                  <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Désignation</th>
                  <th className="text-right px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Quantité Disponible</th>
                </tr>
              </thead>
              <tbody>
                {isLoading && (
                  <tr><td colSpan={5} className="text-center py-10 text-slate-400">
                    <div className="inline-block w-6 h-6 border-2 border-indigo-400 border-t-transparent rounded-full animate-spin" />
                  </td></tr>
                )}
                {!isLoading && filteredStock.length === 0 && (
                  <tr><td colSpan={5} className="text-center py-10 text-slate-400">Aucun article en stock.</td></tr>
                )}
                {!isLoading && filteredStock.map((item: any, idx: number) => (
                  <tr key={idx} className="border-b border-slate-50 dark:border-slate-700/50 last:border-0 hover:bg-slate-50 dark:hover:bg-slate-700/30 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-slate-500 dark:text-slate-400">{item.locationCode}</td>
                    <td className="px-4 py-3 font-medium text-slate-700 dark:text-slate-200">{item.warehouse?.name || item.locationName || '—'}</td>
                    <td className="px-4 py-3 font-mono font-bold text-indigo-600 dark:text-indigo-400">{item.itemCode}</td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{item.itemName}</td>
                    <td className="px-4 py-3 text-right">
                      <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${item.quantityAvailable > 0 ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'}`}>
                        {item.quantityAvailable}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
