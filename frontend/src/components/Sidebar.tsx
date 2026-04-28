import { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ROLE_LABELS, ROLE_COLORS } from '../utils/constants';
import type { Role } from '../types';

const NAV_BY_ROLE: Record<Role, { label: string; icon: string; to: string }[]> = {
  ROLE_DEMANDEUR: [
    { label: 'Mes Demandes', icon: '📋', to: '/demandeur' },
    { label: 'Nouvelle DA',  icon: '➕', to: '/demandeur/new' },
  ],
  ROLE_N1:        [{ label: 'À Valider', icon: '✅', to: '/n1' }],
  ROLE_TECHNICIEN:[{ label: 'À Valider', icon: '🔧', to: '/tech' }],
  ROLE_ACHETEUR:  [
    { label: 'Tableau de bord', icon: '🛒', to: '/acheteur' },
  ],
  ROLE_AMG:       [{ label: 'Dossiers AMG', icon: '📂', to: '/amg' }],
  ROLE_DAF:       [{ label: 'Contrôle Budget', icon: '💰', to: '/daf' }],
  ROLE_DG:        [{ label: 'Direction', icon: '🏢', to: '/dg' }],
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
      {/* Header */}
      <div className={`flex items-center gap-3 px-4 py-5 border-b border-slate-700/60 ${collapsed ? 'justify-center' : ''}`}>
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
        {navItems.map(item => (
          <NavLink
            key={item.to}
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
        ))}
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
