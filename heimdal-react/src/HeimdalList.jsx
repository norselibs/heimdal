import React from 'react';
import { Link } from 'react-router-dom';

export default function HeimdalList({ schema, navigate }) {
  return (
    <div className="hm-list">
      <div className="hm-list-toolbar">
        {(schema.actions ?? []).map(a => (
          <Link key={a.url} to={a.url} className="hm-list-action">{a.label}</Link>
        ))}
      </div>
      <table className="hm-table">
        <thead>
          <tr>
            {(schema.columns ?? []).map(c => <th key={c.name}>{c.label}</th>)}
            <th />
          </tr>
        </thead>
        <tbody>
          {(schema.rows ?? []).map((row, i) => (
            <tr key={i}>
              {(schema.columns ?? []).map(c => (
                <td key={c.name}>{row[c.name] ?? ''}</td>
              ))}
              <td>
                {(row._rowActions ?? []).map(a => (
                  <Link key={a.url} to={a.url} className="hm-row-action">{a.label}</Link>
                ))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
