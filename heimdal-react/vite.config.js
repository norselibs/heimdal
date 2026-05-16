import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Proxy all backend routes to the Java server during development.
// Run the Java server first: ./gradlew :heimdal-integration-test:run
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/bikes':   'http://localhost:8080',
      '/claims':  'http://localhost:8080',
      '/heimdal': 'http://localhost:8080',
    }
  }
});
