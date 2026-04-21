import React from 'react';
import { LayoutDashboard, Users, Settings, Database } from 'lucide-react';

interface SidebarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ currentTab, onTabChange }) => {
  return (
    <aside className="glass-panel" style={{ width: '250px', height: '100%', borderLeft: 'none', borderTop: 'none', borderBottom: 'none', borderRadius: 0, padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      <div>
        <h2 className="text-gradient" style={{ fontSize: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <img src="/vite.svg" alt="Logo" style={{ width: '24px' }} />
          HerdManager
        </h2>
      </div>

      <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
        <button
          onClick={() => onTabChange('dashboard')}
          className={`btn ${currentTab === 'dashboard' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ justifyContent: 'flex-start', padding: '0.75rem 1rem' }}
        >
          <LayoutDashboard size={18} />
          Dashboard
        </button>
        <button
          onClick={() => onTabChange('data-grid')}
          className={`btn ${currentTab === 'data-grid' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ justifyContent: 'flex-start', padding: '0.75rem 1rem' }}
        >
          <Database size={18} />
          Data Grid
        </button>
        <button
          onClick={() => onTabChange('settings')}
          className={`btn ${currentTab === 'settings' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ justifyContent: 'flex-start', padding: '0.75rem 1rem' }}
        >
          <Settings size={18} />
          Settings
        </button>
      </nav>

      <div style={{ marginTop: 'auto', fontSize: '0.75rem', color: 'var(--color-text-muted)', textAlign: 'center' }}>
        HerdManager Companion App<br />
        v1.0.0
      </div>
    </aside>
  );
};

export default Sidebar;
