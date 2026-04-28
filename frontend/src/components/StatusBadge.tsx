import { STATUS_LABELS, STATUS_COLORS } from '../utils/constants';
import type { StatutDA } from '../types';

interface Props { statut: StatutDA; }

export default function StatusBadge({ statut }: Props) {
  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold ${STATUS_COLORS[statut] ?? 'bg-gray-100 text-gray-700'}`}>
      {STATUS_LABELS[statut] ?? statut}
    </span>
  );
}
