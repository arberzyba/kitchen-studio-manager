import i18n from 'i18next'

// German dates read 05.10.2026, 14:30; English ones 5 Oct 2026, 14:30
export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(i18n.language === 'de' ? 'de-DE' : 'en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
