import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible()
}

test('dashboard shows open quotes, revenue, installations and overdue payments', async ({
  page,
}) => {
  await signIn(page, 'office@sedzkitchens.de')

  // One quote has been sent and not answered; one more is still a draft
  const openQuotes = page.getByRole('link', {
    name: /Quotes awaiting an answer/,
  })
  await expect(openQuotes).toContainText('€3,282.26 in total')
  await expect(openQuotes).toContainText('1 more in draft')

  // One invoice from the demo history is unpaid past its due date
  const overdue = page.getByRole('link', { name: /Overdue payments/ })
  await expect(overdue).toContainText('€2,656.79')
  await expect(overdue).toContainText('1 overdue invoice')
  await expect(page.getByText(/· Praxis Dr\. Wagner/)).toBeVisible()

  // Six months of revenue
  await expect(
    page.getByRole('heading', { name: 'Revenue per month (net)' }),
  ).toBeVisible()
  await expect(page.locator('.recharts-bar-rectangle')).toHaveCount(6)

  await expect(page.getByText(/· Sabine Müller, Köln/)).toBeVisible()
})

test('overdue invoice on the dashboard opens the invoice', async ({ page }) => {
  await signIn(page, 'office@sedzkitchens.de')
  await page
    .getByRole('main')
    .getByRole('link', { name: /RE-\d{4}-\d{4}/ })
    .click()

  await expect(page.getByRole('heading', { name: /Invoice RE-/ })).toBeVisible()
  await expect(page.getByText('Overdue', { exact: true })).toBeVisible()
  await expect(page.getByText('€2,656.79').first()).toBeVisible()
})

test('dashboard is shown in German with German formats', async ({ page }) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('button', { name: 'Deutsch' }).click()

  await expect(
    page.getByRole('link', { name: /Überfällige Zahlungen/ }),
  ).toContainText('2.656,79 €')
  await expect(
    page.getByRole('heading', { name: 'Anstehende Montagen' }),
  ).toBeVisible()
})

test('installer sees no business figures', async ({ page }) => {
  await signIn(page, 'installer@sedzkitchens.de')

  await expect(page.getByText('Overdue payments')).toHaveCount(0)
  await expect(page.getByText('Revenue per month (net)')).toHaveCount(0)
})
