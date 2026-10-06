import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible()
}

async function openCustomer(page: Page, name: string) {
  await page.getByRole('link', { name: 'Customers', exact: true }).click()
  await page.getByRole('gridcell', { name }).click()
}

test('admin exports a customer and the export appears in the audit log', async ({
  page,
}) => {
  await signIn(page, 'admin@sedzkitchens.de')
  await openCustomer(page, 'Müller, Sabine')

  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Export data' }).click()
  expect((await download).suggestedFilename()).toMatch(
    /^customer-\d+-data\.json$/,
  )

  await page.getByRole('link', { name: 'Audit log', exact: true }).click()
  await page.getByLabel('Action').click()
  await page.getByRole('option', { name: 'Data exported' }).click()
  const newest = page.getByRole('row').nth(1)
  await expect(newest).toContainText('Anna Schneider')
  await expect(newest).toContainText('Data exported')
  await expect(newest).toContainText(/Customer #\d+/)
})

test('audit log lists changes and can be filtered by record type', async ({
  page,
}) => {
  await signIn(page, 'admin@sedzkitchens.de')
  await page.getByRole('link', { name: 'Audit log', exact: true }).click()
  // The demo data was created by the system, not by a user
  await expect(
    page.getByRole('gridcell', { name: 'System' }).first(),
  ).toBeVisible()

  await page.getByLabel('Record').click()
  await page.getByRole('option', { name: 'Payment' }).click()
  await expect(
    page.getByRole('gridcell', { name: /^Payment #\d+$/ }).first(),
  ).toBeVisible()
  await expect(
    page.getByRole('gridcell', { name: /^Invoice #\d+$/ }),
  ).toHaveCount(0)
})

test('erasing customer data asks for confirmation and can be cancelled', async ({
  page,
}) => {
  await signIn(page, 'admin@sedzkitchens.de')
  await openCustomer(page, 'Müller, Sabine')
  await page.getByRole('button', { name: 'Erase data' }).click()

  const dialog = page.getByRole('dialog')
  await expect(
    dialog.getByText('Erase the data of Sabine Müller?'),
  ).toBeVisible()
  await expect(
    dialog.getByText(/kept for the legal retention period/),
  ).toBeVisible()
  await dialog.getByRole('button', { name: 'Cancel' }).click()

  await expect(dialog).toHaveCount(0)
  await expect(
    page.getByRole('heading', { name: 'Ms Sabine Müller' }),
  ).toBeVisible()
})

test('only admins see the privacy actions and the audit log', async ({
  page,
}) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await expect(
    page.getByRole('link', { name: 'Audit log', exact: true }),
  ).toHaveCount(0)
  await openCustomer(page, 'Müller, Sabine')
  await expect(
    page.getByRole('heading', { name: 'Contact history' }),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: 'Export data' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Erase data' })).toHaveCount(0)

  await page.goto('/audit-log')
  await expect(page).toHaveURL('/')
})
