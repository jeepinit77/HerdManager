import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Eye } from 'lucide-react';
import { v4 as uuidv4 } from 'uuid';

export interface ColumnDef {
  key: string;
  label: string;
  type: 'text' | 'date' | 'select' | 'number';
  options?: (string | { label: string; value: string })[]; // for select
  required?: boolean;
  default?: string;
  hidden?: boolean;
}

interface DataGridEditorProps {
  columns: ColumnDef[];
  onSave: (entries: any[]) => Promise<void>;
  isSaving: boolean;
  entityName: string;
  initialEntries?: any[];
}

const DataGridEditor: React.FC<DataGridEditorProps> = ({ columns, onSave, isSaving, entityName, initialEntries }) => {
  const [columnVisibility, setColumnVisibility] = useState<Record<string, boolean>>(
    columns.reduce((acc, col) => ({ ...acc, [col.key]: !col.hidden }), {})
  );
  const [showColumnToggle, setShowColumnToggle] = useState(false);

  const createEmptyRow = (lastRow?: any) => {
    const newRow: any = { id: uuidv4() };
    columns.forEach(col => {
      if (lastRow && lastRow[col.key] !== undefined && col.type === 'select') {
        newRow[col.key] = lastRow[col.key]; // Copy select defaults
      } else {
        newRow[col.key] = col.default || '';
      }
    });
    return newRow;
  };

  const [entries, setEntries] = useState<any[]>(() => {
    if (initialEntries && initialEntries.length > 0) {
      return initialEntries;
    }
    return [createEmptyRow(), createEmptyRow(), createEmptyRow()];
  });

  const addRow = () => {
    const lastRow = entries[entries.length - 1];
    setEntries([...entries, createEmptyRow(lastRow)]);
  };

  const removeRow = (id: string) => {
    if (entries.length > 1) {
      setEntries(entries.filter(e => e.id !== id));
    }
  };

  const updateEntry = (id: string, field: string, value: string) => {
    setEntries(prevEntries => prevEntries.map(e => {
      if (e.id !== id) return e;

      const updated = { ...e, [field]: value };

      // Validation Rule: Gender and Classification auto-correction
      if (field === 'classification') {
        if (['BULL', 'STEER'].includes(value)) {
          updated.gender = 'MALE';
        } else if (['COW', 'HEIFER'].includes(value)) {
          updated.gender = 'FEMALE';
        }
      } else if (field === 'gender') {
        if (value === 'MALE' && ['COW', 'HEIFER'].includes(updated.classification)) {
          updated.classification = 'BULL'; // default fallback
        } else if (value === 'FEMALE' && ['BULL', 'STEER'].includes(updated.classification)) {
          updated.classification = 'COW';
        }
      }

      return updated;
    }));
  };

  const toggleColumn = (key: string) => {
    setColumnVisibility(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const handleSave = async () => {
    const requiredCols = columns.filter(c => c.required);
    const validEntries = entries.filter(e => {
      return requiredCols.every(col => e[col.key] !== undefined && e[col.key].toString().trim() !== '');
    });

    if (validEntries.length === 0) {
      alert(`Please fill in the required fields for at least one ${entityName}.`);
      return;
    }

    await onSave(validEntries);
    
    // Only reset if we were in "create new" mode (i.e. no initialEntries)
    if (!initialEntries) {
      setEntries([createEmptyRow()]);
    }
  };

  const visibleColumns = columns.filter(col => columnVisibility[col.key]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', height: '100%', overflow: 'hidden' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button onClick={addRow} className="btn btn-secondary" disabled={isSaving}>
            <Plus size={18} /> Add Row
          </button>
          
          <div style={{ position: 'relative' }}>
            <button 
              onClick={() => setShowColumnToggle(!showColumnToggle)} 
              className="btn btn-secondary"
            >
              <Eye size={18} /> Columns
            </button>
            {showColumnToggle && (
              <div className="glass-card" style={{ 
                position: 'absolute', top: '100%', left: 0, marginTop: '0.5rem', 
                padding: '1rem', zIndex: 50, minWidth: '200px',
                display: 'flex', flexDirection: 'column', gap: '0.5rem'
              }}>
                <h4 style={{ fontSize: '0.875rem', color: 'var(--color-text-muted)', marginBottom: '0.5rem' }}>Visible Columns</h4>
                {columns.map(col => (
                  <label key={col.key} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: col.required ? 'not-allowed' : 'pointer' }}>
                    <input 
                      type="checkbox" 
                      checked={columnVisibility[col.key]}
                      onChange={() => !col.required && toggleColumn(col.key)}
                      disabled={col.required}
                    />
                    <span style={{ color: col.required ? 'var(--color-text-muted)' : 'inherit' }}>
                      {col.label} {col.required && '*'}
                    </span>
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>
        
        <button onClick={handleSave} className="btn btn-primary" disabled={isSaving}>
          {isSaving ? 'Saving...' : `Save ${entityName}s`}
        </button>
      </div>

      <div className="table-container" style={{ flex: 1, overflowY: 'auto' }}>
        <table>
          <thead style={{ position: 'sticky', top: 0, zIndex: 10 }}>
            <tr>
              <th style={{ width: '50px' }}>#</th>
              {visibleColumns.map(col => (
                <th key={col.key}>{col.label} {col.required && '*'}</th>
              ))}
              <th style={{ width: '50px' }}></th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry, index) => (
              <tr key={entry.id}>
                <td style={{ color: 'var(--color-text-muted)', textAlign: 'center' }}>{index + 1}</td>
                {visibleColumns.map(col => (
                  <td key={col.key}>
                    {col.type === 'select' ? (
                      <select
                        value={entry[col.key]}
                        onChange={(e) => updateEntry(entry.id, col.key, e.target.value)}
                      >
                        {col.options?.map(opt => {
                          const label = typeof opt === 'string' ? opt : opt.label;
                          const value = typeof opt === 'string' ? opt : opt.value;
                          return <option key={value} value={value}>{label}</option>;
                        })}
                      </select>
                    ) : (
                      <input
                        type={col.type}
                        value={entry[col.key]}
                        onChange={(e) => updateEntry(entry.id, col.key, e.target.value)}
                        placeholder={`Enter ${col.label.toLowerCase()}`}
                        autoFocus={index === entries.length - 1 && col.key === visibleColumns[0].key}
                      />
                    )}
                  </td>
                ))}
                <td>
                  <button 
                    onClick={() => removeRow(entry.id)}
                    className="btn btn-danger"
                    style={{ padding: '0.5rem' }}
                    title="Remove Row"
                  >
                    <Trash2 size={16} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default DataGridEditor;
