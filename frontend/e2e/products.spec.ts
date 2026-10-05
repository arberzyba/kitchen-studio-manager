import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible()
}

test('sales browses the catalog with search and category filter', async ({
  page,
}) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Products' }).click()
  await expect(page.getByRole('gridcell', { name: 'EG-BO-60' })).toBeVisible()
  await expect(
    page.getByRole('gridcell', { name: '€599.00 / pc' }),
  ).toBeVisible()

  await page.getByLabel('Category').click()
  await page.getByRole('option', { name: 'Worktop' }).click()
  await expect(page.getByRole('gridcell', { name: 'AP-QZ-20' })).toBeVisible()
  await expect(page.getByRole('gridcell', { name: 'EG-BO-60' })).toHaveCount(0)

  await page.getByLabel('Search name or article number').fill('eiche')
  await expect(page.getByRole('gridcell', { name: 'AP-EI-38' })).toBeVisible()
  await expect(page.getByRole('gridcell', { name: 'AP-QZ-20' })).toHaveCount(0)

  // Sales staff cannot change the catalog
  await expect(page.getByRole('button', { name: 'New product' })).toHaveCount(0)
})

test('prices use German formatting in German', async ({ page }) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('button', { name: 'Deutsch' }).click()
  await page.getByRole('link', { name: 'Produkte' }).click()
  await expect(
    page.getByRole('gridcell', { name: '599,00 € / Stk.' }),
  ).toBeVisible()
})

test('admin product form validates its input', async ({ page }) => {
  await signIn(page, 'admin@sedzkitchens.de')
  await page.getByRole('link', { name: 'Products' }).click()
  await page.getByRole('button', { name: 'New product' }).click()
  await page.getByRole('button', { name: 'Save' }).click()

  await expect(page.getByText('This field is required').first()).toBeVisible()
  await expect(
    page.getByText('Enter a price of 0 or more').first(),
  ).toBeVisible()
  await expect(page.getByText('Choose a supplier')).toBeVisible()
})

test('suppliers are listed and only admins can edit them', async ({ page }) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page.getByRole('link', { name: 'Suppliers' }).click()
  await expect(
    page.getByRole('cell', { name: 'Westfalen Arbeitsplatten KG' }),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: 'New supplier' })).toHaveCount(
    0,
  )
})
