import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
export default defineConfig({
  plugins: [react()],
  base: '/',
  build: {
    rollupOptions: {
      input: {
        app: resolve(process.cwd(), 'index.html'),
        laboratorio: resolve(process.cwd(), 'anel.html')
      }
    }
  }
});
