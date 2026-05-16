import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Routes, Route, Link } from 'react-router-dom';
import HeimdalForm from './HeimdalForm.jsx';
import HeimdalList from './HeimdalList.jsx';

function Nav({ appName, items, currentPath }) {
  if (!appName && (!items || items.length === 0)) return null;
  return (
    <nav className="hm-nav">
      {appName && (
        <span className="hm-nav-brand" dangerouslySetInnerHTML={{ __html: appName }} />
      )}
      {(items ?? []).map(item => {
        const active = currentPath === item.url || currentPath.startsWith(item.url + '/');
        return (
          <Link
            key={item.url}
            to={item.url}
            className={'hm-nav-item' + (active ? ' hm-nav-item--active' : '')}
          >
            {item.iconHtml && (
              <span dangerouslySetInnerHTML={{ __html: item.iconHtml }} />
            )}
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}

function HeimdalPage({ nav, setNav }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [schema, setSchema] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    const sep = location.search ? '&' : '?';
    fetch(location.pathname + location.search + sep + 'format=json')
      .then(r => {
        if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
        return r.json();
      })
      .then(s => {
        // Extract nav data once and share across page transitions
        if (s.appName || s.navItems) {
          setNav({ appName: s.appName, items: s.navItems });
        }
        setSchema(s);
        setLoading(false);
      })
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
  const [nav, setNav] = useState({ appName: null, items: [] });
  const location = useLocation();

  return (
    <>
      <Nav appName={nav.appName} items={nav.items} currentPath={location.pathname} />
      <div className="hm-page">
        <Routes>
          <Route path="*" element={<HeimdalPage nav={nav} setNav={setNav} />} />
        </Routes>
      </div>
    </>
  );
}
