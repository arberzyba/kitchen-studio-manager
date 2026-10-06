import i18n from 'i18next'

function locale() {
  return i18n.language === 'de' ? 'de-DE' : 'en-GB'
}

// German dates read 05.10.2026, 14:30; English ones 5 Oct 2026, 14:30
export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(locale(), {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

// For plain dates such as 2026-10-05: 05.10.2026 in German, 5 Oct 2026 in English
export function formatDate(value: string) {
  return new Intl.DateTimeFormat(locale(), { dateStyle: 'medium' }).format(
    new Date(value),
  )
}

// For a calendar month such as 2026-10: Okt. 26 in German, Oct 26 in English
export function formatMonth(value: string) {
  return new Intl.DateTimeFormat(locale(), {
    month: 'short',
    year: '2-digit',
  }).format(new Date(`${value}-01`))
}

// German amounts read 1.299,00 €; English ones €1,299.00
export function formatCurrency(value: number) {
  return new Intl.NumberFormat(locale(), {
    style: 'currency',
    currency: 'EUR',
  }).format(value)
}

// 4,2 in German, 4.2 in English
export function formatNumber(value: number) {
  return new Intl.NumberFormat(locale()).format(value)
}
