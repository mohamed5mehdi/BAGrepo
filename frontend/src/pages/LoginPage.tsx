import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth, getDashboardRoute } from '../context/AuthContext';
import { login as loginApi } from '../api/services';
import toast from 'react-hot-toast';
import type { Role } from '../types';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail]     = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await loginApi(email.trim().toLowerCase(), password);
      if (data.success) {
        login({ userId: data.userId, userName: data.userName, email: data.email, role: data.role as Role });
        toast.success(`Bienvenue, ${data.userName} !`);
        navigate(getDashboardRoute(data.role as Role));
      } else {
        toast.error(data.message ?? 'Identifiants incorrects');
      }
    } catch (err: any) {
      if (err.response && err.response.data && err.response.data.message) {
        toast.error(err.response.data.message);
      } else if (err.response && err.response.status === 401) {
        toast.error('Identifiants incorrects');
      } else {
        toast.error('Erreur de connexion au serveur');
      }
    } finally {
      setLoading(false);
    }
  };

  const quickFill = (e: string) => {
    setEmail(e);
    setPassword('password');
  };

  const DEMO_ACCOUNTS = [
    { label: 'Demandeur',  email: 'demandeur@test.com',  icon: '👤', color: 'bg-sky-50 border-sky-200 text-sky-700' },
    { label: 'N+1',        email: 'n1@test.com',         icon: '✅', color: 'bg-violet-50 border-violet-200 text-violet-700' },
    { label: 'Technicien', email: 'tech@test.com',       icon: '🔧', color: 'bg-cyan-50 border-cyan-200 text-cyan-700' },
    { label: 'Acheteur',   email: 'acheteur@test.com',   icon: '🛒', color: 'bg-indigo-50 border-indigo-200 text-indigo-700' },
    { label: 'AMG',        email: 'amg@test.com',        icon: '📂', color: 'bg-orange-50 border-orange-200 text-orange-700' },
    { label: 'DAF',        email: 'daf@test.com',        icon: '💰', color: 'bg-fuchsia-50 border-fuchsia-200 text-fuchsia-700' },
    { label: 'DG',         email: 'dg@test.com',         icon: '🏢', color: 'bg-rose-50 border-rose-200 text-rose-700' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 flex items-center justify-center p-4 font-sans">
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -left-40 w-96 h-96 bg-violet-500/10 rounded-full blur-3xl" />
      </div>

      <div className="relative w-full max-w-md animate-fade-in-up">
        {/* Logo / Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-violet-600 shadow-2xl shadow-indigo-500/30 mb-4">
            <span className="text-3xl">🛒</span>
          </div>
          <h1 className="text-3xl font-bold text-white">GestionsAchat</h1>
          <p className="text-slate-400 mt-1 text-sm">Système de gestion des achats</p>
        </div>

        {/* Login card */}
        <div className="bg-white/10 backdrop-blur-xl border border-white/20 rounded-2xl p-8 shadow-2xl">
          <h2 className="text-xl font-semibold text-white mb-6">Connexion</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">Adresse e-mail</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                autoComplete="email"
                placeholder="prenom@test.com"
                className="w-full px-4 py-2.5 rounded-xl bg-white/10 border border-white/20 text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">Mot de passe</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
                autoComplete="current-password"
                placeholder="••••••••"
                className="w-full px-4 py-2.5 rounded-xl bg-white/10 border border-white/20 text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition text-sm"
              />
            </div>
            <button
              id="btn-login"
              type="submit"
              disabled={loading}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-indigo-500 to-violet-600 text-white font-semibold text-sm hover:from-indigo-600 hover:to-violet-700 transition-all shadow-lg shadow-indigo-500/30 disabled:opacity-60 disabled:cursor-not-allowed mt-2"
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Connexion…
                </span>
              ) : 'Se connecter'}
            </button>
          </form>

          {/* Demo quick-login */}
          <div className="mt-6">
            <p className="text-xs text-slate-400 text-center mb-3 font-medium">— Comptes de démo (cliquez pour remplir) —</p>
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
              {DEMO_ACCOUNTS.map(acc => (
                <button
                  key={acc.email}
                  onClick={() => quickFill(acc.email)}
                  className="flex flex-col items-center gap-1 p-2 rounded-xl bg-white/10 hover:bg-white/20 border border-white/10 text-white text-xs transition-all"
                >
                  <span className="text-lg">{acc.icon}</span>
                  <span className="font-medium">{acc.label}</span>
                </button>
              ))}
            </div>
            <p className="text-xs text-slate-500 text-center mt-2">Mot de passe : <code className="text-slate-300">password</code></p>
          </div>
        </div>
      </div>
    </div>
  );
}
