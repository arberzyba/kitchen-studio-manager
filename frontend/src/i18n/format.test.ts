import i18n from 'i18next'
import { beforeAll, describe, expect, it } from 'vitest'
import { formatCurrency, formatDate, formatMonth, formatNumber } from './format'

// Intl separates amount and currency with a non-breaking space
const normalize = (text: string) => text.replace(/\s/g, ' ')

describe('German formats', () => {
  beforeAll(() => i18n.init({ lng: 'de', resources: {} }))

  it('writes amounts as 1.299,00 €', () => {
    expect(normalize(formatCurrency(1299))).toBe('1.299,00 €')
    expect(normalize(formatCurrency(0.5))).toBe('0,50 €')
  })

  it('writes dates as 05.10.2026', () => {
    expect(formatDate('2026-10-05')).toBe('05.10.2026')
  })

  it('uses a decimal comma', () => {
    expect(formatNumber(4.2)).toBe('4,2')
  })

  it('abbreviates months', () => {
    expect(formatMonth('2026-10')).toBe('Okt. 26')
  })
})

describe('English formats', () => {
  beforeAll(() => i18n.changeLanguage('en'))

  it('writes amounts as €1,299.00', () => {
    expect(formatCurrency(1299)).toBe('€1,299.00')
  })

  it('writes dates as 5 Oct 2026', () => {
    expect(formatDate('2026-10-05')).toBe('5 Oct 2026')
  })

  it('uses a decimal point', () => {
    expect(formatNumber(4.2)).toBe('4.2')
  })
})
