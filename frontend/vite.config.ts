/// <reference types="vitest" />
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react()],
  resolve: { alias: { '@': path.resolve(__dirname, 'src') } },
  server: {
    port: 5173,
    proxy: { '/api': { target: process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080', changeOrigin: true } },
  },
  build: {
    chunkSizeWarningLimit: 600,
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom'],
          tanstack: ['@tanstack/react-query', '@tanstack/react-router', '@tanstack/react-table'],
          charts: ['recharts'],
        },
      },
    },
  },
  test: { environment: 'jsdom', globals: true, setupFiles: ['./src/test/setup.ts'], css: false },
});
