import React, { useState, useRef, useCallback } from 'react';
import { evaluate, flattenFields } from './predicates.js';
import { renderItem } from './fields.jsx';

export default function HeimdalForm({ schema, navigate }) {
  // Initialise state from schema field values
  const [values, setValues]   = useState(() => {
    const init = {};
    flattenFields(schema.items).forEach(f => { init[f.name] = f.value ?? ''; });
    return init;
  });
  const [errors,   setErrors]   = useState({});
  const [sections, setSections] = useState({}); // sectionId → visible override from server
  const seqRef = useRef(0);

  const update = useCallback((name, value) => {
    setValues(prev => ({ ...prev, [name]: value }));
  }, []);

  // ----- validate event -----
  const handleBlur = useCallback(async (fieldName) => {
    const seq = ++seqRef.current;
    try {
      const res = await fetch(schema.eventEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: 'validate', field: fieldName, seq, values })
      });
      const result = await res.json();
      if (result.seq < seqRef.current) return;
      setErrors(prev => ({ ...prev, ...(result.errors ?? {}) }));
    } catch { /* network error — leave errors unchanged */ }
  }, [schema.eventEndpoint, values]);

  // ----- update event (triggersUpdate fields) -----
  const handleUpdate = useCallback(async (fieldName) => {
    const seq = ++seqRef.current;
    try {
      const res = await fetch(schema.eventEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: 'update', field: fieldName, seq, values })
      });
      const result = await res.json();
      if (result.seq < seqRef.current) return;
      if (result.sections) setSections(result.sections);
    } catch {}
  }, [schema.eventEndpoint, values]);

  // ----- action (submit / save draft) -----
  const handleAction = useCallback(async (url) => {
    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(values)
      });
      if (res.status === 422) {
        const body = await res.json();
        setErrors(body.errors ?? {});
      } else if (res.ok) {
        const body = await res.json();
        if (body.redirect) navigate(body.redirect);
      }
    } catch {}
  }, [values, navigate]);

  // ----- section visibility -----
  const isSectionVisible = (item) => {
    if (sections[item.section] !== undefined) return sections[item.section];
    return evaluate(item.visibleWhen, values);
  };

  // ----- allRequiredFieldsValid (for enabledWhen) -----
  const allRequiredValid = () =>
    flattenFields(schema.items)
      .filter(f => {
        const sec = schema.items.find(i => i.section && i.items?.some(fi => fi.name === f.name));
        if (sec && !isSectionVisible(sec)) return false;
        return f.required;
      })
      .every(f => (values[f.name] ?? '').toString().trim() !== '');

  const evalEnabled = (pred) => {
    if (!pred) return true;
    if (pred.op === 'allRequiredValid') return allRequiredValid();
    return evaluate(pred, values);
  };

  // ----- render -----
  return (
    <div className="hm-form">
      {schema.items.map((item, i) => {
        if (item.section !== undefined) {
          const visible = isSectionVisible(item);
          return (
            <fieldset key={item.section} className="hm-section" hidden={!visible}>
              {item.label && <legend className="hm-section-label">{item.label}</legend>}
              {item.items.map(f => renderItem(f, values, errors, update, handleBlur, handleUpdate))}
            </fieldset>
          );
        }
        return renderItem(item, values, errors, update, handleBlur, handleUpdate);
      })}

      <div className="hm-actions">
        {(schema.actions ?? []).map(action => (
          <button
            key={action.url ?? action.label}
            type="button"
            disabled={!evalEnabled(action.enabledWhen)}
            onClick={() => handleAction(action.url)}
          >
            {action.label}
          </button>
        ))}
      </div>
    </div>
  );
}
