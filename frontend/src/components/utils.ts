import type { IncidentState, Severity } from '../types';

export function severityBadge(severity: Severity): string {
  const map: Record<Severity, string> = { P0: 'badge-p0', P1: 'badge-p1', P2: 'badge-p2', P3: 'badge-p3' };
  return `badge ${map[severity] || 'badge-p2'}`;
}

export function stateBadge(state: IncidentState): string {
  const map: Record<IncidentState, string> = {
    OPEN: 'badge-open', INVESTIGATING: 'badge-investigating',
    RESOLVED: 'badge-resolved', CLOSED: 'badge-closed',
  };
  return `badge ${map[state] || ''}`;
}

export function formatDuration(seconds: number | null): string {
  if (!seconds) return '—';
  if (seconds < 60) return `${seconds}s`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}h ${m}m`;
}

export function formatRelative(iso: string): string {
  const diff = (Date.now() - new Date(iso).getTime()) / 1000;
  if (diff < 60) return 'just now';
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
}
