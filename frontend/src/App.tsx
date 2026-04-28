import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import DemandeurPage from './pages/DemandeurPage';
import AcheteurPage from './pages/AcheteurPage';
import DafPage from './pages/DafPage';
import DgPage from './pages/DgPage';
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
            <Route path="/daf"           element={<ProtectedRoute><DafPage /></ProtectedRoute>} />
            <Route path="/dg"            element={<ProtectedRoute><DgPage /></ProtectedRoute>} />

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
