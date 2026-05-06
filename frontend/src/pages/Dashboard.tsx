import { useEffect } from 'react';
import { useIncidentStore } from '../store/incidentStore';
import { useInterval } from '../hooks/useSSE';
import IncidentCard from '../components/IncidentCard';
import { AlertTriangle, Activity, Clock, BarChart3, Zap, TrendingUp } from 'lucide-react';
import { formatDuration } from '../components/utils';

export default function Dashboard() {
  const { activeIncidents, dashboard, fetchActiveIncidents, fetchDashboard, loading } = useIncidentStore();

  useEffect(() => { fetchActiveIncidents(); fetchDashboard(); }, []);
  useInterval(() => { fetchActiveIncidents(); fetchDashboard(); }, 10000);

  const stats = [
    { label: 'Active Incidents', value: dashboard?.totalActiveIncidents ?? 0, icon: AlertTriangle, color: 'from-red-500 to-orange-500', textColor: 'text-red-400' },
    { label: 'Signals Today', value: dashboard?.totalSignalsToday ?? 0, icon: Zap, color: 'from-brand-500 to-blue-500', textColor: 'text-brand-400' },
    { label: 'Avg MTTR', value: formatDuration(dashboard?.avgMttrSeconds ?? 0), icon: Clock, color: 'from-green-500 to-emerald-500', textColor: 'text-green-400' },
    { label: 'Throughput', value: `${dashboard?.signalsPerSecond ?? 0}/s`, icon: TrendingUp, color: 'from-purple-500 to-pink-500', textColor: 'text-purple-400' },
  ];

  const severityData = dashboard?.incidentsBySeverity ?? {};
  const stateData = dashboard?.incidentsByState ?? {};

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">Real-time incident overview</p>
        </div>
        <div className="flex items-center gap-2 text-xs text-gray-500">
          <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
          Auto-refreshing every 10s
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map(({ label, value, icon: Icon, color, textColor }) => (
          <div key={label} className="glass-card p-5 relative overflow-hidden">
            <div className={`absolute top-0 right-0 w-20 h-20 bg-gradient-to-br ${color} opacity-10 rounded-full -translate-y-5 translate-x-5`} />
            <div className="flex items-center gap-3 mb-3">
              <div className={`w-8 h-8 rounded-lg bg-gradient-to-br ${color} flex items-center justify-center`}>
                <Icon size={16} className="text-white" />
              </div>
              <span className="text-xs text-gray-500 uppercase tracking-wider">{label}</span>
            </div>
            <p className={`text-2xl font-bold ${textColor}`}>{value}</p>
          </div>
        ))}
      </div>

      {/* Severity & State Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="glass-card p-5">
          <h2 className="section-title flex items-center gap-2 mb-4">
            <BarChart3 size={18} className="text-brand-400" /> By Severity
          </h2>
          <div className="space-y-3">
            {Object.entries(severityData).map(([sev, count]) => (
              <div key={sev} className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className={`badge badge-${sev.toLowerCase()}`}>{sev}</span>
                </div>
                <div className="flex items-center gap-3">
                  <div className="w-32 h-2 bg-surface-700 rounded-full overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-brand-500 to-brand-400 rounded-full transition-all duration-500"
                      style={{ width: `${Math.min(100, ((count as number) / Math.max(1, dashboard?.totalActiveIncidents ?? 1)) * 100)}%` }} />
                  </div>
                  <span className="text-sm font-mono text-gray-400 w-8 text-right">{count as number}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-card p-5">
          <h2 className="section-title flex items-center gap-2 mb-4">
            <Activity size={18} className="text-brand-400" /> By State
          </h2>
          <div className="space-y-3">
            {Object.entries(stateData).map(([state, count]) => (
              <div key={state} className="flex items-center justify-between">
                <span className={`badge badge-${state.toLowerCase()}`}>{state}</span>
                <span className="text-sm font-mono text-gray-400">{count as number}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Active Incidents */}
      <div>
        <h2 className="section-title mb-4 flex items-center gap-2">
          <AlertTriangle size={18} className="text-red-400" />
          Active Incidents
          <span className="text-xs text-gray-500 font-normal">({activeIncidents.length})</span>
        </h2>
        {loading && activeIncidents.length === 0 ? (
          <div className="glass-card p-12 text-center text-gray-500">
            <div className="animate-spin w-8 h-8 border-2 border-brand-500 border-t-transparent rounded-full mx-auto mb-3" />
            Loading...
          </div>
        ) : activeIncidents.length === 0 ? (
          <div className="glass-card p-12 text-center text-gray-500">
            <Activity size={40} className="mx-auto mb-3 text-green-500" />
            <p className="text-lg font-medium text-green-400">All Clear</p>
            <p className="text-sm">No active incidents</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {activeIncidents.map(incident => (
              <IncidentCard key={incident.id} incident={incident} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
