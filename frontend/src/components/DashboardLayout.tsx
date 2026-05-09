import Sidebar from './Sidebar';
import Topbar from './Topbar';
import ChatbotWidget from './ChatbotWidget';

interface Props {
  title: string;
  pendingCount?: number;
  children: React.ReactNode;
}

export default function DashboardLayout({ title, pendingCount, children }: Props) {
  return (
    <div className="flex h-screen bg-slate-50 dark:bg-slate-950 font-sans overflow-hidden">
      <Sidebar />
      <div className="flex-1 flex flex-col overflow-hidden">
        <Topbar title={title} pendingCount={pendingCount} />
        <main className="flex-1 overflow-y-auto p-6 relative">
          {children}
          <ChatbotWidget />
        </main>
      </div>
    </div>
  );
}

