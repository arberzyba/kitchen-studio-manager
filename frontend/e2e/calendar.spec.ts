import { expect, test, type Page } from '@playwright/test'

async function signIn(page: Page, email: string) {
  await page.goto('/')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('demo1234')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: /Welcome/ })).toBeVisible()
}

async function signOut(page: Page) {
  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page).toHaveURL('/login')
}

// Today at the given hour, in the format a datetime-local input expects
function todayAt(hour: number) {
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(hour)}:00`
}

// Plans an appointment, checks what the installer sees and removes it again, so the test leaves no data behind
test('appointment is planned by sales, visible to its installer and can be cancelled', async ({
  page,
}) => {
  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Calendar' }).click()
  await page.getByRole('button', { name: 'New appointment' }).click()
  const dialog = page.getByRole('dialog')

  await dialog.getByRole('button', { name: 'Save' }).click()
  await expect(dialog.getByText('Choose an order')).toBeVisible()
  await expect(dialog.getByText('Choose an employee')).toBeVisible()

  await dialog.getByLabel('Order').fill('Müller')
  await page.getByRole('option', { name: /AU-\d{4}-0001/ }).click()
  await dialog.getByLabel('Type').click()
  await page.getByRole('option', { name: 'Installation' }).click()
  await dialog.getByLabel('Start').fill(todayAt(13))
  await dialog.getByLabel('End').fill(todayAt(12))
  await dialog.getByLabel('Assigned to').click()
  await page.getByRole('option', { name: /Jonas Becker/ }).click()
  await dialog.getByLabel('Notes').fill('Created by an automated test')
  await dialog.getByRole('button', { name: 'Save' }).click()
  await expect(
    dialog.getByText('The end must be after the start'),
  ).toBeVisible()
  await dialog.getByLabel('End').fill(todayAt(14))
  await dialog.getByRole('button', { name: 'Save' }).click()

  const event = page
    .locator('.fc-event')
    .filter({ hasText: '13:00 - 14:00' })
    .filter({ hasText: 'Installation · Sabine Müller' })
  await expect(event).toBeVisible()
  await signOut(page)

  // The installer sees the job with the customer's address, but cannot change it
  await signIn(page, 'installer@sedzkitchens.de')
  await page.getByRole('link', { name: 'Calendar' }).click()
  await expect(page.getByRole('heading', { name: 'My jobs' })).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'New appointment' }),
  ).toHaveCount(0)
  await event.click()
  await expect(
    dialog.getByText(/Aachener Straße 112, 50674 Köln/),
  ).toBeVisible()
  await expect(dialog.getByText('Created by an automated test')).toBeVisible()
  await expect(dialog.getByRole('button', { name: 'Save' })).toHaveCount(0)
  await dialog.getByRole('button', { name: 'Close' }).click()
  await signOut(page)

  await signIn(page, 'sales@sedzkitchens.de')
  await page.getByRole('link', { name: 'Calendar' }).click()
  await event.click()
  await dialog.getByRole('button', { name: 'Delete' }).click()
  await dialog.getByRole('button', { name: 'Really delete' }).click()
  await expect(event).toHaveCount(0)
})
