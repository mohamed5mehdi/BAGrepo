import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { getAllDA, getUsers, getFamilies, deleteDA, getSuppliers } from '../api/services';
import type { DaHeader, User, Family } from '../types';
import DashboardLayout from '../components/DashboardLayout';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import KpiCard from '../components/KpiCard';
import { formatDA } from '../utils/constants';

type AdminTab = 'demandes' | 'users' | 'catalog' | 'suppliers';

export default function AdminPage() {
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<AdminTab>('demandes');
  const [statusFilter, setStatusFilter] = useState<string | 'PENDING' | null>(null);
  const [selectedDa, setSelectedDa] = useState<DaHeader | null>(null);

  // Queries
  const { data: das = [], isLoading: loadingDas } = useQuery({
    queryKey: ['admin-das'],
    queryFn: () => getAllDA().then(r => r.data),
  });

  const { data: users = [], isLoading: loadingUsers } = useQuery({
    queryKey: ['admin-users'],
    queryFn: () => getUsers().then(r => r.data),
    enabled: activeTab === 'users',
  });

  const { data: families = [], isLoading: loadingCatalog } = useQuery({
    queryKey: ['admin-catalog'],
    queryFn: () => getFamilies().then(r => r.data),
    enabled: activeTab === 'catalog',
  });

  const { data: suppliers = [], isLoading: loadingSuppliers } = useQuery({
    queryKey: ['admin-suppliers'],
    queryFn: () => getSuppliers().then(r => r.data),
    enabled: activeTab === 'suppliers',
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteDA(id),
    onSuccess: () => {
      toast.success('Suppression réussie');
      qc.invalidateQueries({ queryKey: ['admin-das'] });
      setSelectedDa(null);
    }
  });

  // KPI Calculations
  const kpis = {
    total: das.length,
    pending: das.filter(d => !['PO_CREE', 'REJETEE', 'VALIDEE'].includes(d.statut)).length,
    valid: das.filter(d => d.statut === 'VALIDEE' || d.statut === 'PO_CREE').length,
    rejected: das.filter(d => d.statut === 'REJETEE').length,
  };

  // Advanced Filtering Logic
  const filteredDas = das.filter(da => {
    if (!statusFilter) return true;
    if (statusFilter === 'PENDING') return !['PO_CREE', 'REJETEE', 'VALIDEE'].includes(da.statut);
    if (statusFilter === 'VALIDATED') return da.statut === 'VALIDEE' || da.statut === 'PO_CREE';
    return da.statut === statusFilter;
  });

  return (
    <DashboardLayout title="Administration du Système — Eduka Style">
      <div className="space-y-6">
        
        {/* KPI Dashboard - Interactive Filters */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <KpiCard 
            label="Toutes les Demandes" value={kpis.total} icon="📊" 
            color="from-slate-700 to-slate-900" 
            onClick={() => setStatusFilter(null)}
            active={statusFilter === null}
          />
          <KpiCard 
            label="En Validation" value={kpis.pending} icon="⏳" 
            color="from-amber-500 to-orange-600" 
            onClick={() => setStatusFilter('PENDING')}
            active={statusFilter === 'PENDING'}
          />
          <KpiCard 
            label="Dossiers Validés" value={kpis.valid} icon="✅" 
            color="from-emerald-500 to-teal-600" 
            onClick={() => setStatusFilter('VALIDATED')}
            active={statusFilter === 'VALIDATED'}
          />
          <KpiCard 
            id="filter-rejected"
            label="Demandes Rejetées" value={kpis.rejected} icon="❌" 
            color="from-rose-500 to-red-700" 
            onClick={() => setStatusFilter('REJETEE')}
            active={statusFilter === 'REJETEE'}
          />
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-1 bg-slate-100 dark:bg-slate-900 p-1 rounded-2xl w-fit border border-slate-200 dark:border-slate-800">
          {(['demandes', 'users', 'catalog', 'suppliers'] as const).map(tab => (
            <button
              key={tab}
              type="button"
              onClick={() => setActiveTab(tab)}
              className={`px-6 py-2.5 rounded-xl text-sm font-bold transition-all relative z-10 ${
                activeTab === tab 
                  ? 'bg-white dark:bg-slate-800 text-blue-600 shadow-md ring-1 ring-slate-200 dark:ring-slate-700' 
                  : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-300'
              }`}
            >
              {tab === 'demandes' && '📋 Flux Demandes'}
              {tab === 'users' && '👥 Utilisateurs'}
              {tab === 'catalog' && '📂 Référentiel'}
              {tab === 'suppliers' && '🏢 Fournisseurs'}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="bg-white dark:bg-slate-800 rounded-3xl border border-slate-100 dark:border-slate-700 shadow-sm overflow-hidden min-h-[400px]">
          {activeTab === 'demandes' && (
            <div className="p-6 animate-fade-in">
              <div className="flex justify-between items-center mb-6">
                <div>
                  <h3 className="text-lg font-bold">Flux des Demandes</h3>
                  <p className="text-xs text-slate-400">
                    {statusFilter ? `Filtrage actif : ${statusFilter}` : 'Affichage global de toutes les demandes.'}
                  </p>
                </div>
              </div>
              <DaTable 
                rows={filteredDas as any} 
                loading={loadingDas} 
                onRowClick={(da) => setSelectedDa(da as DaHeader)} 
                actionLabel={() => 'Gérer'}
              />
            </div>
          )}

          {activeTab === 'users' && (
            <div className="p-6 animate-fade-in">
              <h3 className="text-lg font-bold mb-6">Gestion des Utilisateurs</h3>
              {loadingUsers ? (
                <div className="flex justify-center p-10"><div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" /></div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {users.map(u => (
                    <div key={u.oid_user} className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900/50 border border-slate-100 dark:border-slate-800 flex items-center gap-4">
                      <div className="w-12 h-12 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-black text-xl">
                        {u.nom.charAt(0)}
                      </div>
                      <div>
                        <p className="font-bold text-sm">{u.nom}</p>
                        <p className="text-xs text-slate-400">{u.email}</p>
                        <span className="inline-block mt-1 px-2 py-0.5 rounded-full bg-slate-200 dark:bg-slate-800 text-[10px] font-bold text-slate-600 dark:text-slate-400">
                          {u.role}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'catalog' && (
            <div className="p-6 animate-fade-in">
              <h3 className="text-lg font-bold mb-6">Catalogue des Familles</h3>
              {loadingCatalog ? (
                <div className="flex justify-center p-10"><div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" /></div>
              ) : (
                <div className="space-y-3">
                  {families.map(f => (
                    <div key={f.id} className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900/50 border border-slate-100 dark:border-slate-800 flex justify-between items-center">
                      <div>
                        <p className="font-bold">{f.name}</p>
                        <div className="flex gap-4 mt-1">
                          <p className="text-xs text-slate-400">openning_val: <span className="text-slate-600 font-semibold">{(f.budgetTotal || f.budget_initial || 0).toLocaleString()} MAD</span></p>
                          <p className="text-xs text-slate-400">consummed_val: <span className="text-rose-600 font-semibold">{((f.budgetTotal || f.budget_initial || 0) - (f.budgetRestant || f.budget_restant || 0)).toLocaleString()} MAD</span></p>
                          <p className="text-xs text-slate-400">current_val: <span className="text-emerald-600 font-bold">{(f.budgetRestant || f.budget_restant || 0).toLocaleString()} MAD</span></p>
                        </div>
                      </div>
                      <button className="text-blue-600 text-xs font-bold hover:underline">Modifier</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'suppliers' && (
            <div className="p-6 animate-fade-in">
              <h3 className="text-lg font-bold mb-6">Gestion des Fournisseurs</h3>
              {loadingSuppliers ? (
                <div className="flex justify-center p-10"><div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" /></div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {suppliers.map(s => (
                    <div key={s.oidSupplier} className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900/50 border border-slate-100 dark:border-slate-800 shadow-sm">
                      <div className="flex justify-between items-start">
                        <div className="space-y-1">
                          <p className="font-black text-slate-800 dark:text-white flex items-center gap-2">
                             {s.nom}
                             {s.isCertified && <span className="px-1.5 py-0.5 rounded bg-blue-100 text-blue-600 text-[8px] uppercase">Certifié</span>}
                          </p>
                          <div className="flex items-center gap-2">
                            <span className="text-amber-500 text-[10px]">
                                {'★'.repeat(s.rating || 0)}{'☆'.repeat(5 - (s.rating || 0))}
                            </span>
                            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-tighter">Sector: {s.sector}</span>
                          </div>
                          <div className="mt-1">
                             <span className="text-[10px] font-mono text-slate-500 bg-slate-200 dark:bg-slate-700/50 px-1.5 py-0.5 rounded border border-slate-300 dark:border-slate-600">
                               ICE: {s.ice || 'Non renseigné'}
                             </span>
                          </div>
                          <p className="text-xs text-slate-500 font-medium mt-1">{s.contact} • {s.phone || s.email}</p>
                          <p className="text-[10px] text-slate-400 mt-1 italic flex items-center gap-1">
                            <span>📍 {s.adresse}</span>
                            <span>•</span>
                            <span className="text-indigo-500 font-bold">🚚 {s.averageLeadTime} jours</span>
                          </p>
                        </div>
                        <span className="px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-600 text-[10px] font-bold uppercase tracking-wider border border-emerald-100">Actif</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>


      {/* Admin Modal */}
      {selectedDa && (
        <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="Action Administrative" wide>
          <div className="bg-rose-50 dark:bg-rose-900/10 p-6 rounded-2xl border border-rose-100 dark:border-rose-900/30">
            <h4 className="font-bold text-rose-800 dark:text-rose-400 mb-2">Zone de Danger</h4>
            <p className="text-sm text-rose-600/80 dark:text-rose-400/60 mb-4">
              En tant qu'administrateur, vous pouvez forcer la suppression de cette demande (ID: {formatDA(selectedDa.id || (selectedDa as any).oid_da)}). 
              Cette action est irréversible.
            </p>
            <button
              onClick={() => {
                if (confirm('Voulez-vous vraiment supprimer cette demande ?')) {
                  deleteMutation.mutate(selectedDa.id || (selectedDa as any).oid_da);
                }
              }}
              className="px-6 py-2.5 bg-rose-600 text-white rounded-xl font-bold text-sm shadow-lg shadow-rose-200 dark:shadow-none hover:bg-rose-700 transition-all"
            >
              🗑️ Supprimer définitivement
            </button>
          </div>
        </DaModal>
      )}
    </DashboardLayout>
  );
}

