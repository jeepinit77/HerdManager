import React, { useState, useEffect } from 'react';
import { collection, doc, setDoc, getDocs, query, where } from 'firebase/firestore';
import type { User } from 'firebase/auth';
import { db } from '../firebase/config';
import DataGridEditor from '../components/DataGridEditor';
import type { ColumnDef } from '../components/DataGridEditor';

interface DataGridProps {
  user: User | null;
}

const cowColumnsBase: ColumnDef[] = [
  { key: 'tagNumber', label: 'Tag Number', type: 'text', required: true },
  { key: 'name', label: 'Name', type: 'text', hidden: true },
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

const DataGrid: React.FC<DataGridProps> = ({ user }) => {
  const [activeTab, setActiveTab] = useState<'cows' | 'pastures' | 'activities'>('cows');
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  
  // State for dynamic columns
  const [cowCols, setCowCols] = useState<ColumnDef[]>(cowColumnsBase);
  
  // Filter state
  const [filterClass, setFilterClass] = useState('All');
  const [filterGender, setFilterGender] = useState('All');
  const [filterStatus, setFilterStatus] = useState('ACTIVE');

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
        const pastureOptions = snap.docs
          .map(doc => ({ value: doc.id, label: doc.data().name || 'Unnamed', isDeleted: doc.data().isDeleted }))
          .filter(p => p.label && p.isDeleted !== true);
        
        // Add "None" option as the default to avoid accidental assignment
        const allOptions = [{ label: 'None', value: '' }, ...pastureOptions];

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
          if (filterClass !== 'All' && d.classification !== filterClass) return false;
          if (filterGender !== 'All' && d.gender !== filterGender) return false;
          if (filterStatus !== 'All' && d.status !== filterStatus) return false;
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
        }));
      
      setInitialCows(loaded.length > 0 ? loaded : undefined);
      if (loaded.length === 0) alert('No records found matching those filters.');
    } catch (err) {
      console.error("Error loading cows", err);
      alert('Error loading existing data.');
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
      setInitialPastures(loaded.length > 0 ? loaded : undefined);
      if (loaded.length === 0) alert('No pastures found.');
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
      const q = query(collection(db, 'users', user.uid, 'activities'), limit(100));
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
      setInitialActivities(loaded.length > 0 ? loaded : undefined);
      if (loaded.length === 0) alert('No activities found.');
    } catch (err) {
      console.error("Error loading activities", err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSave = async (entries: any[], collectionName: string, mapData: (entry: any) => any) => {
    if (!user) {
      alert("You must be logged in to save.");
      return;
    }

    setIsSaving(true);
    try {
      for (const entry of entries) {
        const firestoreId = entry.id; 
        const data = mapData(entry);
        data.updatedAt = Date.now();
        data.createdAt = data.createdAt || Date.now();
        data.updatedBy = user.uid;
        data.isDeleted = false; // Ensure it's not deleted

        // Path is users/{uid}/{collectionName}/{firestoreId}
        const docRef = doc(collection(db, 'users', user.uid, collectionName), firestoreId);
        await setDoc(docRef, data, { merge: true }); // Merge ensures we update existing docs without destroying fields not in grid
      }
      alert(`Successfully saved ${entries.length} records!`);
      
      // Clear initial data so the grid resets or the user can reload
      if (collectionName === 'cows') setInitialCows(undefined);
      if (collectionName === 'pastures') setInitialPastures(undefined);
      if (collectionName === 'activities') setInitialActivities(undefined);
    } catch (error) {
      console.error('Error saving entries:', error);
      alert('Error saving entries. Check console for details.');
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
      status: filterStatus !== 'All' ? filterStatus : 'ACTIVE', // keep status if editing, but default active
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

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem', height: '100%' }}>
      <div className="page-header">
        <div>
          <h1 className="text-gradient">Data Grid</h1>
          <p style={{ color: 'var(--color-text-muted)' }}>Create or edit multiple records efficiently.</p>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid var(--color-border)', paddingBottom: '0.5rem' }}>
        <button 
          onClick={() => setActiveTab('cows')}
          className={`btn ${activeTab === 'cows' ? 'btn-primary' : 'btn-secondary'}`}
        >
          Cows
        </button>
        <button 
          onClick={() => setActiveTab('pastures')}
          className={`btn ${activeTab === 'pastures' ? 'btn-primary' : 'btn-secondary'}`}
        >
          Pastures
        </button>
        <button 
          onClick={() => setActiveTab('activities')}
          className={`btn ${activeTab === 'activities' ? 'btn-primary' : 'btn-secondary'}`}
        >
          Activities
        </button>
      </div>

      {activeTab === 'cows' && (
        <div className="glass-panel" style={{ padding: '1rem', display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem', color: 'var(--color-text-muted)' }}>Classification</label>
            <select value={filterClass} onChange={e => setFilterClass(e.target.value)} style={{ width: '150px' }}>
              <option value="All">All</option>
              <option value="COW">Cow</option>
              <option value="BULL">Bull</option>
              <option value="HEIFER">Heifer</option>
              <option value="STEER">Steer</option>
              <option value="CALF">Calf</option>
            </select>
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem', color: 'var(--color-text-muted)' }}>Gender</label>
            <select value={filterGender} onChange={e => setFilterGender(e.target.value)} style={{ width: '150px' }}>
              <option value="All">All</option>
              <option value="FEMALE">Female</option>
              <option value="MALE">Male</option>
              <option value="TBD">TBD</option>
            </select>
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem', color: 'var(--color-text-muted)' }}>Status</label>
            <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)} style={{ width: '150px' }}>
              <option value="All">All</option>
              <option value="ACTIVE">Active</option>
              <option value="SOLD">Sold</option>
              <option value="DECEASED">Deceased</option>
            </select>
          </div>
          <button onClick={loadExistingCows} className="btn btn-secondary" disabled={isLoading} style={{ marginLeft: 'auto' }}>
            {isLoading ? 'Loading...' : 'Load Existing'}
          </button>
        </div>
      )}

      {activeTab === 'pastures' && (
        <div className="glass-panel" style={{ padding: '1rem', display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
          <button onClick={loadExistingPastures} className="btn btn-secondary" disabled={isLoading}>
            {isLoading ? 'Loading...' : 'Load Existing Pastures'}
          </button>
        </div>
      )}

      {activeTab === 'activities' && (
        <div className="glass-panel" style={{ padding: '1rem', display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
          <button onClick={loadExistingActivities} className="btn btn-secondary" disabled={isLoading}>
            {isLoading ? 'Loading...' : 'Load Existing Activities (Recent 50)'}
          </button>
        </div>
      )}

      <div style={{ flex: 1, overflow: 'hidden' }}>
        {activeTab === 'cows' && (
          <DataGridEditor 
            // Add a key so it completely remounts when initialCows changes, avoiding stale state
            key={`cows-${initialCows ? 'loaded' : 'empty'}`}
            columns={cowCols} 
            onSave={handleSaveCows} 
            isSaving={isSaving} 
            entityName="Cow" 
            initialEntries={initialCows}
          />
        )}
        {activeTab === 'pastures' && (
          <DataGridEditor 
            key={`pastures-${initialPastures ? 'loaded' : 'empty'}`}
            columns={pastureColumns} 
            onSave={handleSavePastures} 
            isSaving={isSaving} 
            entityName="Pasture" 
            initialEntries={initialPastures}
          />
        )}
        {activeTab === 'activities' && (
          <DataGridEditor 
            key={`activities-${initialActivities ? 'loaded' : 'empty'}`}
            columns={activityColumns} 
            onSave={handleSaveActivities} 
            isSaving={isSaving} 
            entityName="Activity" 
            initialEntries={initialActivities}
          />
        )}
      </div>
    </div>
  );
};

export default DataGrid;
