import React, { useState, useEffect } from 'react';
import type { User } from 'firebase/auth';
import { collection, getDocs, query } from 'firebase/firestore';
import { db } from '../firebase/config';

interface DashboardProps {
  user: User | null;
}

const Dashboard: React.FC<DashboardProps> = ({ user }) => {
  const [cows, setCows] = useState<any[]>([]);
  const [pastures, setPastures] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(true);

  const fetchData = async () => {
    if (!user) return;
    setIsLoading(true);
    try {
      // Fetch Cows
      const cowQ = query(collection(db, 'users', user.uid, 'cows'));
      const cowSnap = await getDocs(cowQ);
      // Filter client-side to be robust against missing fields
      const cowData = cowSnap.docs
        .map(doc => ({ id: doc.id, ...doc.data() }))
        .filter((c: any) => c.isDeleted !== true);
      setCows(cowData);

      // Fetch Pastures to resolve names
      const pastureQ = query(collection(db, 'users', user.uid, 'pastures'));
      const pastureSnap = await getDocs(pastureQ);
      const pMap: Record<string, string> = {};
      pastureSnap.docs.forEach(doc => {
        const data = doc.data();
        if (data.isDeleted !== true) {
          pMap[doc.id] = data.name || 'Unnamed';
        }
      });
      setPastures(pMap);
    } catch (error) {
      console.error("Error fetching dashboard data:", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [user]);

  const activeHerd = cows.filter(c => c.status === 'ACTIVE' || !c.status);
  const soldHerd = cows.filter(c => c.status === 'SOLD');
  
  // Calculate by pasture
  const byPasture = activeHerd.reduce((acc, cow) => {
    const pId = cow.pastureId;
    const name = pId ? (pastures[pId] || 'Unknown Pasture') : 'No Pasture';
    acc[name] = (acc[name] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  // Calculate by classification
  const byClass = activeHerd.reduce((acc, cow) => {
    const c = cow.classification || 'Unknown';
    acc[c] = (acc[c] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  // Calculate calves this year
  const currentYear = new Date().getFullYear();
  const calvesThisYear = activeHerd.filter(c => {
    if (!c.birthDate) return false;
    const birthYear = new Date(c.birthDate).getFullYear();
    return birthYear === currentYear && c.classification === 'CALF';
  }).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      <div className="page-header">
        <div>
          <h1 className="text-gradient">Herd Dashboard</h1>
          <p style={{ color: 'var(--color-text-muted)' }}>Overview of your cattle and pastures.</p>
        </div>
        <button onClick={fetchData} className="btn btn-primary" disabled={isLoading}>
          {isLoading ? 'Loading...' : 'Refresh Data'}
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem' }}>
        <div className="glass-card" style={{ padding: '1.5rem' }}>
          <h3 style={{ color: 'var(--color-text-muted)', fontSize: '0.875rem', textTransform: 'uppercase' }}>Active Herd</h3>
          <div style={{ fontSize: '2.5rem', fontWeight: '700', marginTop: '0.5rem' }}>{activeHerd.length}</div>
        </div>
        <div className="glass-card" style={{ padding: '1.5rem' }}>
          <h3 style={{ color: 'var(--color-text-muted)', fontSize: '0.875rem', textTransform: 'uppercase' }}>Calves this Year</h3>
          <div style={{ fontSize: '2.5rem', fontWeight: '700', marginTop: '0.5rem', color: 'var(--color-accent)' }}>{calvesThisYear}</div>
        </div>
        <div className="glass-card" style={{ padding: '1.5rem' }}>
          <h3 style={{ color: 'var(--color-text-muted)', fontSize: '0.875rem', textTransform: 'uppercase' }}>Sold</h3>
          <div style={{ fontSize: '2.5rem', fontWeight: '700', marginTop: '0.5rem' }}>{soldHerd.length}</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem' }}>By Pasture</h3>
          <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {Object.entries(byPasture).sort((a, b) => b[1] - a[1]).map(([pasture, count]) => (
              <li key={pasture} style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>{pasture}</span> <strong>{count}</strong>
              </li>
            ))}
            {Object.keys(byPasture).length === 0 && <li style={{ color: 'var(--color-text-muted)' }}>No active cattle</li>}
          </ul>
        </div>
        
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem' }}>By Classification</h3>
          <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
             {Object.entries(byClass).sort((a, b) => b[1] - a[1]).map(([cls, count]) => (
              <li key={cls} style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>{cls}</span> <strong>{count}</strong>
              </li>
            ))}
            {Object.keys(byClass).length === 0 && <li style={{ color: 'var(--color-text-muted)' }}>No active cattle</li>}
          </ul>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
