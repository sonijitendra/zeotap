import { Link, useLocation } from 'react-router-dom';
import { Activity, LayoutDashboard, AlertTriangle } from 'lucide-react';

export default function Sidebar() {
  const { pathname } = useLocation();
  const links = [
    { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/incidents', icon: AlertTriangle, label: 'Incidents' },
  ];

  return (
    <aside className="w-64 h-screen bg-surface-800/80 backdrop-blur-xl border-r border-white/5 flex flex-col fixed left-0 top-0 z-40">
      {/* Logo */}
      <div className="px-6 py-5 border-b border-white/5">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 flex items-center justify-center">
            <Activity size={20} className="text-white" />
          </div>
          <div>
            <h1 className="text-base font-bold tracking-tight">IMS</h1>
            <p className="text-[10px] text-gray-500 uppercase tracking-widest">Incident Mgmt</p>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {links.map(({ to, icon: Icon, label }) => {
          const active = pathname === to || (to !== '/' && pathname.startsWith(to));
          return (
            <Link key={to} to={to}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                active
                  ? 'bg-brand-600/20 text-brand-400 border border-brand-500/20'
                  : 'text-gray-400 hover:text-white hover:bg-white/5'
              }`}>
              <Icon size={18} />
              {label}
            </Link>
          );
        })}
      </nav>

      {/* Status indicator */}
      <div className="px-4 py-3 border-t border-white/5">
        <div className="flex items-center gap-2 text-xs text-gray-500">
          <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
          System Operational
        </div>
      </div>
    </aside>
  );
}
