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

// German amounts read 1.299,00 €; English ones €1,299.00
export function formatCurrency(value: number) {
  return new Intl.NumberFormat(locale(), {
    style: 'currency',
    currency: 'EUR',
  }).format(value)
}
