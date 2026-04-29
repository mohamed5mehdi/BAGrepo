import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ValidatorPage from './ValidatorPage';

export const N1Page  = () => <ValidatorPage role="MANAGER_N1"  myStatut="SOUMISE"    title="Dashboard N+1 — Validation Hiérarchique"  icon="✅" color="from-violet-500 to-purple-600" />;
export const TechPage = () => <ValidatorPage role="TECHNICIEN"  myStatut="VALIDEE_N1"  title="Dashboard Technicien — Validation Technique" icon="🔧" color="from-cyan-500 to-teal-600" />;
export const AmgPage  = () => <ValidatorPage role="AMG"         myStatut="EN_VALIDATION_AMG"   title="Dashboard AMG — Validation Intermédiaire"   icon="📂" color="from-orange-500 to-amber-600" />;

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}
