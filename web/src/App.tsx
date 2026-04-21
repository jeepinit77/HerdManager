import React, { useState, useEffect } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import type { User } from 'firebase/auth';
import { auth } from './firebase/config';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import DataGrid from './pages/DataGrid';
import Login from './pages/Login';

const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState('data-grid');
  const [user, setUser] = useState<User | null>(null);
  const [isAuthLoading, setIsAuthLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setIsAuthLoading(false);
    });
    return () => unsubscribe();
  }, []);

  const renderContent = () => {
    switch (currentTab) {
      case 'dashboard':
        return <Dashboard user={user} />;
      case 'data-grid':
        return <DataGrid user={user} />;
      case 'settings':
        return (
          <div>
            <h1 className="text-gradient">Settings</h1>
            <p style={{ color: 'var(--color-text-muted)', marginTop: '0.5rem' }}>Configuration options will go here.</p>
            <button 
              onClick={() => auth.signOut()}
              className="btn btn-danger"
              style={{ marginTop: '2rem' }}
            >
              Sign Out
            </button>
          </div>
        );
      default:
        return <Dashboard />;
    }
  };

  if (isAuthLoading) {
    return (
      <div style={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center', background: 'var(--color-bg)' }}>
        <p style={{ color: 'var(--color-text-muted)' }}>Loading...</p>
      </div>
    );
  }

  if (!user) {
    return <Login onLogin={() => {}} />;
  }

  return (
    <div className="app-container">
      <Sidebar currentTab={currentTab} onTabChange={setCurrentTab} />
      <main className="main-content">
        {renderContent()}
      </main>
    </div>
  );
};

export default App;
