import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Only proxy requests that include ?format=json — these are API calls from
// the React app. Regular browser navigation (no query param) falls through
// to index.html so React Router handles the route client-side.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/bikes':   { target: 'http://localhost:8080', bypass: req => req.url.includes('format=json') ? null : req.url },
      '/claims':  { target: 'http://localhost:8080', bypass: req => req.url.includes('format=json') ? null : req.url },
      '/heimdal': { target: 'http://localhost:8080' },
    }
  }
});
