import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible()
}

test('sales searches customers and opens one with its contact history', async ({
  page,
}) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Customers' }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Müller, Sabine' }),
  ).toBeVisible()

  await page.getByLabel('Search name, company, email or city').fill('bonn')
  await expect(
    page.getByRole('gridcell', { name: 'Krüger, Michael' }),
  ).toBeVisible()
  await expect(
    page.getByRole('gridcell', { name: 'Müller, Sabine' }),
  ).toHaveCount(0)

  await page.getByRole('gridcell', { name: 'Krüger, Michael' }).click()
  await expect(
    page.getByRole('heading', { name: 'Mr Michael Krüger' }),
  ).toBeVisible()
  await expect(page.getByText('Kölnstraße 310')).toBeVisible()
  await expect(page.getByText(/drei baugleiche Küchenzeilen/)).toBeVisible()
})

test('customer form validates its input', async ({ page }) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.goto('/customers/new')
  await page.getByLabel('Postal code').fill('123')
  await page.getByRole('button', { name: 'Save' }).click()

  await expect(page.getByText('Enter a 5-digit postal code')).toBeVisible()
  await expect(page.getByText('This field is required').first()).toBeVisible()
  await expect(page).toHaveURL('/customers/new')
})

test('office can view customers but not edit them', async ({ page }) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page.getByRole('link', { name: 'Customers' }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Müller, Sabine' }),
  ).toBeVisible()
  await expect(page.getByRole('link', { name: 'New customer' })).toHaveCount(0)

  await page.goto('/customers/new')
  await expect(page).toHaveURL('/')
})

test('installer has no access to customers', async ({ page }) => {
  await signIn(page, 'installer@sedzkitchens.de')
  await expect(page.getByRole('link', { name: 'Customers' })).toHaveCount(0)

  await page.goto('/customers')
  await expect(page).toHaveURL('/')
})
