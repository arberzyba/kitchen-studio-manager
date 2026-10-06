import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Forward API calls to the Spring Boot backend so the browser needs no CORS setup in dev
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    // Unit tests live next to the code; the browser tests in e2e/ are run by Playwright
    include: ['src/**/*.test.ts'],
  },
})
