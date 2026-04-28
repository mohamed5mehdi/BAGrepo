import { useAuth } from '../context/AuthContext';
import { ROLE_LABELS } from '../utils/constants';

interface Props { title: string; pendingCount?: number; }

export default function Topbar({ title, pendingCount = 0 }: Props) {
  const { user } = useAuth();

  return (
    <header className="sticky top-0 z-20 bg-white/80 dark:bg-slate-900/80 backdrop-blur-sm border-b border-slate-200 dark:border-slate-700 px-6 py-3 flex items-center justify-between">
      <h1 className="text-lg font-semibold text-slate-800 dark:text-white">{title}</h1>
      <div className="flex items-center gap-4">
        {/* Notification badge */}
        <button className="relative p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors text-slate-500">
          🔔
          {pendingCount > 0 && (
            <span className="absolute -top-0.5 -right-0.5 w-5 h-5 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">
              {pendingCount > 9 ? '9+' : pendingCount}
            </span>
          )}
        </button>
        {/* User pill */}
        {user && (
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-100 dark:bg-slate-800 text-sm">
            <span className="font-medium text-slate-700 dark:text-slate-200">{user.userName}</span>
            <span className="text-xs text-slate-400">·</span>
            <span className="text-xs text-slate-500 dark:text-slate-400">{ROLE_LABELS[user.role]}</span>
          </div>
        )}
      </div>
    </header>
  );
}
