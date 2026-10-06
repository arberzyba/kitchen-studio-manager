import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible()
}

test('order shows its progress, items and totals', async ({ page }) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page.getByRole('link', { name: 'Orders', exact: true }).click()
  await page.getByRole('gridcell', { name: 'Sabine Müller' }).click()

  await expect(
    page.getByRole('heading', { name: /Order AU-\d{4}-0001/ }),
  ).toBeVisible()
  // The demo order has been measured, so ordering from the supplier is the next step
  await expect(
    page.getByRole('button', { name: 'Mark as: Ordered from supplier' }),
  ).toBeVisible()
  await expect(page.getByRole('cell', { name: 'EG-IK-80' })).toBeVisible()
  await expect(page.getByText('€6,862.02')).toBeVisible()
  await expect(page.getByRole('link', { name: /AN-\d{4}-0001/ })).toBeVisible()
})

test('accepted quote links to its order', async ({ page }) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Quotes' }).click()
  await page.getByRole('gridcell', { name: 'Sabine Müller' }).click()
  await page.getByRole('link', { name: /View order AU-\d{4}-0001/ }).click()

  await expect(
    page.getByRole('heading', { name: /Order AU-\d{4}-0001/ }),
  ).toBeVisible()
})

test('order list can be filtered by status', async ({ page }) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Orders', exact: true }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Sabine Müller' }),
  ).toBeVisible()

  await page.getByLabel('Status').click()
  await page.getByRole('option', { name: 'Completed' }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Sabine Müller' }),
  ).toHaveCount(0)
})

test('installer has no access to orders', async ({ page }) => {
  await signIn(page, 'installer@sedzkitchens.de')
  await expect(
    page.getByRole('link', { name: 'Orders', exact: true }),
  ).toHaveCount(0)

  await page.goto('/orders')
  await expect(page).toHaveURL('/')
})
