import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string, password = 'demo1234') {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
}

test('visitor is sent to the login page', async ({ page }) => {
  await page.goto('/users')
  await expect(page).toHaveURL('/login')
})

test('wrong password shows an error', async ({ page }) => {
  await signIn(page, 'admin@sedzkitchens.de', 'wrong-password')
  await expect(page.getByText('Invalid email or password')).toBeVisible()
})

test('admin signs in, sees the user list and signs out', async ({ page }) => {
  await signIn(page, 'admin@sedzkitchens.de')
  await expect(
    page.getByRole('heading', { name: 'Welcome, Anna' }),
  ).toBeVisible()

  await page.getByRole('link', { name: 'Users', exact: true }).click()
  await expect(
    page.getByRole('cell', { name: 'installer@sedzkitchens.de', exact: true }),
  ).toBeVisible()

  // The session survives a page reload
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible()

  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page).toHaveURL('/login')
})

test('new user form validates its input', async ({ page }) => {
  await signIn(page, 'admin@sedzkitchens.de')
  await page.getByRole('link', { name: 'Users', exact: true }).click()
  await page.getByRole('button', { name: 'New user' }).click()
  await page.getByRole('button', { name: 'Save' }).click()

  await expect(page.getByText('Enter a valid email address')).toBeVisible()
  await expect(page.getByText('Use at least 8 characters')).toBeVisible()
})

test('installer has no access to user management', async ({ page }) => {
  await signIn(page, 'installer@sedzkitchens.de')
  await expect(
    page.getByRole('heading', { name: 'Welcome, Jonas' }),
  ).toBeVisible()
  await expect(
    page.getByRole('link', { name: 'Users', exact: true }),
  ).toHaveCount(0)

  await page.goto('/users')
  await expect(page).toHaveURL('/')
})

test('language can be switched to German and is remembered', async ({
  page,
}) => {
  await page.goto('/login')
  await page.getByRole('button', { name: 'Deutsch' }).click()
  await expect(page.getByRole('button', { name: 'Anmelden' })).toBeVisible()

  await page.reload()
  await expect(page.getByLabel('Passwort')).toBeVisible()

  await page.getByLabel('E-Mail').fill('admin@sedzkitchens.de')
  await page.getByLabel('Passwort').fill('demo1234')
  await page.getByRole('button', { name: 'Anmelden' }).click()
  await expect(
    page.getByRole('heading', { name: 'Willkommen, Anna' }),
  ).toBeVisible()
  await expect(page.getByText('Anna Schneider (Administrator)')).toBeVisible()
  await page.screenshot({ path: 'test-results/german.png' })
})

test('expired session returns to a login page that works', async ({ page }) => {
  // Simulates a browser that still holds a token from an earlier, expired session
  await page.goto('/login')
  await page.evaluate(() => localStorage.setItem('token', 'expired-token'))
  await page.goto('/customers')
  await expect(page).toHaveURL('/login')

  await page.getByLabel('Email').fill('sales@sedzkitchens.de')
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(
    page.getByRole('heading', { name: 'Welcome, Lukas' }),
  ).toBeVisible()
})
