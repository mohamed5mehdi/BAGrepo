import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth, getDashboardRoute } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import DemandeurPage from './pages/DemandeurPage';
import AcheteurPage from './pages/AcheteurPage';
import AdminPage from './pages/AdminPage';
import MagasinierPage from './pages/MagasinierPage';
import MagasinierDestPage from './pages/MagasinierDestPage';
import ComptablePage from './pages/ComptablePage';
import ValidatorPage from './pages/ValidatorPage';
import RespAchatPage from './pages/RespAchatPage';
import { RoleProtectedRoute } from './pages/RolePages';
import AIDashboardPage from './pages/AIDashboardPage';
import LogistiquePage from './pages/LogistiquePage';

const qc = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
});

function RedirectToRole() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={getDashboardRoute(user.role)} replace />;
}

export default function App() {
  return (
    <QueryClientProvider client={qc}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<RedirectToRole />} />

            <Route path="/demandeur"     element={<RoleProtectedRoute roles={['EMPLOYE']}><DemandeurPage defaultTab="DA" /></RoleProtectedRoute>} />
            <Route path="/demandeur/new" element={<RoleProtectedRoute roles={['EMPLOYE']}><DemandeurPage defaultTab="DA" /></RoleProtectedRoute>} />
            <Route path="/demandeur/transferts" element={<RoleProtectedRoute roles={['EMPLOYE']}><DemandeurPage defaultTab="TRANSFERT" /></RoleProtectedRoute>} />
            <Route path="/n1"            element={<RoleProtectedRoute roles={['MANAGER_N1']}><ValidatorPage role="MANAGER_N1" myStatut="SOUMISE" title="Dashboard N+1 — Validation Hiérarchique" icon="✅" color="from-violet-500 to-purple-600" /></RoleProtectedRoute>} />
            <Route path="/tech"          element={<RoleProtectedRoute roles={['TECHNICIEN']}><ValidatorPage role="TECHNICIEN" myStatut="VALIDE_N1" title="Dashboard Technicien — Validation Technique" icon="🔧" color="from-cyan-500 to-teal-600" /></RoleProtectedRoute>} />
            <Route path="/acheteur"      element={<RoleProtectedRoute roles={['ACHETEUR']}><AcheteurPage /></RoleProtectedRoute>} />
            <Route path="/amg"           element={<RoleProtectedRoute roles={['AMG']}><ValidatorPage role="AMG" myStatut="VALIDE_TECH" title="Dashboard AMG — Validation Intermédiaire" icon="📂" color="from-orange-500 to-amber-600" /></RoleProtectedRoute>} />
            <Route path="/daf"           element={<RoleProtectedRoute roles={['DAF']}><ValidatorPage role="DAF" myStatut="VALIDE_AMG" title="Dashboard DAF — Contrôle Budgétaire" icon="💰" color="from-fuchsia-500 to-purple-700" /></RoleProtectedRoute>} />
            <Route path="/dg"            element={<RoleProtectedRoute roles={['DG']}><ValidatorPage role="DG" myStatut="VALIDE_DG" title="Dashboard DG — Direction Générale" icon="🏢" color="from-slate-600 to-slate-800" /></RoleProtectedRoute>} />
            <Route path="/admin"         element={<RoleProtectedRoute roles={['ADMINISTRATEUR']}><AdminPage /></RoleProtectedRoute>} />
            <Route path="/magasinier"    element={<RoleProtectedRoute roles={['MAGASINIER']}><MagasinierPage /></RoleProtectedRoute>} />
            <Route path="/magasinier-dest" element={<RoleProtectedRoute roles={['MAGASINIER_DEST']}><MagasinierDestPage /></RoleProtectedRoute>} />
            <Route path="/comptable"     element={<RoleProtectedRoute roles={['COMPTABLE']}><ComptablePage /></RoleProtectedRoute>} />
            <Route path="/resp-achat"    element={<RoleProtectedRoute roles={['RESP_ACHAT']}><RespAchatPage /></RoleProtectedRoute>} />
            <Route path="/ai-dashboard"  element={<RoleProtectedRoute roles={['DG', 'ADMINISTRATEUR', 'DAF', 'AMG', 'ACHETEUR', 'COMPTABLE', 'RESP_ACHAT']}><AIDashboardPage /></RoleProtectedRoute>} />
            <Route path="/logistics"     element={<RoleProtectedRoute roles={['ACHETEUR', 'ADMINISTRATEUR']}><LogistiquePage /></RoleProtectedRoute>} />

            {/* Catch-all */}
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </BrowserRouter>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: { borderRadius: '12px', fontFamily: 'Outfit, sans-serif', fontSize: '14px' },
            success: { iconTheme: { primary: '#10b981', secondary: '#fff' } },
            error:   { iconTheme: { primary: '#ef4444', secondary: '#fff' } },
          }}
        />
      </AuthProvider>
    </QueryClientProvider>
  );
}


