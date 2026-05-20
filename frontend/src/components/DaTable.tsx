import type { DemandeAchatInterne } from '../types';
import { formatDA, formatCurrency } from '../utils/constants';
import StatusBadge from './StatusBadge';

interface Props {
  rows: DemandeAchatInterne[];
  onRowClick: (da: DemandeAchatInterne) => void;
  showRequester?: boolean;
  actionLabel?: (da: DemandeAchatInterne) => string;
  loading?: boolean;
  searchQuery?: string;
  showPrice?: boolean;
  renderExtraBadge?: (da: DemandeAchatInterne) => React.ReactNode;
}

export default function DaTable({
  rows, onRowClick, showRequester = true,
  actionLabel, loading, searchQuery = '',
  showPrice = true, renderExtraBadge
}: Props) {

  const filtered = rows.filter(da => {
    const q = searchQuery.toLowerCase();
    if (!q) return true;
    return (
      da.designation?.toLowerCase().includes(q) ||
      formatDA(da.id).toLowerCase().includes(q) ||
      da.demandeur?.nom?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="bg-white dark:bg-slate-800 rounded-2xl border border-slate-100 dark:border-slate-700 shadow-sm overflow-hidden animate-fade-in-up">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50 dark:bg-slate-900/60 border-b border-slate-100 dark:border-slate-700">
              <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Code</th>
              <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Objet</th>
              {showRequester && (
                <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Demandeur</th>
              )}
              {showPrice && (
                <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Montant Est.</th>
              )}
              <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Statut</th>
              <th className="text-left px-4 py-3 font-semibold text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wide">Date</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr><td colSpan={7} className="text-center py-10 text-slate-400">
                <div className="inline-block w-6 h-6 border-2 border-indigo-400 border-t-transparent rounded-full animate-spin" />
              </td></tr>
            )}
            {!loading && filtered.length === 0 && (
              <tr><td colSpan={7} className="text-center py-10 text-slate-400">
                {searchQuery ? `Aucun résultat pour « ${searchQuery} »` : 'Aucune demande trouvée.'}
              </td></tr>
            )}
            {!loading && filtered.map(da => {
              const total = da.montantEstime || 0;
              const label = actionLabel ? actionLabel(da) : '✏️ Traiter';
              return (
                 <tr
                  key={da.id || (da as any).oid_da}
                  className="da-row border-b border-slate-50 dark:border-slate-700/50 last:border-0"
                  onClick={() => onRowClick(da)}
                >
                   <td className="px-4 py-3">
                    <span className="font-mono font-bold text-indigo-600 dark:text-indigo-400">
                        {formatDA(da.id || (da as any).oid_da)}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-medium text-slate-700 dark:text-slate-200 max-w-[200px] truncate">
                    {da.designation}
                    {renderExtraBadge && renderExtraBadge(da)}
                  </td>
                  {showRequester && (
                    <td className="px-4 py-3 text-slate-500 dark:text-slate-400">{da.demandeur?.nom ?? '—'}</td>
                  )}
                  {showPrice && (
                    <td className="px-4 py-3 font-semibold text-amber-600 dark:text-amber-400">
                      {total > 0 ? formatCurrency(total) : '—'}
                    </td>
                  )}
                  <td className="px-4 py-3"><StatusBadge statut={da.statut} /></td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{da.dateCreation ?? '—'}</td>
                  <td className="px-4 py-3">
                    <button
                      className="px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 dark:bg-indigo-900/30 dark:hover:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 rounded-lg text-xs font-semibold transition-colors"
                      onClick={e => { e.stopPropagation(); onRowClick(da); }}
                    >
                      {label}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
