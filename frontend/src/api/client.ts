import axios from 'axios';
import type { ApiResponse, DashboardSummary, Incident, PageResponse, RcaRequest, RcaResponse, Signal, TimelineEntry } from '../types';

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

export const incidentApi = {
  list: (params?: { state?: string; severity?: string; page?: number; size?: number }) =>
    api.get<ApiResponse<PageResponse<Incident>>>('/incidents', { params }).then(r => r.data.data),

  getActive: () =>
    api.get<ApiResponse<Incident[]>>('/incidents/active').then(r => r.data.data),

  getById: (id: string) =>
    api.get<ApiResponse<Incident>>(`/incidents/${id}`).then(r => r.data.data),

  transition: (id: string, targetState: string, changedBy?: string, notes?: string) =>
    api.patch<ApiResponse<Incident>>(`/incidents/${id}/transition`,
      { targetState, changedBy, notes }).then(r => r.data.data),

  getTimeline: (id: string) =>
    api.get<ApiResponse<TimelineEntry[]>>(`/incidents/${id}/timeline`).then(r => r.data.data),

  getSignals: (id: string) =>
    api.get<ApiResponse<Signal[]>>(`/incidents/${id}/signals`).then(r => r.data.data),
};

export const rcaApi = {
  submit: (incidentId: string, data: RcaRequest) =>
    api.post<ApiResponse<RcaResponse>>(`/incidents/${incidentId}/rca`, data).then(r => r.data.data),

  get: (incidentId: string) =>
    api.get<ApiResponse<RcaResponse>>(`/incidents/${incidentId}/rca`).then(r => r.data.data),
};

export const dashboardApi = {
  getSummary: () =>
    api.get<ApiResponse<DashboardSummary>>('/dashboard').then(r => r.data.data),
};

export default api;
