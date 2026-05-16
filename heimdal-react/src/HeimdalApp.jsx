import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Routes, Route } from 'react-router-dom';
import HeimdalForm from './HeimdalForm.jsx';
import HeimdalList from './HeimdalList.jsx';

/**
 * Top-level app shell. Fetches the Heimdal schema from the current path
 * and renders either a form or a list based on what comes back.
 */
function HeimdalPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [schema, setSchema] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetch(location.pathname + location.search, {
      headers: { 'Accept': 'application/json' }
    })
      .then(r => {
        if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
        return r.json();
      })
      .then(s => { setSchema(s); setLoading(false); })
      .catch(e => { setError(e.message); setLoading(false); });
  }, [location.pathname, location.search]);

  if (loading) return <div className="hm-loading">Loading…</div>;
  if (error)   return <div className="hm-error-page">Error: {error}</div>;
  if (!schema) return null;

  if (schema.listId) return <HeimdalList schema={schema} navigate={navigate} />;
  if (schema.formId) return <HeimdalForm schema={schema} navigate={navigate} />;
  return <pre>{JSON.stringify(schema, null, 2)}</pre>;
}

export default function HeimdalApp() {
  return (
    <Routes>
      <Route path="*" element={<HeimdalPage />} />
    </Routes>
  );
}
