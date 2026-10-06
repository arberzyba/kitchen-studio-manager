import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible()
}

test('office looks up a supplier order with its items and purchase value', async ({
  page,
}) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page.getByRole('link', { name: 'Supplier orders' }).click()
  await expect(
    page.getByRole('gridcell', { name: 'Westfalen Arbeitsplatten KG' }),
  ).toBeVisible()

  await page
    .getByLabel('Search number, supplier, order or customer')
    .fill('rheinland')
  await expect(
    page.getByRole('gridcell', { name: 'Westfalen Arbeitsplatten KG' }),
  ).toHaveCount(0)
  await page
    .getByRole('gridcell', { name: 'Rheinland Küchenmöbel GmbH' })
    .click()

  await expect(
    page.getByRole('heading', { name: /Supplier order BE-\d{4}-0001/ }),
  ).toBeVisible()
  // 4 x 112.00 + 2 x 198.00 + 5 x 84.00 + 1 x 265.00 at purchase prices
  await expect(page.getByRole('cell', { name: 'US-90-W' })).toBeVisible()
  await expect(page.getByRole('cell', { name: '€1,529.00' })).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'Mark as delivered' }),
  ).toBeVisible()
})

test('order page shows placed and open supplier orders', async ({ page }) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page.getByRole('link', { name: 'Orders', exact: true }).click()
  await page.getByRole('gridcell', { name: 'Sabine Müller' }).click()

  await expect(page.getByRole('link', { name: /BE-\d{4}-0001/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /BE-\d{4}-0002/ })).toBeVisible()
  // The appliances have not been ordered yet
  await expect(page.getByText('Hausgeräte Nord Vertriebs GmbH')).toBeVisible()
  await expect(page.getByText('3 items to order')).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'Order', exact: true }),
  ).toBeVisible()
})

test('sales can see supplier orders but not place or change them', async ({
  page,
}) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Orders', exact: true }).click()
  await page.getByRole('gridcell', { name: 'Sabine Müller' }).click()
  await expect(page.getByText('3 items to order')).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'Order', exact: true }),
  ).toHaveCount(0)

  await page.getByRole('link', { name: /BE-\d{4}-0001/ }).click()
  await expect(page.getByRole('cell', { name: '€1,529.00' })).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'Mark as delivered' }),
  ).toHaveCount(0)
})

test('installer has no access to supplier orders', async ({ page }) => {
  await signIn(page, 'installer@sedzkitchens.de')
  await expect(page.getByRole('link', { name: 'Supplier orders' })).toHaveCount(
    0,
  )

  await page.goto('/supplier-orders')
  await expect(page).toHaveURL('/')
})
