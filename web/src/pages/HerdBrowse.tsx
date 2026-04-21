import React, { useState, useEffect } from 'react';
import { collection, getDocs, query, where, orderBy } from 'firebase/firestore';
import type { User } from 'firebase/auth';
import { db } from '../firebase/config';
import { Search, Filter, ChevronRight, MapPin, Tag, Edit3, X, History } from 'lucide-react';

interface HerdBrowseProps {
  user: User | null;
  initialFilters?: any;
  onEdit: (cowId: string) => void;
}

const HerdBrowse: React.FC<HerdBrowseProps> = ({ user, initialFilters, onEdit }) => {
  const [cows, setCows] = useState<any[]>([]);
  const [pastures, setPastures] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Filters
  const [filters, setFilters] = useState({
    classification: initialFilters?.classification || 'All',
    gender: initialFilters?.gender || 'All',
    status: initialFilters?.status || 'ACTIVE',
    pastureId: initialFilters?.pastureId || 'All'
  });

  // Details state
  const [selectedCow, setSelectedCow] = useState<any | null>(null);
  const [cowActivities, setCowActivities] = useState<any[]>([]);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);

  useEffect(() => {
    if (initialFilters) {
      setFilters(initialFilters);
    }
  }, [initialFilters]);

  const [viewMode, setViewMode] = useState<'cards' | 'grid'>(() => {
    return (localStorage.getItem('herdViewMode') as 'cards' | 'grid') || 'cards';
  });

  useEffect(() => {
    localStorage.setItem('herdViewMode', viewMode);
  }, [viewMode]);

  const fetchData = async () => {
    if (!user) return;
    setIsLoading(true);
    try {
      // Fetch Pastures first
      const pastureQ = query(collection(db, 'users', user.uid, 'pastures'));
      const pSnap = await getDocs(pastureQ);
      const pMap: Record<string, string> = {};
      pSnap.docs.forEach(doc => {
        pMap[doc.id] = doc.data().name || 'Unnamed';
      });
      setPastures(pMap);

      // Fetch Cows
      const cowQ = query(collection(db, 'users', user.uid, 'cows'));
      const cowSnap = await getDocs(cowQ);
      const cowData = cowSnap.docs
        .map(doc => ({ id: doc.id, ...doc.data() }))
        .filter((c: any) => {
          if (c.isDeleted === true) return false;
          if (filters.classification !== 'All' && c.classification !== filters.classification) return false;
          if (filters.gender !== 'All' && c.gender !== filters.gender) return false;
          if (filters.status !== 'All' && c.status !== filters.status) return false;
          const cowPastureId = c.pastureId || '';
          if (filters.pastureId !== 'All' && cowPastureId !== filters.pastureId) return false;
          return true;
        });
      setCows(cowData);
    } catch (error) {
      console.error("Error fetching herd:", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [user, filters]);

  const filteredCows = cows.filter(c => 
    c.tagNumber?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.name?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const openDetails = async (cow: any) => {
    if (!user) return;
    setSelectedCow(cow);
    setIsLoadingDetails(true);
    try {
      const q = query(
        collection(db, 'users', user.uid, 'activities'),
        where('cowId', '==', cow.id),
        orderBy('date', 'desc')
      );
      const snap = await getDocs(q);
      setCowActivities(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    } catch (err) {
      console.error("Error loading cow activities", err);
    } finally {
      setIsLoadingDetails(false);
    }
  };

  return (
    <div style={{ display: 'flex', gap: '2rem', height: '100%', position: 'relative' }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '1.5rem', minWidth: 0 }}>
        <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h1 className="text-gradient">Herd Overview</h1>
            <p style={{ color: 'var(--color-text-muted)' }}>{filteredCows.length} animals matching filters.</p>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button 
              onClick={() => onEdit('all')} 
              className="btn btn-primary"
              disabled={filteredCows.length === 0}
            >
              <Edit3 size={18} /> Mass Edit Results
            </button>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1rem', display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ position: 'relative', flex: 1, minWidth: '200px' }}>
            <Search size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-muted)' }} />
            <input 
              type="text" 
              placeholder="Search by tag or name..." 
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              style={{ paddingLeft: '2.75rem' }}
            />
          </div>
          
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
             <select 
              value={filters.status} 
              onChange={e => setFilters(prev => ({ ...prev, status: e.target.value }))}
              style={{ width: 'auto' }}
            >
              <option value="All">All Status</option>
              <option value="ACTIVE">Active</option>
              <option value="SOLD">Sold</option>
              <option value="DECEASED">Deceased</option>
            </select>

            <div style={{ display: 'flex', background: 'rgba(255,255,255,0.05)', padding: '0.25rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)' }}>
              <button 
                onClick={() => setViewMode('cards')} 
                className={`btn ${viewMode === 'cards' ? 'btn-primary' : ''}`}
                style={{ padding: '0.4rem', background: viewMode === 'cards' ? '' : 'transparent', border: 'none' }}
              >
                Cards
              </button>
              <button 
                onClick={() => setViewMode('grid')} 
                className={`btn ${viewMode === 'grid' ? 'btn-primary' : ''}`}
                style={{ padding: '0.4rem', background: viewMode === 'grid' ? '' : 'transparent', border: 'none' }}
              >
                Grid
              </button>
            </div>
          </div>
        </div>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--color-text-muted)' }}>Loading your herd...</div>
        ) : viewMode === 'cards' ? (
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', 
            gap: '1rem',
            overflowY: 'auto',
            paddingBottom: '2rem'
          }}>
            {filteredCows.map(cow => (
              <div 
                key={cow.id} 
                className="glass-card hover-bg" 
                style={{ padding: '1.25rem', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                onClick={() => openDetails(cow)}
              >
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                  <div style={{ 
                    width: '50px', height: '50px', borderRadius: 'var(--radius-md)', 
                    background: 'rgba(59, 130, 246, 0.1)', border: '1px solid var(--color-border)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    color: 'var(--color-primary)', fontWeight: 'bold', fontSize: '1.25rem'
                  }}>
                    {cow.tagNumber?.slice(-2)}
                  </div>
                  <div>
                    <div style={{ fontWeight: '600' }}>Tag #{cow.tagNumber}</div>
                    <div style={{ fontSize: '0.875rem', color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                      <MapPin size={12} /> {pastures[cow.pastureId] || 'No Pasture'}
                    </div>
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '0.25rem' }}>
                  <span style={{ 
                    fontSize: '0.75rem', padding: '0.25rem 0.5rem', borderRadius: '100px',
                    background: cow.status === 'ACTIVE' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                    color: cow.status === 'ACTIVE' ? 'var(--color-accent)' : 'var(--color-danger)',
                    border: `1px solid ${cow.status === 'ACTIVE' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`
                  }}>
                    {cow.classification}
                  </span>
                  <ChevronRight size={18} style={{ color: 'var(--color-text-muted)' }} />
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="table-container glass-panel" style={{ flex: 1, overflowY: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>Tag #</th>
                  <th>Name</th>
                  <th>Classification</th>
                  <th>Gender</th>
                  <th>Pasture</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {filteredCows.map(cow => (
                  <tr key={cow.id} className="hover-bg" style={{ cursor: 'pointer' }} onClick={() => openDetails(cow)}>
                    <td><div style={{ fontWeight: '600', color: 'var(--color-primary)' }}>{cow.tagNumber}</div></td>
                    <td>{cow.name || '-'}</td>
                    <td>{cow.classification}</td>
                    <td>{cow.gender}</td>
                    <td>{pastures[cow.pastureId] || '-'}</td>
                    <td>
                      <span style={{ 
                        color: cow.status === 'ACTIVE' ? 'var(--color-accent)' : 'var(--color-danger)',
                        fontSize: '0.875rem'
                      }}>
                        {cow.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {!isLoading && filteredCows.length === 0 && (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--color-text-muted)', border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-lg)' }}>
            No cattle found matching those criteria.
          </div>
        )}
      </div>

      {/* Details Panel */}
      {selectedCow && (
        <div className="glass-panel" style={{ 
          width: '400px', 
          display: 'flex', 
          flexDirection: 'column',
          animation: 'slideIn 0.3s ease-out'
        }}>
          <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--color-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2 style={{ margin: 0 }}>Animal Details</h2>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button 
                onClick={() => onEdit(selectedCow.tagNumber)} 
                className="btn btn-secondary" 
                style={{ padding: '0.5rem' }}
                title="Edit in Data Grid"
              >
                <Edit3 size={18} />
              </button>
              <button onClick={() => setSelectedCow(null)} className="btn btn-secondary" style={{ padding: '0.5rem' }}><X size={18} /></button>
            </div>
          </div>
          
          <div style={{ padding: '1.5rem', overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div className="glass-card" style={{ padding: '1.5rem' }}>
               <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
                <div>
                  <label>Tag Number</label>
                  <div style={{ fontWeight: '600', fontSize: '1.125rem' }}>#{selectedCow.tagNumber}</div>
                </div>
                <div>
                  <label>Status</label>
                  <div style={{ color: selectedCow.status === 'ACTIVE' ? 'var(--color-accent)' : 'var(--color-danger)', fontWeight: '600' }}>{selectedCow.status}</div>
                </div>
                <div>
                  <label>Classification</label>
                  <div>{selectedCow.classification}</div>
                </div>
                <div>
                  <label>Gender</label>
                  <div>{selectedCow.gender}</div>
                </div>
                <div>
                  <label>Current Pasture</label>
                  <div>{pastures[selectedCow.pastureId] || 'None Assigned'}</div>
                </div>
                <div>
                  <label>Birth Date</label>
                  <div>{selectedCow.birthDate ? new Date(selectedCow.birthDate).toLocaleDateString() : 'Unknown'}</div>
                </div>
              </div>
              {selectedCow.colorMarkings && (
                <div style={{ marginTop: '1rem' }}>
                  <label>Color / Markings</label>
                  <div>{selectedCow.colorMarkings}</div>
                </div>
              )}
            </div>

            <div>
              <h3 style={{ fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                <History size={18} /> Activity History
              </h3>
              {isLoadingDetails ? (
                <div style={{ color: 'var(--color-text-muted)', textAlign: 'center', padding: '1rem' }}>Loading history...</div>
              ) : cowActivities.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {cowActivities.map(act => (
                    <div key={act.id} className="glass-card" style={{ padding: '1rem', fontSize: '0.875rem' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                        <strong style={{ color: 'var(--color-primary)' }}>{act.activityType}</strong>
                        <span style={{ color: 'var(--color-text-muted)' }}>{new Date(act.date).toLocaleDateString()}</span>
                      </div>
                      <div style={{ color: 'var(--color-text)' }}>{act.notes || 'No notes provided.'}</div>
                    </div>
                  ))}
                </div>
              ) : (
                <div style={{ color: 'var(--color-text-muted)', textAlign: 'center', padding: '2rem', border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-lg)' }}>
                  No activities recorded.
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default HerdBrowse;
