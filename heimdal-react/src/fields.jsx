import React, { useState } from 'react';

// ---------------------------------------------------------------------------
// Field registry — maps component names to React components.
// Register custom components: registerField('hm-rating-field', RatingField)
// ---------------------------------------------------------------------------

const registry = {};

export function registerField(componentName, Component) {
  registry[componentName] = Component;
}

function resolve(componentName) {
  return registry[componentName] ?? FallbackField;
}

// ---------------------------------------------------------------------------
// Render entry point called by HeimdalForm
// ---------------------------------------------------------------------------

export function renderItem(item, values, errors, update, onBlur, onUpdate) {
  if (!item.name) return null; // layout item — skip for now
  const Component = resolve(item.component);
  return (
    <Component
      key={item.name}
      def={item}
      value={values[item.name] ?? ''}
      errors={errors[item.name] ?? []}
      onChange={v => update(item.name, v)}
      onBlur={item.validateOn === 'blur' ? () => onBlur(item.name) : undefined}
      onUpdate={item.triggersUpdate ? () => onUpdate(item.name) : undefined}
    />
  );
}

// ---------------------------------------------------------------------------
// Shared wrapper
// ---------------------------------------------------------------------------

function FieldWrapper({ label, required, errors, children }) {
  return (
    <div className="hm-field">
      <span className="hm-label">{label}{required && ' *'}</span>
      {children}
      {errors.length > 0 && <span className="hm-error">{errors[0]}</span>}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Built-in field components
// ---------------------------------------------------------------------------

function TextField({ def, value, errors, onChange, onBlur, onUpdate }) {
  return (
    <FieldWrapper label={def.label} required={def.required} errors={errors}>
      <input
        type="text"
        name={def.name}
        value={value}
        readOnly={def.readonly}
        onChange={e => onChange(e.target.value)}
        onBlur={() => { onBlur?.(); onUpdate?.(); }}
      />
    </FieldWrapper>
  );
}

function TextareaField({ def, value, errors, onChange, onBlur }) {
  return (
    <FieldWrapper label={def.label} required={def.required} errors={errors}>
      <textarea
        name={def.name}
        value={value}
        readOnly={def.readonly}
        onChange={e => onChange(e.target.value)}
        onBlur={onBlur}
      />
    </FieldWrapper>
  );
}

function NumberField({ def, value, errors, onChange, onBlur, onUpdate }) {
  return (
    <FieldWrapper label={def.label} required={def.required} errors={errors}>
      <input
        type="number"
        name={def.name}
        value={value}
        readOnly={def.readonly}
        onChange={e => onChange(e.target.value)}
        onBlur={() => { onBlur?.(); onUpdate?.(); }}
      />
    </FieldWrapper>
  );
}

function DateField({ def, value, errors, onChange, onBlur }) {
  return (
    <FieldWrapper label={def.label} required={def.required} errors={errors}>
      <input
        type="date"
        name={def.name}
        value={value}
        readOnly={def.readonly}
        onChange={e => onChange(e.target.value)}
        onBlur={onBlur}
      />
    </FieldWrapper>
  );
}

function CheckboxField({ def, value, errors, onChange }) {
  return (
    <FieldWrapper label={def.label} required={def.required} errors={errors}>
      <input
        type="checkbox"
        name={def.name}
        checked={value === 'true'}
        disabled={def.readonly}
        onChange={e => onChange(String(e.target.checked))}
      />
    </FieldWrapper>
  );
}

function SelectField({ def, value, errors, onChange, onUpdate }) {
  return (
    <FieldWrapper label={def.label} required={def.required} errors={errors}>
      <select
        name={def.name}
        value={value}
        disabled={def.readonly}
        onChange={e => { onChange(e.target.value); onUpdate?.(); }}
      >
        <option value="">— select —</option>
        {(def.options ?? []).map(o => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </FieldWrapper>
  );
}

function CollectionField({ def, value, errors, onChange }) {
  const columns = def.columns ?? [];
  const [rows, setRows] = useState(() => {
    try { return Array.isArray(value) ? value : JSON.parse(value || '[]'); }
    catch { return []; }
  });

  const commit = (newRows) => { setRows(newRows); onChange(newRows); };

  const addRow = () => commit([...rows, {}]);
  const removeRow = i => commit(rows.filter((_, idx) => idx !== i));
  const updateCell = (i, col, v) => {
    const next = rows.map((r, idx) => idx === i ? { ...r, [col]: v } : r);
    commit(next);
  };

  return (
    <div className="hm-field hm-collection-field">
      <span className="hm-label">{def.label}{def.required && ' *'}</span>
      <table className="hm-collection-table">
        <thead>
          <tr>
            {columns.map(c => <th key={c.name}>{c.label}</th>)}
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i}>
              {columns.map(c => (
                <td key={c.name}>
                  <input
                    type={c.type === 'integer' || c.type === 'decimal' ? 'number' : c.type === 'date' ? 'date' : 'text'}
                    value={row[c.name] ?? ''}
                    onChange={e => updateCell(i, c.name, e.target.value)}
                  />
                </td>
              ))}
              <td><button type="button" onClick={() => removeRow(i)}>✕</button></td>
            </tr>
          ))}
        </tbody>
      </table>
      <button type="button" className="hm-collection-add" onClick={addRow}>+ Add row</button>
      {errors.length > 0 && <span className="hm-error">{errors[0]}</span>}
    </div>
  );
}

function FallbackField({ def, value, errors, onChange, onBlur }) {
  return (
    <FieldWrapper label={`${def.label} (${def.component})`} required={def.required} errors={errors}>
      <input type="text" name={def.name} value={value}
             onChange={e => onChange(e.target.value)} onBlur={onBlur} />
    </FieldWrapper>
  );
}

// Register built-ins
registerField('hm-text-field',       TextField);
registerField('hm-textarea-field',   TextareaField);
registerField('hm-number-field',     NumberField);
registerField('hm-integer-field',    NumberField);
registerField('hm-decimal-field',    NumberField);
registerField('hm-date-field',       DateField);
registerField('hm-checkbox-field',   CheckboxField);
registerField('hm-select-field',     SelectField);
registerField('hm-collection-field', CollectionField);
