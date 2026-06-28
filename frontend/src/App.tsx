import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth, getDashboardRoute } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import DemandeurPage from './pages/DemandeurPage';
import AcheteurPage from './pages/AcheteurPage';
import AdminPage from './pages/AdminPage';
import MagasinierPage from './pages/MagasinierPage';
import MagasinierCasaPage from './pages/MagasinierCasaPage';
import MagasinierRabatPage from './pages/MagasinierRabatPage';
import MagasinierTangerPage from './pages/MagasinierTangerPage';
import MagasinierMarrakechPage from './pages/MagasinierMarrakechPage';
import ComptablePage from './pages/ComptablePage';
import ValidatorPage from './pages/ValidatorPage';
import RespAchatPage from './pages/RespAchatPage';
import { RoleProtectedRoute } from './pages/RolePages';
import DafPage from './pages/DafPage';
import DgPage from './pages/DgPage';
import AIDashboardPage from './pages/AIDashboardPage';
import BiDashboardPage from './pages/BiDashboardPage';
import LogistiquePage from './pages/LogistiquePage';
import DocumentsPage from './pages/DocumentsPage';
import MagasinierStock from './pages/MagasinierStock';
import MagasinierTransferIn from './pages/MagasinierTransferIn';
import MagasinierTransferOut from './pages/MagasinierTransferOut';
import MagasinierDocuments from './pages/MagasinierDocuments';
import MagasinierDestPage from './pages/MagasinierDestPage';
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

            <Route path="/demandeur"     element={<RoleProtectedRoute roles={['EMPLOYE', 'ADMINISTRATEUR']}><DemandeurPage /></RoleProtectedRoute>} />
            <Route path="/demandeur/new" element={<RoleProtectedRoute roles={['EMPLOYE', 'ADMINISTRATEUR']}><DemandeurPage /></RoleProtectedRoute>} />
            <Route path="/n1"            element={<RoleProtectedRoute roles={['MANAGER_N1', 'ADMINISTRATEUR']}><ValidatorPage role="MANAGER_N1" myStatut="SOUMISE" title="Dashboard N+1 — Validation Hiérarchique" icon="✅" color="from-violet-500 to-purple-600" /></RoleProtectedRoute>} />
            <Route path="/tech"          element={<RoleProtectedRoute roles={['TECHNICIEN', 'ADMINISTRATEUR']}><ValidatorPage role="TECHNICIEN" myStatut="VALIDE_N1" title="Dashboard Technicien — Validation Technique" icon="🔧" color="from-cyan-500 to-teal-600" /></RoleProtectedRoute>} />
            <Route path="/acheteur"      element={<RoleProtectedRoute roles={['ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'ADMINISTRATEUR']}><AcheteurPage /></RoleProtectedRoute>} />
            <Route path="/amg"           element={<RoleProtectedRoute roles={['AMG', 'ADMINISTRATEUR']}><ValidatorPage role="AMG" myStatut="VALIDE_ACHETEUR" title="Dashboard AMG — Validation Intermédiaire" icon="📂" color="from-orange-500 to-amber-600" /></RoleProtectedRoute>} />
            <Route path="/daf"           element={<RoleProtectedRoute roles={['DAF', 'ADMINISTRATEUR']}><DafPage /></RoleProtectedRoute>} />
            <Route path="/dg"            element={<RoleProtectedRoute roles={['DG', 'ADMINISTRATEUR']}><DgPage /></RoleProtectedRoute>} />
            <Route path="/admin"         element={<RoleProtectedRoute roles={['ADMINISTRATEUR']}><AdminPage /></RoleProtectedRoute>} />
            <Route path="/magasinier"            element={<RoleProtectedRoute roles={['MAGASINIER', 'MAGASINIER_DEST', 'ADMINISTRATEUR']}><MagasinierPage /></RoleProtectedRoute>} />
            <Route path="/magasinier/casa"       element={<RoleProtectedRoute roles={['MAGASINIER_CASA']}><MagasinierCasaPage /></RoleProtectedRoute>} />
            <Route path="/magasinier/rabat"      element={<RoleProtectedRoute roles={['MAGASINIER_RABAT']}><MagasinierRabatPage /></RoleProtectedRoute>} />
            <Route path="/magasinier/tanger"     element={<RoleProtectedRoute roles={['MAGASINIER_TANGER']}><MagasinierTangerPage /></RoleProtectedRoute>} />
            <Route path="/magasinier/marrakech"  element={<RoleProtectedRoute roles={['MAGASINIER_MARRAKECH']}><MagasinierMarrakechPage /></RoleProtectedRoute>} />
            <Route path="/magasinier/dest"        element={<RoleProtectedRoute roles={['MAGASINIER_DEST']}><MagasinierDestPage /></RoleProtectedRoute>} />
            
            <Route path="/magasinier/stock"        element={<RoleProtectedRoute roles={['MAGASINIER', 'MAGASINIER_CASA', 'MAGASINIER_RABAT', 'MAGASINIER_TANGER', 'MAGASINIER_MARRAKECH', 'MAGASINIER_DEST', 'ADMINISTRATEUR']}><MagasinierStock /></RoleProtectedRoute>} />
            <Route path="/magasinier/transfer-in"  element={<RoleProtectedRoute roles={['MAGASINIER', 'MAGASINIER_CASA', 'MAGASINIER_RABAT', 'MAGASINIER_TANGER', 'MAGASINIER_MARRAKECH', 'MAGASINIER_DEST', 'ADMINISTRATEUR']}><MagasinierTransferIn /></RoleProtectedRoute>} />
            <Route path="/magasinier/transfer-out" element={<RoleProtectedRoute roles={['MAGASINIER', 'MAGASINIER_CASA', 'MAGASINIER_RABAT', 'MAGASINIER_TANGER', 'MAGASINIER_MARRAKECH', 'MAGASINIER_DEST', 'ADMINISTRATEUR']}><MagasinierTransferOut /></RoleProtectedRoute>} />
            <Route path="/magasinier/documents"    element={<RoleProtectedRoute roles={['MAGASINIER', 'MAGASINIER_CASA', 'MAGASINIER_RABAT', 'MAGASINIER_TANGER', 'MAGASINIER_MARRAKECH', 'MAGASINIER_DEST', 'ADMINISTRATEUR']}><MagasinierDocuments /></RoleProtectedRoute>} />
            <Route path="/comptable"             element={<RoleProtectedRoute roles={['COMPTABLE', 'ADMINISTRATEUR']}><ComptablePage /></RoleProtectedRoute>} />
            <Route path="/resp-achat"    element={<RoleProtectedRoute roles={['RESP_ACHAT']}><RespAchatPage /></RoleProtectedRoute>} />
            <Route path="/ai-dashboard"  element={<RoleProtectedRoute roles={['DG', 'ADMINISTRATEUR', 'DAF', 'AMG', 'ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'COMPTABLE', 'RESP_ACHAT']}><AIDashboardPage /></RoleProtectedRoute>} />
            <Route path="/bi-dashboard"  element={<RoleProtectedRoute roles={['DG', 'ADMINISTRATEUR', 'DAF', 'AMG', 'ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'COMPTABLE', 'RESP_ACHAT', 'MANAGER_N1', 'DEMANDEUR']}><BiDashboardPage /></RoleProtectedRoute>} />
            <Route path="/logistics"     element={<RoleProtectedRoute roles={['ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'ADMINISTRATEUR']}><LogistiquePage /></RoleProtectedRoute>} />
            <Route path="/documents"     element={<RoleProtectedRoute roles={['ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'ADMINISTRATEUR', 'COMPTABLE', 'MAGASINIER']}><DocumentsPage /></RoleProtectedRoute>} />

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


