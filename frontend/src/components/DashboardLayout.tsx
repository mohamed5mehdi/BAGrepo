import Sidebar from './Sidebar';
import Topbar from './Topbar';
import ChatbotWidget from './ChatbotWidget';
import { useAuth } from '../context/AuthContext';

interface Props {
  title: string;
  pendingCount?: number;
  children: React.ReactNode;
}

export default function DashboardLayout({ title, pendingCount, children }: Props) {
  const { user } = useAuth();
  const showBiButton = user?.role === 'DAF' || user?.role === 'DG' || user?.role === 'ADMINISTRATEUR';

  return (
    <div className="flex h-screen bg-slate-50 dark:bg-slate-950 font-sans overflow-hidden">
      <Sidebar />
      <div className="flex-1 flex flex-col overflow-hidden">
        <Topbar title={title} pendingCount={pendingCount} />
        <main className="flex-1 overflow-y-auto p-6 relative">
          {children}
          <ChatbotWidget />
          
          {/* ── Global Floating BI Dashboard Button ── */}
          {showBiButton && (
            <button
              onClick={() => window.location.href='/ai-dashboard'}
              className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 px-8 py-3 rounded-full bg-gradient-to-r from-indigo-600 to-violet-600 text-white font-black text-sm shadow-[0_0_40px_rgba(79,70,229,0.5)] hover:scale-105 transition-all border border-indigo-400/50 flex items-center gap-3 animate-bounce"
            >
              <span className="text-xl">📊</span>
              OUVRIR LE TABLEAU DE BORD BI (IA)
            </button>
          )}

        </main>
      </div>
    </div>
  );
}

