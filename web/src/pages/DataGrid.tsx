import React, { useState, useEffect } from 'react';
import { collection, doc, setDoc, getDocs, query, where, limit, orderBy } from 'firebase/firestore';
import type { User } from 'firebase/auth';
import { db } from '../firebase/config';
import DataGridEditor from '../components/DataGridEditor';
import type { ColumnDef } from '../components/DataGridEditor';
import { X, History } from 'lucide-react';

interface DataGridProps {
  user: User | null;
  initialFilters?: any;
  onFiltersChange?: (filters: any) => void;
}

const cowColumnsBase: ColumnDef[] = [
  { key: 'tagNumber', label: 'Tag Number', type: 'text', required: true },
  { key: 'name', label: 'Name', type: 'text', hidden: false },
  { key: 'classification', label: 'Classification', type: 'select', options: ['COW', 'BULL', 'HEIFER', 'STEER', 'CALF'], default: 'COW', required: true },
  { key: 'gender', label: 'Gender', type: 'select', options: ['FEMALE', 'MALE', 'TBD'], default: 'FEMALE', required: true },
  { key: 'birthDate', label: 'Birth Date', type: 'date', hidden: true },
  { key: 'colorMarkings', label: 'Color / Markings', type: 'text', hidden: true },
  { key: 'pastureId', label: 'Pasture', type: 'select', options: [], required: true },
];

const pastureColumns: ColumnDef[] = [
  { key: 'name', label: 'Pasture Name', type: 'text', required: true },
  { key: 'acreage', label: 'Acreage', type: 'number', hidden: true },
];

const activityColumns: ColumnDef[] = [
  { key: 'activityType', label: 'Activity Type', type: 'select', options: ['MOVED', 'WEANED', 'SOLD', 'DECEASED', 'WORKED', 'CASTRATED', 'BIRTH', 'OTHER'], default: 'WORKED', required: true },
  { key: 'date', label: 'Date', type: 'date', required: true },
  { key: 'notes', label: 'Notes', type: 'text', hidden: false },
  { key: 'cowId', label: 'Cow ID / Tag', type: 'text', required: true },
];

const DataGrid: React.FC<DataGridProps> = ({ user, initialFilters, onFiltersChange }) => {
  const [activeTab, setActiveTab] = useState<'cows' | 'pastures' | 'activities'>('cows');
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  
  // Details state
  const [selectedCow, setSelectedCow] = useState<any | null>(null);
  const [cowActivities, setCowActivities] = useState<any[]>([]);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);

  // State for dynamic columns and filters
  const [cowCols, setCowCols] = useState<ColumnDef[]>(cowColumnsBase);
  const [pastureOptions, setPastureOptions] = useState<{label: string, value: string}[]>([]);
  
  // Filter state (initially from props or defaults)
  const [filters, setFilters] = useState({
    classification: initialFilters?.classification || 'All',
    gender: initialFilters?.gender || 'All',
    status: initialFilters?.status || 'All',
    pastureId: initialFilters?.pastureId || 'All',
    tagNumber: initialFilters?.tagNumber || ''
  });

  // Sync internal filter state if props change (e.g. from Dashboard or Browse)
  useEffect(() => {
    if (initialFilters) {
      setFilters(prev => ({ ...prev, ...initialFilters }));
    }
  }, [initialFilters]);

  // Auto-load if we have specific filters set on mount or change
  useEffect(() => {
    if (user && (filters.tagNumber || filters.pastureId !== 'All' || filters.classification !== 'All')) {
      loadExistingCows();
    }
  }, [user, filters.pastureId, filters.classification, filters.gender, filters.status, filters.tagNumber]);

  const updateFilter = (key: string, value: string) => {
    const newFilters = { ...filters, [key]: value };
    setFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  // Loaded data state to pass to the grid
  const [initialCows, setInitialCows] = useState<any[] | undefined>(undefined);
  const [initialPastures, setInitialPastures] = useState<any[] | undefined>(undefined);
  const [initialActivities, setInitialActivities] = useState<any[] | undefined>(undefined);

  // Fetch pastures on mount to populate the select dropdown
  useEffect(() => {
    if (!user) return;
    const fetchPastures = async () => {
      try {
        const q = query(collection(db, 'users', user.uid, 'pastures'));
        const snap = await getDocs(q);
        const options = snap.docs
          .map(doc => ({ value: doc.id, label: doc.data().name || 'Unnamed', isDeleted: doc.data().isDeleted }))
          .filter(p => p.label && p.isDeleted !== true);
        
        setPastureOptions(options);

        // Add "None" option as the default to avoid accidental assignment
        const allOptions = [{ label: 'None', value: '' }, ...options];

        setCowCols(prev => prev.map(col => {
          if (col.key === 'pastureId') {
            return { 
              ...col, 
              options: allOptions, 
              default: '' 
            };
          }
          return col;
        }));
      } catch (err) {
        console.error("Error loading pastures", err);
      }
    };
    fetchPastures();
  }, [user]);

  // Load existing records
  const loadExistingCows = async () => {
    if (!user) return;
    setIsLoading(true);
    try {
      const q = query(collection(db, 'users', user.uid, 'cows'));
      const snap = await getDocs(q);
      
      const loaded = snap.docs
        .map(doc => {
          const d = doc.data();
          return { id: doc.id, ...d };
        })
        .filter((d: any) => {
          if (d.isDeleted === true) return false;
          if (filters.classification !== 'All' && d.classification !== filters.classification) return false;
          if (filters.gender !== 'All' && d.gender !== filters.gender) return false;
          if (filters.status !== 'All' && d.status !== filters.status) return false;
          const cowPastureId = d.pastureId || '';
          if (filters.pastureId !== 'All' && cowPastureId !== filters.pastureId) return false;
          if (filters.tagNumber && !d.tagNumber?.toLowerCase().includes(filters.tagNumber.toLowerCase())) return false;
          return true;
        })
        .map(d => ({
          id: d.id,
          tagNumber: d.tagNumber || '',
          name: d.name || '',
          classification: d.classification || 'COW',
          gender: d.gender || 'FEMALE',
          birthDate: d.birthDate ? new Date(d.birthDate).toISOString().split('T')[0] : '',
          colorMarkings: d.colorMarkings || '',
          pastureId: d.pastureId || '',
          status: d.status || 'ACTIVE'
        }));
      
      setInitialCows(loaded.length > 0 ? loaded : []);
    } catch (err) {
      console.error("Error loading cows", err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadExistingPastures = async () => {
    if (!user) return;
    setIsLoading(true);
    try {
      const q = query(collection(db, 'users', user.uid, 'pastures'));
      const snap = await getDocs(q);
      const loaded = snap.docs
        .map(doc => ({ id: doc.id, ...doc.data() }))
        .filter((d: any) => d.isDeleted !== true)
        .map((d: any) => ({
          id: d.id,
          name: d.name || '',
          acreage: d.sizeAcres || '',
        }));
      setInitialPastures(loaded.length > 0 ? loaded : []);
    } catch (err) {
      console.error("Error loading pastures", err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadExistingActivities = async () => {
    if (!user) return;
    setIsLoading(true);
    try {
      const q = query(
        collection(db, 'users', user.uid, 'activities'), 
        orderBy('date', 'desc'),
        limit(100)
      );
      const snap = await getDocs(q);
      const loaded = snap.docs
        .map(doc => ({ id: doc.id, ...doc.data() }))
        .filter((d: any) => d.isDeleted !== true)
        .map((d: any) => ({
          id: d.id,
          activityType: d.activityType || 'WORKED',
          date: d.date ? new Date(d.date).toISOString().split('T')[0] : '',
          notes: d.notes || '',
          cowId: d.cowId || '',
        }));
      setInitialActivities(loaded.length > 0 ? loaded : []);
    } catch (err) {
      console.error("Error loading activities", err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSave = async (entries: any[], collectionName: string, mapData: (entry: any) => any) => {
    if (!user) return;
    setIsSaving(true);
    try {
      for (const entry of entries) {
        const firestoreId = entry.id; 
        const data = mapData(entry);
        data.updatedAt = Date.now();
        data.createdAt = data.createdAt || Date.now();
        data.updatedBy = user.uid;
        data.isDeleted = false;

        const docRef = doc(collection(db, 'users', user.uid, collectionName), firestoreId);
        await setDoc(docRef, data, { merge: true });
      }
      alert(`Successfully saved ${entries.length} records!`);
      if (collectionName === 'cows') setInitialCows(undefined);
      if (collectionName === 'pastures') setInitialPastures(undefined);
      if (collectionName === 'activities') setInitialActivities(undefined);
    } catch (error) {
      console.error('Error saving entries:', error);
      alert('Error saving entries.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveCows = (entries: any[]) => {
    return handleSave(entries, 'cows', (entry) => ({
      tagNumber: entry.tagNumber,
      name: entry.name,
      classification: entry.classification,
      gender: entry.gender,
      birthDate: entry.birthDate ? new Date(entry.birthDate).getTime() : null,
      colorMarkings: entry.colorMarkings,
      pastureId: entry.pastureId, 
      status: entry.status || 'ACTIVE',
    }));
  };

  const handleSavePastures = (entries: any[]) => {
    return handleSave(entries, 'pastures', (entry) => ({
      name: entry.name,
      sizeAcres: entry.acreage ? parseFloat(entry.acreage) : null,
      isDeleted: false,
    }));
  };

  const handleSaveActivities = (entries: any[]) => {
    return handleSave(entries, 'activities', (entry) => ({
      activityType: entry.activityType,
      date: entry.date ? new Date(entry.date).getTime() : Date.now(),
      notes: entry.notes,
      cowId: entry.cowId, 
    }));
  };

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
    <div style={{ display: 'flex', gap: '1.5rem', height: '100%', position: 'relative' }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '2rem', minWidth: 0 }}>
        <div className="page-header">
          <div>
            <h1 className="text-gradient">Data Grid</h1>
            <p style={{ color: 'var(--color-text-muted)' }}>Create or edit multiple records efficiently.</p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid var(--color-border)', paddingBottom: '0.5rem' }}>
          <button onClick={() => setActiveTab('cows')} className={`btn ${activeTab === 'cows' ? 'btn-primary' : 'btn-secondary'}`}>Cows</button>
          <button onClick={() => setActiveTab('pastures')} className={`btn ${activeTab === 'pastures' ? 'btn-primary' : 'btn-secondary'}`}>Pastures</button>
          <button onClick={() => setActiveTab('activities')} className={`btn ${activeTab === 'activities' ? 'btn-primary' : 'btn-secondary'}`}>Activities</button>
        </div>

        {activeTab === 'cows' && (
          <div className="glass-panel" style={{ padding: '1rem', display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: '150px' }}>
              <label>Classification</label>
              <select value={filters.classification} onChange={e => updateFilter('classification', e.target.value)}>
                <option value="All">All Classifications</option>
                <option value="COW">Cow</option>
                <option value="BULL">Bull</option>
                <option value="HEIFER">Heifer</option>
                <option value="STEER">Steer</option>
                <option value="CALF">Calf</option>
              </select>
            </div>
            <div style={{ flex: 1, minWidth: '150px' }}>
              <label>Gender</label>
              <select value={filters.gender} onChange={e => updateFilter('gender', e.target.value)}>
                <option value="All">All Genders</option>
                <option value="FEMALE">Female</option>
                <option value="MALE">Male</option>
                <option value="TBD">TBD</option>
              </select>
            </div>
            <div style={{ flex: 1, minWidth: '150px' }}>
              <label>Status</label>
              <select value={filters.status} onChange={e => updateFilter('status', e.target.value)}>
                <option value="All">All Status</option>
                <option value="ACTIVE">Active</option>
                <option value="SOLD">Sold</option>
                <option value="DECEASED">Deceased</option>
              </select>
            </div>
            <div style={{ flex: 1, minWidth: '150px' }}>
              <label>Pasture</label>
              <select value={filters.pastureId} onChange={e => updateFilter('pastureId', e.target.value)}>
                <option value="All">All Pastures</option>
                <option value="">None</option>
                {pastureOptions.map(p => (
                  <option key={p.value} value={p.value}>{p.label}</option>
                ))}
              </select>
            </div>
            <button onClick={loadExistingCows} className="btn btn-secondary" disabled={isLoading}>
              {isLoading ? 'Loading...' : 'Search'}
            </button>
          </div>
        )}

        {activeTab === 'pastures' && (
          <div className="glass-panel" style={{ padding: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
            <button onClick={loadExistingPastures} className="btn btn-secondary" disabled={isLoading}>
              {isLoading ? 'Loading...' : 'Load All Pastures'}
            </button>
          </div>
        )}

        {activeTab === 'activities' && (
          <div className="glass-panel" style={{ padding: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
            <button onClick={loadExistingActivities} className="btn btn-secondary" disabled={isLoading}>
              {isLoading ? 'Loading...' : 'Load Recent Activities'}
            </button>
          </div>
        )}

        <div style={{ flex: 1, overflow: 'hidden' }}>
          <DataGridEditor 
            columns={activeTab === 'cows' ? cowCols : activeTab === 'pastures' ? pastureColumns : activityColumns} 
            onSave={activeTab === 'cows' ? handleSaveCows : activeTab === 'pastures' ? handleSavePastures : handleSaveActivities} 
            isSaving={isSaving} 
            entityName={activeTab.charAt(0).toUpperCase() + activeTab.slice(1, -1)} 
            initialEntries={activeTab === 'cows' ? initialCows : activeTab === 'pastures' ? initialPastures : initialActivities}
            onRowClick={activeTab === 'cows' ? openDetails : undefined}
          />
        </div>
      </div>

      {/* Details Panel */}
      {selectedCow && (
        <div className="glass-panel" style={{ 
          width: '350px', 
          display: 'flex', 
          flexDirection: 'column',
          animation: 'slideIn 0.3s ease-out'
        }}>
          <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--color-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2 style={{ margin: 0 }}>Cow Details</h2>
            <button onClick={() => setSelectedCow(null)} className="btn btn-secondary" style={{ padding: '0.25rem' }}><X size={20} /></button>
          </div>
          <div style={{ padding: '1.5rem', overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div className="glass-card" style={{ padding: '1rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
                <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: 'var(--color-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.25rem', fontWeight: 'bold' }}>
                  {selectedCow.tagNumber.slice(-2)}
                </div>
                <div>
                  <div style={{ fontWeight: '600', fontSize: '1.125rem' }}>Tag #{selectedCow.tagNumber}</div>
                  <div style={{ color: 'var(--color-text-muted)', fontSize: '0.875rem' }}>{selectedCow.name || 'Unnamed Cow'}</div>
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', fontSize: '0.875rem' }}>
                <div><label>Class</label><div>{selectedCow.classification}</div></div>
                <div><label>Gender</label><div>{selectedCow.gender}</div></div>
                <div><label>Pasture</label><div>{pastureOptions.find(p => p.value === selectedCow.pastureId)?.label || 'None'}</div></div>
                <div><label>Status</label><div style={{ color: selectedCow.status === 'ACTIVE' ? 'var(--color-accent)' : 'var(--color-danger)' }}>{selectedCow.status}</div></div>
              </div>
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
                    <div key={act.id} className="glass-card" style={{ padding: '0.75rem', fontSize: '0.875rem' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                        <strong style={{ color: 'var(--color-primary)' }}>{act.activityType}</strong>
                        <span style={{ color: 'var(--color-text-muted)' }}>{new Date(act.date).toLocaleDateString()}</span>
                      </div>
                      <div style={{ color: 'var(--color-text)' }}>{act.notes || 'No notes provided.'}</div>
                    </div>
                  ))}
                </div>
              ) : (
                <div style={{ color: 'var(--color-text-muted)', textAlign: 'center', padding: '1rem', border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-md)' }}>
                  No activities recorded yet.
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DataGrid;
