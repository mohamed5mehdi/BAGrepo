import { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ROLE_LABELS, ROLE_COLORS } from '../utils/constants';
import type { Role } from '../types';




const NAV_BY_ROLE: Record<Role, { label: string; icon: string; to: string; category?: string }[]> = {
  EMPLOYE: [
    { label: 'Mes Demandes', icon: '📋', to: '/demandeur' },
    { label: 'Nouvelle DA',  icon: '➕', to: '/demandeur/new' },
    { label: 'Transferts Stock', icon: '🔄', to: '/demandeur/transferts' },
  ],
  MANAGER_N1:        [{ label: 'À Valider', icon: '✅', to: '/n1' }],
  TECHNICIEN:[{ label: 'À Valider', icon: '🔧', to: '/tech' }],
  ACHETEUR:  [
    { label: 'Tableau de bord', icon: '🛒', to: '/acheteur' },
    { label: 'Logistique', icon: '🚚', to: '/logistics' },
    { label: 'Documents', icon: '📄', to: '/documents' },
  ],
  ACHETEUR_INFORMATIQUE:  [
    { label: 'Tableau de bord', icon: '🛒', to: '/acheteur' },
    { label: 'Logistique', icon: '🚚', to: '/logistics' },
    { label: 'Documents', icon: '📄', to: '/documents' },
  ],
  ACHETEUR_BUREAUTIQUE:  [
    { label: 'Tableau de bord', icon: '🛒', to: '/acheteur' },
    { label: 'Logistique', icon: '🚚', to: '/logistics' },
    { label: 'Documents', icon: '📄', to: '/documents' },
  ],
  ACHETEUR_MOBILIER:  [
    { label: 'Tableau de bord', icon: '🛒', to: '/acheteur' },
    { label: 'Logistique', icon: '🚚', to: '/logistics' },
    { label: 'Documents', icon: '📄', to: '/documents' },
  ],
  ACHETEUR_CONSOMMABLE:  [
    { label: 'Tableau de bord', icon: '🛒', to: '/acheteur' },
    { label: 'Logistique', icon: '🚚', to: '/logistics' },
    { label: 'Documents', icon: '📄', to: '/documents' },
  ],
  ACHETEUR_AUTRE:  [
    { label: 'Tableau de bord', icon: '🛒', to: '/acheteur' },
    { label: 'Logistique', icon: '🚚', to: '/logistics' },
    { label: 'Documents', icon: '📄', to: '/documents' },
  ],
  AMG:       [{ label: 'Dossiers AMG', icon: '📂', to: '/amg' }],
  DAF:       [
    { label: 'Contrôle Budget', icon: '💰', to: '/daf' },
    { label: 'Intelligence IA', icon: '🤖', to: '/ai-dashboard' },
    { label: 'BI Dynamique', icon: '📊', to: '/bi-dashboard' },
  ],
  DG:        [
    { label: 'Direction', icon: '🏢', to: '/dg' },
    { label: 'Dashboard IA', icon: '📊', to: '/ai-dashboard' },
    { label: 'BI Dynamique', icon: '📈', to: '/bi-dashboard' },
  ],
  MAGASINIER: [
    { label: 'Réception GRN', icon: '📦', to: '/magasinier' },
    { label: 'View Stock', icon: '👁️', to: '/magasinier/stock' },
    { label: 'Transfer IN', icon: '📥', to: '/magasinier/transfer-in' },
    { label: 'Transfer OUT', icon: '📤', to: '/magasinier/transfer-out' },
    { label: 'Documents LTO/LTI', icon: '📄', to: '/magasinier/documents' }
  ],
  MAGASINIER_DEST: [
    { label: 'Tableau de bord', icon: '🏠', to: '/magasinier/dest' },
    { label: 'View Stock', icon: '👁️', to: '/magasinier/stock' },
    { label: 'Transfer IN', icon: '📥', to: '/magasinier/transfer-in' },
    { label: 'Transfer OUT', icon: '📤', to: '/magasinier/transfer-out' },
    { label: 'Documents LTO/LTI', icon: '📄', to: '/magasinier/documents' }
  ],
  MAGASINIER_CASA: [
    { label: 'Tableau de bord', icon: '🏠', to: '/magasinier/casa' },
    { label: 'View Stock', icon: '👁️', to: '/magasinier/stock' },
    { label: 'Transfer IN', icon: '📥', to: '/magasinier/transfer-in' },
    { label: 'Transfer OUT', icon: '📤', to: '/magasinier/transfer-out' },
    { label: 'Documents LTO/LTI', icon: '📄', to: '/magasinier/documents' }
  ],
  MAGASINIER_RABAT: [
    { label: 'Tableau de bord', icon: '🏠', to: '/magasinier/rabat' },
    { label: 'View Stock', icon: '👁️', to: '/magasinier/stock' },
    { label: 'Transfer IN', icon: '📥', to: '/magasinier/transfer-in' },
    { label: 'Transfer OUT', icon: '📤', to: '/magasinier/transfer-out' },
    { label: 'Documents LTO/LTI', icon: '📄', to: '/magasinier/documents' }
  ],
  MAGASINIER_TANGER: [
    { label: 'Tableau de bord', icon: '🏠', to: '/magasinier/tanger' },
    { label: 'View Stock', icon: '👁️', to: '/magasinier/stock' },
    { label: 'Transfer IN', icon: '📥', to: '/magasinier/transfer-in' },
    { label: 'Transfer OUT', icon: '📤', to: '/magasinier/transfer-out' },
    { label: 'Documents LTO/LTI', icon: '📄', to: '/magasinier/documents' }
  ],
  MAGASINIER_MARRAKECH: [
    { label: 'Tableau de bord', icon: '🏠', to: '/magasinier/marrakech' },
    { label: 'View Stock', icon: '👁️', to: '/magasinier/stock' },
    { label: 'Transfer IN', icon: '📥', to: '/magasinier/transfer-in' },
    { label: 'Transfer OUT', icon: '📤', to: '/magasinier/transfer-out' },
    { label: 'Documents LTO/LTI', icon: '📄', to: '/magasinier/documents' }
  ],
  COMPTABLE:  [
    { label: 'Facturation GRC', icon: '🧾', to: '/comptable' },
    { label: 'Documents', icon: '📄', to: '/documents' }
  ],
  RESP_ACHAT: [{ label: 'Approbation PO', icon: '🛡️', to: '/resp-achat' }],
  ADMINISTRATEUR:     [
    { category: '⚙️ Paramétrage Global', label: 'Administration', icon: '⚙️', to: '/admin' },

    { category: '🎯 Décisionnel & IA', label: 'Surveillance IA', icon: '🤖', to: '/ai-dashboard' },
    { category: '🎯 Décisionnel & IA', label: 'BI Dynamique', icon: '📊', to: '/bi-dashboard' },
    { category: '📊 Décisionnel & IA', label: 'Documents', icon: '📄', to: '/documents' },

    { category: '💰 Finance', label: 'Facturation GRC', icon: '🧾', to: '/comptable' },

    { category: '👥 Portail Opérationnel', label: 'Mes Demandes', icon: '📋', to: '/demandeur' },
    { category: '👥 Portail Opérationnel', label: 'Nouvelle DA', icon: '➕', to: '/demandeur/new' },

    { category: '🛡️ Validations', label: 'À Valider N+1', icon: '✅', to: '/n1' },
    { category: '🛡️ Validations', label: 'À Valider Tech', icon: '🔧', to: '/tech' },
    { category: '🛡️ Validations', label: 'Dossiers AMG', icon: '📂', to: '/amg' },
    { category: '🛡️ Validations', label: 'Contrôle Budget', icon: '💰', to: '/daf' },
    { category: '🛡️ Validations', label: 'Direction', icon: '🏢', to: '/dg' },

    { category: '🛒 Achats & Logistique', label: 'Acheteur', icon: '🛒', to: '/acheteur' },
    { category: '🛒 Achats & Logistique', label: 'Logistique', icon: '🚚', to: '/logistics' },
    { category: '🛒 Achats & Logistique', label: 'Magasin (Réception)', icon: '📦', to: '/magasinier' },
  ],
};

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);

  if (!user) return null;
  const navItems = NAV_BY_ROLE[user.role] ?? [];
  const gradientClass = ROLE_COLORS[user.role];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside className={`flex flex-col h-screen sticky top-0 bg-slate-900 text-white transition-all duration-300 ${collapsed ? 'w-16' : 'w-60'} flex-shrink-0 z-30`}>
      {/* Logo BAG */}
      {!collapsed ? (
        <div className="flex items-center justify-center px-4 pt-5 pb-3 border-b border-slate-700/60">
          <img
            src="/bag-logo.png"
            alt="Bugshan Automotive Group"
            className="h-12 w-auto object-contain"
            onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
          />
        </div>
      ) : (
        <div className="flex items-center justify-center py-3 border-b border-slate-700/60">
          <span className="text-white font-black text-sm tracking-widest">BAG</span>
        </div>
      )}

      {/* Header – User info */}
      <div className={`flex items-center gap-3 px-4 py-4 border-b border-slate-700/60 ${collapsed ? 'justify-center' : ''}`}>
        <div className={`w-9 h-9 rounded-xl bg-gradient-to-br ${gradientClass} flex items-center justify-center text-lg font-bold flex-shrink-0`}>
          {user.userName.charAt(0).toUpperCase()}
        </div>
        {!collapsed && (
          <div className="overflow-hidden">
            <p className="font-semibold text-sm truncate">{user.userName}</p>
            <p className="text-xs text-slate-400">{ROLE_LABELS[user.role]}</p>
          </div>
        )}
      </div>

      {/* Nav items */}
      <nav className="flex-1 py-4 px-2 flex flex-col gap-1 overflow-y-auto">
        {navItems.map((item, index) => {
          const showCategory = item.category && (index === 0 || navItems[index - 1].category !== item.category);

          return (
            <div key={item.to} className="flex flex-col">
              {showCategory && !collapsed && (
                <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest mt-4 mb-2 px-3">
                  {item.category}
                </span>
              )}
              {showCategory && collapsed && (
                <div className="w-full h-px bg-slate-700/50 my-2" />
              )}
              <NavLink
                to={item.to}
                end
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all
                   ${isActive
                     ? `bg-gradient-to-r ${gradientClass} text-white shadow-lg`
                     : 'text-slate-400 hover:text-white hover:bg-slate-800'
                   }
                   ${collapsed ? 'justify-center' : ''}`
                }
              >
                <span className="text-lg flex-shrink-0">{item.icon}</span>
                {!collapsed && <span>{item.label}</span>}
              </NavLink>
            </div>
          );
        })}
      </nav>

      {/* Footer: collapse toggle + logout */}
      <div className="border-t border-slate-700/60 p-2 flex flex-col gap-1">
        <button
          onClick={() => setCollapsed(c => !c)}
          className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 text-sm transition-all"
        >
          <span className="text-lg">{collapsed ? '→' : '←'}</span>
          {!collapsed && <span>Réduire</span>}
        </button>
        <button
          onClick={handleLogout}
          className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-red-400 hover:text-white hover:bg-red-600/20 text-sm transition-all"
        >
          <span className="text-lg">🚪</span>
          {!collapsed && <span>Déconnexion</span>}
        </button>
      </div>
    </aside>
  );
}
