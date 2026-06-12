import { useEffect, useRef } from 'react';
import type { DemandeAchatInterne } from '../types';
import { formatDA, formatCurrency } from '../utils/constants';
import StatusBadge from './StatusBadge';
import WorkflowStepper from './WorkflowStepper';

interface Props {
  da: DemandeAchatInterne | null;
  onClose: () => void;
  title?: string;
  children?: React.ReactNode;  // role-specific action panel
  wide?: boolean;
  showPrice?: boolean;
}

export default function DaModal({ da, onClose, title, children, wide = false, showPrice = true }: Props) {

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
  const total = da.montantEstime || 0;

  // Le prix n'est logiquement défini (et donc affichable) qu'après le traitement par l'acheteur.
  // Si rejetée avant l'acheteur, le prix unitaire est nul ou 0.
  const isPricedState = !['BROUILLON', 'SOUMISE', 'VALIDE_N1', 'VALIDE_TECH', 'AFFECTEE'].includes(da.statut)
    && !(da.statut === 'REJETEE' && (!da.prixUnitaire || da.prixUnitaire <= 0));
  const displayPrice = showPrice && isPricedState;

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
            {title ?? '📋 Détail DA'} — <span className="text-indigo-600 font-mono">{formatDA(da.id)}</span>
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
              { label: 'Code DA',    value: formatDA(da.id) },
              { label: 'Date',       value: da.dateCreation ?? '—' },
              { label: 'Objet',      value: da.designation },
              { label: 'Demandeur',  value: da.demandeur?.nom ?? '—' },
              ...(displayPrice ? [{ label: 'Montant HT', value: total > 0 ? formatCurrency(total) : '—' }] : []),
              ...(displayPrice ? [{ label: 'Montant TTC (20%)', value: total > 0 ? formatCurrency(total * 1.20) : '—' }] : []),
              { label: 'Fournisseur', value: da.fournisseur?.nom ?? (da as any).fournisseur_nom ?? '—' },
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

          {/* Details */}
          {da.designation && (
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Lignes de la demande</p>
              <div className="overflow-x-auto rounded-xl border border-slate-100 dark:border-slate-700">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 dark:bg-slate-900/60 text-xs text-slate-500 uppercase">
                    <tr>
                      <th className="px-3 py-2 text-left">Article</th>
                      <th className="px-3 py-2 text-left">Catégorie</th>
                      <th className="px-3 py-2 text-center">Qté</th>
                      {displayPrice && (
                        <>
                          <th className="px-3 py-2 text-right">
                              {['SOUMISE', 'VALIDE_N1', 'VALIDE_TECH'].includes(da.statut) ? 'P.U. EST. (HT)' : 'P.U. Négocié (HT)'}
                          </th>
                          <th className="px-3 py-2 text-right">Total HT</th>
                        </>
                      )}
                    </tr>
                  </thead>
                  <tbody>
                      <tr className="border-t border-slate-50 dark:border-slate-700">
                        <td className="px-3 py-2 font-medium">{da.designation ?? '—'}</td>
                        <td className="px-3 py-2 text-slate-500">{da.categorie ?? '—'}</td>
                        <td className="px-3 py-2 text-center">{da.quantite}</td>
                        {displayPrice && (
                          <>
                            <td className="px-3 py-2 text-right font-mono">{formatCurrency(da.prixUnitaire ?? 0)}</td>
                            <td className="px-3 py-2 text-right font-black text-indigo-600">
                              {formatCurrency((da.quantite ?? 0) * (da.prixUnitaire ?? 0))}
                            </td>
                          </>
                        )}
                      </tr>
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Stepper */}
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Progression du workflow</p>
            <WorkflowStepper statut={da.statut} isPieceRechange={da.isPieceRechange} />
          </div>

          {/* Role-specific action panel */}
          {children}
        </div>
      </div>
    </div>
  );
}
