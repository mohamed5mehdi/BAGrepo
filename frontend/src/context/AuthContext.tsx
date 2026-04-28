import React, { createContext, useContext, useState, useEffect } from 'react';
import type { AuthUser, Role } from '../types';

interface AuthContextType {
  user: AuthUser | null;
  login: (user: AuthUser) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    try {
      const stored = localStorage.getItem('auth_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const login = (u: AuthUser) => {
    setUser(u);
    localStorage.setItem('auth_user', JSON.stringify(u));
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('auth_user');
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

// Util: role → dashboard route
export function getDashboardRoute(role: Role): string {
  const map: Record<Role, string> = {
    ROLE_DEMANDEUR:  '/demandeur',
    ROLE_N1:         '/n1',
    ROLE_TECHNICIEN: '/tech',
    ROLE_ACHETEUR:   '/acheteur',
    ROLE_AMG:        '/amg',
    ROLE_DAF:        '/daf',
    ROLE_DG:         '/dg',
  };
  return map[role] ?? '/login';
}
