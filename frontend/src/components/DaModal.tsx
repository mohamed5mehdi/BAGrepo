import { useEffect, useRef } from 'react';
import type { DaHeader } from '../types';
import { formatDA, getDaTotal, formatCurrency } from '../utils/constants';
import StatusBadge from './StatusBadge';
import WorkflowStepper from './WorkflowStepper';

interface Props {
  da: DaHeader | null;
  onClose: () => void;
  title?: string;
  children?: React.ReactNode;  // role-specific action panel
  wide?: boolean;
}

export default function DaModal({ da, onClose, title, children, wide = false }: Props) {
  const ref = useRef<HTMLDivElement>(null);

  // Close on backdrop click
  const handleBackdrop = (e: React.MouseEvent) => {
    if (e.target === ref.current) onClose();
  };

  // Close on Escape
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  if (!da) return null;
  const total = getDaTotal(da);

  return (
    <div
      ref={ref}
      className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in-up"
      onClick={handleBackdrop}
    >
      <div className={`bg-white dark:bg-slate-900 rounded-2xl shadow-2xl flex flex-col max-h-[90vh] ${wide ? 'w-full max-w-4xl' : 'w-full max-w-2xl'}`}>
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-700 flex-shrink-0">
          <h2 className="font-bold text-slate-800 dark:text-white text-lg">
            {title ?? '📋 Détail DA'} — <span className="text-indigo-600 font-mono">{formatDA(da.oid_da)}</span>
          </h2>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-500 transition-colors"
          >✕</button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-5">
          {/* Info grid */}
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
            {[
              { label: 'Code DA',    value: formatDA(da.oid_da) },
              { label: 'Date',       value: da.dateCreation ?? '—' },
              { label: 'Objet',      value: da.objet },
              { label: 'Demandeur',  value: da.demandeur?.nom ?? '—' },
              { label: 'Montant Est.', value: total > 0 ? formatCurrency(total) : '—' },
              { label: 'Statut',     value: <StatusBadge statut={da.statut} /> },
            ].map(({ label, value }) => (
              <div key={label} className="bg-slate-50 dark:bg-slate-800 rounded-xl p-3">
                <p className="text-xs text-slate-400 font-medium mb-1">{label}</p>
                <div className="font-semibold text-slate-700 dark:text-slate-200 text-sm">{value}</div>
              </div>
            ))}
          </div>

          {/* Justification */}
          {da.justification && (
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Justification</p>
              <p className="text-sm text-slate-600 dark:text-slate-400 italic bg-slate-50 dark:bg-slate-800 rounded-xl p-3">
                {da.justification}
              </p>
            </div>
          )}

          {/* Details lines */}
          {da.details && da.details.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Lignes de la demande</p>
              <div className="overflow-x-auto rounded-xl border border-slate-100 dark:border-slate-700">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 dark:bg-slate-900/60 text-xs text-slate-500 uppercase">
                    <tr>
                      <th className="px-3 py-2 text-left">Article</th>
                      <th className="px-3 py-2 text-left">Description</th>
                      <th className="px-3 py-2 text-center">Qté</th>
                      <th className="px-3 py-2 text-right">P.U. HT</th>
                      <th className="px-3 py-2 text-right">Total HT</th>
                    </tr>
                  </thead>
                  <tbody>
                    {da.details.map((d, i) => (
                      <tr key={i} className="border-t border-slate-50 dark:border-slate-700">
                        <td className="px-3 py-2 font-medium">{d.itemName ?? d.description ?? '—'}</td>
                        <td className="px-3 py-2 text-slate-500">{d.description ?? '—'}</td>
                        <td className="px-3 py-2 text-center">{d.quantite}</td>
                        <td className="px-3 py-2 text-right">{formatCurrency(d.prix_unitaire ?? 0)}</td>
                        <td className="px-3 py-2 text-right font-semibold text-amber-600">
                          {formatCurrency((d.quantite ?? 0) * (d.prix_unitaire ?? 0))}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Stepper */}
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Progression du workflow</p>
            <WorkflowStepper statut={da.statut} />
          </div>

          {/* Role-specific action panel */}
          {children}
        </div>
      </div>
    </div>
  );
}
