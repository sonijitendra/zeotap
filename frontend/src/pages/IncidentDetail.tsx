import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useIncidentStore } from '../store/incidentStore';
import { incidentApi } from '../api/client';
import type { Signal, TimelineEntry, IncidentState } from '../types';
import { severityBadge, stateBadge, formatDuration, formatRelative } from '../components/utils';
import { ArrowLeft, Radio, Clock, GitBranch, FileText, AlertCircle, CheckCircle } from 'lucide-react';

const VALID_TRANSITIONS: Record<string, string[]> = {
  OPEN: ['INVESTIGATING'],
  INVESTIGATING: ['RESOLVED', 'OPEN'],
  RESOLVED: ['CLOSED', 'INVESTIGATING'],
  CLOSED: [],
};

export default function IncidentDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { selectedIncident: incident, fetchIncident, transitionState, loading, error, clearError } = useIncidentStore();
  const [signals, setSignals] = useState<Signal[]>([]);
  const [timeline, setTimeline] = useState<TimelineEntry[]>([]);
  const [tab, setTab] = useState<'signals' | 'timeline'>('signals');

  useEffect(() => {
    if (id) {
      fetchIncident(id);
      incidentApi.getSignals(id).then(setSignals).catch(() => {});
      incidentApi.getTimeline(id).then(setTimeline).catch(() => {});
    }
  }, [id]);

  if (!incident && loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="animate-spin w-8 h-8 border-2 border-brand-500 border-t-transparent rounded-full" />
    </div>
  );

  if (!incident) return (
    <div className="glass-card p-12 text-center text-gray-500">Incident not found</div>
  );

  const transitions = VALID_TRANSITIONS[incident.state] || [];

  const handleTransition = async (target: string) => {
    if (target === 'CLOSED' && !incident.hasRca) {
      navigate(`/incidents/${id}/rca`);
      return;
    }
    try {
      await transitionState(incident.id, target, 'operator');
      if (id) {
        incidentApi.getTimeline(id).then(setTimeline).catch(() => {});
      }
    } catch (e) {}
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Back + Header */}
      <div>
        <button onClick={() => navigate('/')} className="btn-ghost flex items-center gap-1 mb-4 text-sm">
          <ArrowLeft size={16} /> Back
        </button>

        <div className="glass-card p-6">
          <div className="flex items-start justify-between mb-4">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <span className={severityBadge(incident.severity)}>{incident.severity}</span>
                <span className={stateBadge(incident.state)}>{incident.state}</span>
              </div>
              <h1 className="text-xl font-bold">{incident.title}</h1>
              <p className="text-sm text-gray-500 font-mono">{incident.componentId}</p>
            </div>
          </div>

          {/* Metadata Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
            <div><p className="text-xs text-gray-500 mb-1">Signals</p><p className="font-semibold flex items-center gap-1"><Radio size={14} className="text-brand-400" />{incident.signalCount}</p></div>
            <div><p className="text-xs text-gray-500 mb-1">Created</p><p className="text-sm">{formatRelative(incident.createdAt)}</p></div>
            <div><p className="text-xs text-gray-500 mb-1">MTTR</p><p className="text-sm text-green-400 font-medium">{formatDuration(incident.mttrSeconds)}</p></div>
            <div><p className="text-xs text-gray-500 mb-1">RCA</p><p className="text-sm">{incident.hasRca ? <span className="text-green-400 flex items-center gap-1"><CheckCircle size={14} />Submitted</span> : <span className="text-yellow-400">Pending</span>}</p></div>
          </div>

          {/* Actions */}
          {error && <div className="mb-3 p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-sm text-red-400 flex items-center gap-2"><AlertCircle size={16} />{error}<button onClick={clearError} className="ml-auto text-xs underline">Dismiss</button></div>}

          <div className="flex gap-2 flex-wrap">
            {transitions.map(t => (
              <button key={t} onClick={() => handleTransition(t)}
                className={`btn-primary text-sm ${t === 'CLOSED' ? 'from-gray-600 to-gray-500 shadow-gray-500/25' : t === 'RESOLVED' ? 'from-green-600 to-green-500 shadow-green-500/25' : ''}`}>
                → {t}
              </button>
            ))}
            {incident.state === 'RESOLVED' && !incident.hasRca && (
              <button onClick={() => navigate(`/incidents/${id}/rca`)} className="btn-primary from-yellow-600 to-yellow-500 shadow-yellow-500/25 text-sm">
                <FileText size={14} className="inline mr-1" /> Submit RCA
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Tabs: Signals & Timeline */}
      <div className="glass-card overflow-hidden">
        <div className="flex border-b border-white/5">
          {(['signals', 'timeline'] as const).map(t => (
            <button key={t} onClick={() => setTab(t)}
              className={`px-6 py-3 text-sm font-medium capitalize transition-colors ${tab === t ? 'text-brand-400 border-b-2 border-brand-500' : 'text-gray-500 hover:text-white'}`}>
              {t === 'signals' ? <><Radio size={14} className="inline mr-1" />{t} ({signals.length})</> : <><GitBranch size={14} className="inline mr-1" />{t} ({timeline.length})</>}
            </button>
          ))}
        </div>

        <div className="p-4 max-h-96 overflow-y-auto">
          {tab === 'signals' ? (
            signals.length === 0 ? <p className="text-gray-500 text-sm text-center py-8">No signals</p> :
            <div className="space-y-2">
              {signals.map(s => (
                <div key={s.signalId} className="p-3 bg-surface-800/50 rounded-xl border border-white/5 text-sm animate-slide-in">
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-mono text-xs text-gray-500">{s.signalId.slice(0, 8)}...</span>
                    <span className={`badge badge-${s.severity.toLowerCase()}`}>{s.severity}</span>
                  </div>
                  <p className="text-gray-300">{s.message}</p>
                  <p className="text-xs text-gray-500 mt-1">{formatRelative(s.timestamp)}</p>
                </div>
              ))}
            </div>
          ) : (
            timeline.length === 0 ? <p className="text-gray-500 text-sm text-center py-8">No timeline entries</p> :
            <div className="relative pl-6 space-y-4">
              <div className="absolute left-2 top-0 bottom-0 w-0.5 bg-gradient-to-b from-brand-500 to-transparent" />
              {timeline.map(t => (
                <div key={t.id} className="relative animate-slide-in">
                  <div className="absolute -left-[18px] top-1 w-3 h-3 rounded-full bg-brand-500 border-2 border-surface-800" />
                  <div className="p-3 bg-surface-800/50 rounded-xl border border-white/5">
                    <div className="flex items-center gap-2 text-sm">
                      {t.fromState && <span className={`badge badge-${t.fromState.toLowerCase()}`}>{t.fromState}</span>}
                      <span className="text-gray-500">→</span>
                      <span className={`badge badge-${t.toState.toLowerCase()}`}>{t.toState}</span>
                    </div>
                    {t.notes && <p className="text-xs text-gray-400 mt-1">{t.notes}</p>}
                    <p className="text-xs text-gray-500 mt-1">{t.changedBy} · {formatRelative(t.createdAt)}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
