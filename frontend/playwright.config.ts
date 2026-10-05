import { defineConfig, devices } from '@playwright/test'

// End-to-end tests need the backend running on the dev profile (it provides the demo logins)
export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:5173',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
  },
})
