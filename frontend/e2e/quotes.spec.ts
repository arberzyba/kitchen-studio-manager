import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible()
}

test('sales filters the quote list and opens an accepted quote', async ({
  page,
}) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Quotes' }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Thomas Schmidt' }),
  ).toBeVisible()

  await page.getByLabel('Status').click()
  await page.getByRole('option', { name: 'Accepted' }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Thomas Schmidt' }),
  ).toHaveCount(0)
  await page.getByRole('gridcell', { name: 'Sabine Müller' }).click()

  await expect(
    page.getByRole('heading', { name: /Quote AN-\d{4}-0001/ }),
  ).toBeVisible()
  await expect(page.getByText('Discount 5 %')).toBeVisible()
  await expect(page.getByText('€5,766.40')).toBeVisible()
  await expect(page.getByText('€1,095.62')).toBeVisible()
  await expect(page.getByText('€6,862.02')).toBeVisible()
  // An accepted quote can no longer be edited or sent
  await expect(page.getByRole('link', { name: 'Edit' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: /Send/ })).toHaveCount(0)
})

test('quote editor calculates net, VAT and gross while typing', async ({
  page,
}) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.goto('/quotes/new')

  await page.getByLabel('Customer', { exact: true }).fill('Fischer')
  await page.getByRole('option', { name: 'Andreas Fischer' }).click()
  await page.getByLabel(/Add product/).fill('induktion')
  await page.getByRole('option', { name: /EG-IK-80/ }).click()
  await page.getByLabel(/Add product/).fill('quarz')
  await page.getByRole('option', { name: /AP-QZ-20/ }).click()

  // 1 x 749.00 + 2.5 m x 369.00 = 1671.50; less 10 % = 1504.35; VAT 285.83; gross 1790.18
  await page
    .getByRole('row', { name: /AP-QZ-20/ })
    .getByRole('spinbutton')
    .first()
    .fill('2.5')
  await page.getByLabel('Overall discount').fill('10')
  await expect(page.getByText('€1,671.50')).toBeVisible()
  await expect(page.getByText('€1,504.35')).toBeVisible()
  await expect(page.getByText('€285.83')).toBeVisible()
  await expect(page.getByText('€1,790.18')).toBeVisible()

  await page.getByRole('button', { name: /Remove Induktionskochfeld/ }).click()
  await expect(page.getByText('€922.50')).toHaveCount(2)
})

test('quote form validates its input', async ({ page }) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.goto('/quotes/new')
  await page.getByRole('button', { name: 'Save' }).click()

  await expect(page.getByText('Choose a customer')).toBeVisible()
  await expect(page.getByText('Add at least one product')).toBeVisible()
  await expect(page).toHaveURL('/quotes/new')
})

test('quote PDF can be downloaded', async ({ page }) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Quotes' }).click()
  await page.getByRole('gridcell', { name: 'Sabine Müller' }).click()

  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: 'PDF' }).click()
  expect((await download).suggestedFilename()).toMatch(
    /^Angebot-AN-\d{4}-0001\.pdf$/,
  )
})

test('office can read quotes but not create them', async ({ page }) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page.getByRole('link', { name: 'Quotes' }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Sabine Müller' }),
  ).toBeVisible()
  await expect(page.getByRole('link', { name: 'New quote' })).toHaveCount(0)

  await page.goto('/quotes/new')
  await expect(page).toHaveURL('/')
})
