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

  await page.getByRole('link', { name: 'Users' }).click()
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
  await page.getByRole('link', { name: 'Users' }).click()
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
  await expect(page.getByRole('link', { name: 'Users' })).toHaveCount(0)

  await page.goto('/users')
  await expect(page).toHaveURL('/')
})
