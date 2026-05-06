export type Severity = 'P0' | 'P1' | 'P2' | 'P3';
export type IncidentState = 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'CLOSED';

export interface Incident {
  id: string;
  componentId: string;
  severity: Severity;
  state: IncidentState;
  title: string;
  description: string;
  signalCount: number;
  firstSignalAt: string;
  resolvedAt: string | null;
  closedAt: string | null;
  mttrSeconds: number | null;
  assignedTo: string | null;
  createdAt: string;
  updatedAt: string;
  hasRca: boolean;
}

export interface Signal {
  signalId: string;
  componentId: string;
  severity: string;
  timestamp: string;
  message: string;
  metadata: Record<string, unknown>;
  incidentId: string;
  processed: boolean;
  processedAt: string;
}

export interface RcaRequest {
  incidentStartTime: string;
  incidentEndTime: string;
  rootCauseCategory: string;
  rootCauseDetail: string;
  fixApplied: string;
  preventionSteps: string;
  submittedBy: string;
}

export interface RcaResponse {
  id: string;
  incidentId: string;
  incidentStartTime: string;
  incidentEndTime: string;
  rootCauseCategory: string;
  rootCauseDetail: string;
  fixApplied: string;
  preventionSteps: string;
  submittedBy: string;
  submittedAt: string;
}

export interface TimelineEntry {
  id: string;
  incidentId: string;
  fromState: string | null;
  toState: string;
  changedBy: string;
  notes: string;
  createdAt: string;
}

export interface DashboardSummary {
  totalActiveIncidents: number;
  totalSignalsToday: number;
  incidentsBySeverity: Record<string, number>;
  incidentsByState: Record<string, number>;
  avgMttrSeconds: number;
  signalsPerSecond: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errorCode?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
