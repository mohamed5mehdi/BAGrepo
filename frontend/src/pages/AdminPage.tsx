import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { api } from '../api/axios';
import type { DaHeader } from '../types';
import DashboardLayout from '../components/DashboardLayout';
import DaTable from '../components/DaTable';
import DaModal from '../components/DaModal';
import { formatDA } from '../utils/constants';

export default function AdminPage() {
  const qc = useQueryClient();
  const [selectedDa, setSelectedDa] = useState<DaHeader | null>(null);

  // Get ALL DAs
  const { data: das, isLoading } = useQuery<DaHeader[]>({
    queryKey: ['admin-das'],
    queryFn: async () => {
      const res = await api.get('/api/da-headers');
      return res.data;
    },
  });

  // Delete DA Mutation
  const deleteDa = useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/api/da-headers/${id}`);
    },
    onSuccess: () => {
      toast.success('Demande supprimée avec succès');
      qc.invalidateQueries({ queryKey: ['admin-das'] });
      setSelectedDa(null);
    },
    onError: () => toast.error('Erreur lors de la suppression'),
  });

  return (
    <DashboardLayout title="Administration Générale" icon="⚙️" color="from-slate-800 to-black">
      <div className="space-y-6">
        <div className="bg-white dark:bg-slate-800 p-6 rounded-2xl shadow-sm">
          <h2 className="text-lg font-bold mb-4">Toutes les Demandes d'Achat</h2>
          <p className="text-sm text-slate-500 mb-6">
            En tant qu'administrateur, vous pouvez visualiser toutes les demandes d'achat du système, peu importe leur statut, et les supprimer si nécessaire.
          </p>

          <DaTable
            rows={das ?? []}
            loading={isLoading}
            onRowClick={setSelectedDa}
            actionLabel={() => '👁️ Gérer'}
          />
        </div>
      </div>

      <DaModal da={selectedDa} onClose={() => setSelectedDa(null)} title="Gestion Administrateur">
        <div className="bg-slate-50 dark:bg-slate-800/50 p-4 rounded-xl border border-slate-100 dark:border-slate-700 flex flex-col gap-3">
          <h3 className="font-semibold text-sm">Actions Administrateur</h3>
          <p className="text-xs text-slate-500">
            Attention : ces actions sont définitives et outrepassent le workflow normal.
          </p>
          <div className="flex gap-3">
            <button
              onClick={() => {
                if (confirm(`Êtes-vous sûr de vouloir supprimer la demande ${formatDA(selectedDa!.oid_da)} ?`)) {
                  deleteDa.mutate(selectedDa!.oid_da);
                }
              }}
              disabled={deleteDa.isPending}
              className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-sm font-semibold transition-colors"
            >
              🗑️ Supprimer cette DA
            </button>
            <button
              onClick={() => alert("D'autres actions admin pourront être ajoutées ici ! (ex: forcer validation)")}
              className="px-4 py-2 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 rounded-xl text-sm font-semibold transition-colors"
            >
              ⚙️ Forcer le statut (Demo)
            </button>
          </div>
        </div>
      </DaModal>
    </DashboardLayout>
  );
}
