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
  onRowClick?: (entry: any) => void;
}

const DataGridEditor: React.FC<DataGridEditorProps> = ({ columns, onSave, isSaving, entityName, initialEntries, onRowClick }) => {
  const storageKey = `columnVisibility_${entityName}`;
  const [columnVisibility, setColumnVisibility] = useState<Record<string, boolean>>(() => {
    const saved = localStorage.getItem(storageKey);
    if (saved) return JSON.parse(saved);
    return columns.reduce((acc, col) => ({ ...acc, [col.key]: !col.hidden }), {});
  });

  useEffect(() => {
    localStorage.setItem(storageKey, JSON.stringify(columnVisibility));
  }, [columnVisibility, storageKey]);

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

  const isRowEmpty = (row: any) => {
    return columns.every(col => {
      const val = row[col.key];
      return val === undefined || val === '' || val === col.default;
    });
  };

  const [entries, setEntries] = useState<any[]>(() => {
    if (initialEntries && initialEntries.length > 0) {
      return initialEntries;
    }
    return [createEmptyRow()];
  });

  useEffect(() => {
    if (initialEntries && initialEntries.length > 0) {
      setEntries(initialEntries);
    } else if (initialEntries && initialEntries.length === 0) {
      setEntries([createEmptyRow()]);
    }
  }, [initialEntries]);

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
    setEntries(prevEntries => {
      const newEntries = prevEntries.map(e => {
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
            updated.classification = 'BULL'; 
          } else if (value === 'FEMALE' && ['BULL', 'STEER'].includes(updated.classification)) {
            updated.classification = 'COW';
          }
        }

        return updated;
      });

      // Auto-add row if editing the last row and it's not empty anymore
      const editedRowIndex = newEntries.findIndex(e => e.id === id);
      if (editedRowIndex === newEntries.length - 1 && !isRowEmpty(newEntries[editedRowIndex])) {
        newEntries.push(createEmptyRow(newEntries[editedRowIndex]));
      }

      return newEntries;
    });
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
              <th style={{ width: '100px' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry, index) => (
              <tr key={entry.id} className={onRowClick ? 'hover-bg' : ''} style={{ cursor: onRowClick ? 'pointer' : 'default' }}>
                <td style={{ color: 'var(--color-text-muted)', textAlign: 'center' }}>{index + 1}</td>
                {visibleColumns.map(col => (
                  <td key={col.key} onClick={(e) => {
                    // Don't trigger row click if clicking an input/select
                    if (e.target === e.currentTarget) onRowClick?.(entry);
                  }}>
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
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    {onRowClick && (
                      <button 
                        onClick={() => onRowClick(entry)}
                        className="btn btn-secondary"
                        style={{ padding: '0.5rem' }}
                        title="View Details"
                      >
                        <Eye size={16} />
                      </button>
                    )}
                    <button 
                      onClick={() => removeRow(entry.id)}
                      className="btn btn-danger"
                      style={{ padding: '0.5rem' }}
                      title="Remove Row"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
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
