import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ValidatorPage from './ValidatorPage';
import MagasinierPage from './MagasinierPage';
import ComptablePage from './ComptablePage';

export const N1Page  = () => <ValidatorPage role="MANAGER_N1"  myStatut="SOUMISE"    title="Dashboard N+1 — Validation Hiérarchique"  icon="✅" color="from-violet-500 to-purple-600" />;
export const TechPage = () => <ValidatorPage role="TECHNICIEN"  myStatut="VALIDE_N1"  title="Dashboard Technicien — Validation Technique" icon="🔧" color="from-cyan-500 to-teal-600" />;
export const AmgPage  = () => <ValidatorPage role="AMG"         myStatut="VALIDE_TECH"   title="Dashboard AMG — Validation Intermédiaire"   icon="📂" color="from-orange-500 to-amber-600" />;
export const MagasinierPageWrapper = () => <MagasinierPage />;
export const ComptablePageWrapper = () => <ComptablePage />;

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}
