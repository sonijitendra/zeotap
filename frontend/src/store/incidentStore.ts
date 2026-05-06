import { create } from 'zustand';
import type { DashboardSummary, Incident } from '../types';
import { dashboardApi, incidentApi } from '../api/client';

interface IncidentStore {
  incidents: Incident[];
  activeIncidents: Incident[];
  selectedIncident: Incident | null;
  dashboard: DashboardSummary | null;
  loading: boolean;
  error: string | null;
  fetchActiveIncidents: () => Promise<void>;
  fetchIncident: (id: string) => Promise<void>;
  fetchDashboard: () => Promise<void>;
  transitionState: (id: string, state: string, by?: string, notes?: string) => Promise<void>;
  clearError: () => void;
}

export const useIncidentStore = create<IncidentStore>((set, get) => ({
  incidents: [],
  activeIncidents: [],
  selectedIncident: null,
  dashboard: null,
  loading: false,
  error: null,

  fetchActiveIncidents: async () => {
    set({ loading: true, error: null });
    try {
      const data = await incidentApi.getActive();
      set({ activeIncidents: data, loading: false });
    } catch (err: any) {
      set({ error: err.response?.data?.message || 'Failed to fetch incidents', loading: false });
    }
  },

  fetchIncident: async (id: string) => {
    set({ loading: true, error: null });
    try {
      const data = await incidentApi.getById(id);
      set({ selectedIncident: data, loading: false });
    } catch (err: any) {
      set({ error: err.response?.data?.message || 'Failed to fetch incident', loading: false });
    }
  },

  fetchDashboard: async () => {
    try {
      const data = await dashboardApi.getSummary();
      set({ dashboard: data });
    } catch (err: any) {
      console.error('Dashboard fetch error:', err);
    }
  },

  transitionState: async (id, state, by, notes) => {
    set({ loading: true, error: null });
    try {
      const updated = await incidentApi.transition(id, state, by, notes);
      set({ selectedIncident: updated, loading: false });
      get().fetchActiveIncidents();
    } catch (err: any) {
      set({ error: err.response?.data?.message || 'Transition failed', loading: false });
      throw err;
    }
  },

  clearError: () => set({ error: null }),
}));
