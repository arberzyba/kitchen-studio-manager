import { describe, expect, it } from 'vitest'
import { lineTotal, totals } from './calculation'

// The same cases as the backend's QuoteCalculatorTests, so the live preview and the saved quote agree
describe('lineTotal', () => {
  it('multiplies quantity and unit price', () => {
    expect(lineTotal(4, 189, 0)).toBe(756)
  })

  it('supports fractional quantities for worktops', () => {
    expect(lineTotal(4.2, 369, 0)).toBe(1549.8)
  })

  it('applies the line discount and rounds to cents', () => {
    expect(lineTotal(1, 649, 10)).toBe(584.1)
    // 3 x 33.33 = 99.99, less 12.5 % = 87.49125
    expect(lineTotal(3, 33.33, 12.5)).toBe(87.49)
  })
})

describe('totals', () => {
  it('adds 19 % VAT when there is no discount', () => {
    expect(totals([756, 599], 0)).toEqual({
      subtotal: 1355,
      discountAmount: 0,
      netTotal: 1355,
      vatAmount: 257.45,
      grossTotal: 1612.45,
    })
  })

  it('takes the overall discount before VAT', () => {
    expect(totals([1000], 5)).toEqual({
      subtotal: 1000,
      discountAmount: 50,
      netTotal: 950,
      vatAmount: 180.5,
      grossTotal: 1130.5,
    })
  })

  it('matches the backend for the demo kitchen', () => {
    // 4 x 189.00 + 4.2 m x 369.00 less 10 %, then 5 % overall discount
    const lines = [lineTotal(4, 189, 0), lineTotal(4.2, 369, 10)]
    expect(totals(lines, 5)).toEqual({
      subtotal: 2150.82,
      discountAmount: 107.54,
      netTotal: 2043.28,
      vatAmount: 388.22,
      grossTotal: 2431.5,
    })
  })

  it('is all zeroes for a quote without items', () => {
    expect(totals([], 10).grossTotal).toBe(0)
  })
})
