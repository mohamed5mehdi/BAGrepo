import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import DemandeurPage from './pages/DemandeurPage';
import AcheteurPage from './pages/AcheteurPage';
import AdminPage from './pages/AdminPage';
import LogisticsPage from './pages/LogisticsPage';
import ValidatorPage from './pages/ValidatorPage';
import { N1Page, TechPage, AmgPage, ProtectedRoute } from './pages/RolePages';

const qc = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={qc}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<Navigate to="/login" replace />} />

            <Route path="/demandeur"     element={<ProtectedRoute><DemandeurPage /></ProtectedRoute>} />
            <Route path="/demandeur/new" element={<ProtectedRoute><DemandeurPage /></ProtectedRoute>} />
            <Route path="/n1"            element={<ProtectedRoute><N1Page /></ProtectedRoute>} />
            <Route path="/tech"          element={<ProtectedRoute><TechPage /></ProtectedRoute>} />
            <Route path="/acheteur"      element={<ProtectedRoute><AcheteurPage /></ProtectedRoute>} />
            <Route path="/amg"           element={<ProtectedRoute><AmgPage /></ProtectedRoute>} />
            <Route path="/daf"           element={<ProtectedRoute><ValidatorPage role="DAF" myStatut="EN_VALIDATION_DAF" title="Dashboard DAF — Contrôle Budgétaire" icon="💰" color="from-fuchsia-500 to-purple-700" /></ProtectedRoute>} />
            <Route path="/dg"            element={<ProtectedRoute><ValidatorPage role="DG" myStatut="EN_VALIDATION_DG" title="Dashboard DG — Direction Générale" icon="🏢" color="from-slate-600 to-slate-800" /></ProtectedRoute>} />
            <Route path="/admin"         element={<ProtectedRoute><AdminPage /></ProtectedRoute>} />
            <Route path="/logistics"     element={<ProtectedRoute><LogisticsPage /></ProtectedRoute>} />

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


