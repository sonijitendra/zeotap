import { useNavigate } from 'react-router-dom';
import type { Incident } from '../types';
import { severityBadge, stateBadge, formatRelative, formatDuration } from './utils';
import { ChevronRight, Radio } from 'lucide-react';

interface Props { incident: Incident; }

export default function IncidentCard({ incident }: Props) {
  const navigate = useNavigate();

  return (
    <div onClick={() => navigate(`/incidents/${incident.id}`)}
      className="glass-card-hover p-4 animate-fade-in group">
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className={severityBadge(incident.severity)}>{incident.severity}</span>
          <span className={stateBadge(incident.state)}>{incident.state}</span>
        </div>
        <ChevronRight size={16} className="text-gray-600 group-hover:text-brand-400 transition-colors" />
      </div>

      <h3 className="text-sm font-semibold text-white mb-1 truncate">{incident.title}</h3>
      <p className="text-xs text-gray-500 mb-3 font-mono">{incident.componentId}</p>

      <div className="flex items-center justify-between text-xs text-gray-500">
        <div className="flex items-center gap-1">
          <Radio size={12} className="text-brand-400" />
          {incident.signalCount} signals
        </div>
        <span>{formatRelative(incident.createdAt)}</span>
      </div>

      {incident.mttrSeconds && (
        <div className="mt-2 text-xs text-gray-500">
          MTTR: <span className="text-green-400 font-medium">{formatDuration(incident.mttrSeconds)}</span>
        </div>
      )}
    </div>
  );
}
