interface Props {
  label: string;
  value: number | string;
  icon: string;
  color: string; // tailwind gradient class
  sub?: string;
  onClick?: () => void;
  active?: boolean;
  id?: string;
}

export default function KpiCard({ label, value, icon, color, sub, onClick, active, id }: Props) {
  return (
    <button 
      id={id}
      onClick={onClick}
      disabled={!onClick}
      className={`relative overflow-hidden rounded-2xl bg-white dark:bg-slate-800 border transition-all p-5 animate-fade-in-up text-left w-full
        ${onClick ? 'cursor-pointer hover:shadow-lg hover:scale-[1.02] active:scale-95' : 'cursor-default'}
        ${active ? 'border-blue-500 ring-2 ring-blue-500/20 shadow-md' : 'border-slate-100 dark:border-slate-700 shadow-sm'}
      `}
    >

      <div className={`absolute top-0 right-0 w-24 h-24 rounded-bl-full bg-gradient-to-br ${color} opacity-10`} />
      <div className="flex items-start gap-4">
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-2xl bg-gradient-to-br ${color} shadow-lg shadow-indigo-100/50 dark:shadow-slate-900/50`}>
          {icon}
        </div>
        <div>
          <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wide">{label}</p>
          <p className="text-3xl font-bold text-slate-800 dark:text-white mt-0.5">{value}</p>
          {sub && <p className="text-xs text-slate-400 mt-0.5">{sub}</p>}
        </div>
      </div>
    </button>
  );
}
