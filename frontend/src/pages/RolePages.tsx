import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoginPage from './LoginPage';
import DemandeurPage from './DemandeurPage';
import AcheteurPage from './AcheteurPage';
import RespAchatPage from './RespAchatPage';
import DafPage from './DafPage';
import DgPage from './DgPage';
import AdminPage from './AdminPage';
import MagasinierPage from './MagasinierPage';
import ComptablePage from './ComptablePage';
import ValidatorPage from './ValidatorPage';

export function RoleProtectedRoute({ roles, children }: { roles: string[], children: React.ReactNode }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" />;
  if (!roles.includes(user.role)) return <Navigate to="/unauthorized" />;

  return <>{children}</>;
}

export function RoleHome() {
  const { user } = useAuth();

  if (!user) return <LoginPage />;

  switch (user.role) {
    case 'EMPLOYE': return <DemandeurPage />;
    case 'ACHETEUR': return <AcheteurPage />;
    case 'RESP_ACHAT': return <RespAchatPage />;
    
    case 'MANAGER_N1': return <ValidatorPage 
      role="MANAGER_N1" 
      myStatut="SOUMISE" 
      title="Dashboard N+1 — Validation Hiérarchique" 
      icon="✅" 
      color="from-violet-500 to-purple-600" />;

    case 'TECHNICIEN': return <ValidatorPage 
      role="TECHNICIEN" 
      myStatut="VALIDE_N1" 
      title="Dashboard Technicien — Validation Technique" 
      icon="🔧" 
      color="from-cyan-500 to-teal-600" />;

    case 'AMG': return <ValidatorPage 
      role="AMG" 
      myStatut="VALIDE_ACHETEUR" 
      title="Dashboard AMG — Validation Intermédiaire" 
      icon="📂" 
      color="from-orange-500 to-amber-600" />;

    case 'DAF': return <DafPage />;
    case 'DG': return <DgPage />;
    case 'ADMINISTRATEUR': return <AdminPage />;
    case 'MAGASINIER': return <MagasinierPage />;
    case 'COMPTABLE': return <ComptablePage />;
    default: return <Navigate to="/login" />;
  }
}
