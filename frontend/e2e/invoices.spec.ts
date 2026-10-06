import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible()
}

test('office opens a partially paid invoice and downloads its PDF', async ({
  page,
}) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page.getByRole('link', { name: 'Invoices', exact: true }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Partially paid' }),
  ).toBeVisible()
  await page.getByRole('gridcell', { name: 'Frau Sabine Müller' }).click()

  await expect(
    page.getByRole('heading', { name: /Invoice RE-\d{4}-\d{4}/ }),
  ).toBeVisible()
  // 6,862.02 invoiced, 2,000.00 deposit received
  await expect(page.getByRole('cell', { name: 'Anzahlung' })).toBeVisible()
  await expect(page.getByText('€4,862.02')).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'Record payment' }),
  ).toBeVisible()

  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: 'PDF' }).click()
  expect((await download).suggestedFilename()).toMatch(
    /^Rechnung-RE-\d{4}-\d{4}\.pdf$/,
  )
})

test('invoice list can be narrowed to overdue invoices', async ({ page }) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page.getByRole('link', { name: 'Invoices', exact: true }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Frau Sabine Müller' }),
  ).toBeVisible()

  // The demo invoice is not due yet
  await page.getByLabel('Overdue only').check()
  await expect(
    page.getByRole('gridcell', { name: 'Frau Sabine Müller' }),
  ).toHaveCount(0)
})

test('order page links to its invoice', async ({ page }) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page.getByRole('link', { name: 'Orders', exact: true }).click()
  await page.getByRole('gridcell', { name: 'Sabine Müller' }).click()
  await expect(page.getByText('€4,862.02 open')).toBeVisible()
  await page.getByRole('link', { name: /RE-\d{4}-\d{4}/ }).click()

  await expect(
    page.getByRole('heading', { name: /Invoice RE-\d{4}-\d{4}/ }),
  ).toBeVisible()
})

test('sales can view invoices but not record payments or send them', async ({
  page,
}) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Invoices', exact: true }).click()
  await page.getByRole('gridcell', { name: 'Frau Sabine Müller' }).click()

  await expect(page.getByRole('cell', { name: 'Anzahlung' })).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'Record payment' }),
  ).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Send by email' })).toHaveCount(
    0,
  )
  await expect(
    page.getByRole('button', { name: /Remove payment/ }),
  ).toHaveCount(0)
})

test('installer has no access to invoices', async ({ page }) => {
  await signIn(page, 'installer@sedzkitchens.de')
  await expect(
    page.getByRole('link', { name: 'Invoices', exact: true }),
  ).toHaveCount(0)

  await page.goto('/invoices')
  await expect(page).toHaveURL('/')
})
