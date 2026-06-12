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
export function getDashboardRoute(role: string): string {
  const map: Record<string, string> = {
    EMPLOYE:               '/demandeur',
    MANAGER_N1:            '/n1',
    TECHNICIEN:            '/tech',
    ACHETEUR:              '/acheteur',
    ACHETEUR_INFORMATIQUE: '/acheteur',
    ACHETEUR_BUREAUTIQUE:  '/acheteur',
    ACHETEUR_MOBILIER:     '/acheteur',
    ACHETEUR_CONSOMMABLE:  '/acheteur',
    ACHETEUR_AUTRE:        '/acheteur',
    AMG:                   '/amg',
    DAF:                   '/daf',
    DG:                    '/dg',
    ADMINISTRATEUR:        '/admin',
    MAGASINIER:            '/magasinier',
    MAGASINIER_DEST:       '/magasinier/dest',
    MAGASINIER_CASA:       '/magasinier/casa',
    MAGASINIER_RABAT:      '/magasinier/rabat',
    MAGASINIER_TANGER:     '/magasinier/tanger',
    MAGASINIER_MARRAKECH:  '/magasinier/marrakech',
    COMPTABLE:             '/comptable',
    RESP_ACHAT:            '/resp-achat',
  };
  // Handle case where role might be prefixed with ROLE_
  const cleanRole = role.replace('ROLE_', '');
  return map[cleanRole] ?? '/login';
}
