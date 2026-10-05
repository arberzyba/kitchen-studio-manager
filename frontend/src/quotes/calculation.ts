// Live preview of the quote totals while editing. It follows the same steps as the backend's
// QuoteCalculator; the backend result is the authoritative one and is what gets saved and printed.

export const VAT_RATE = 19

function roundToCents(value: number) {
  return Math.round((value + Number.EPSILON) * 100) / 100
}

export function lineTotal(
  quantity: number,
  unitPrice: number,
  discountPercent: number,
) {
  return roundToCents(quantity * unitPrice * (1 - discountPercent / 100))
}

export function totals(lineTotals: number[], discountPercent: number) {
  const subtotal = roundToCents(lineTotals.reduce((sum, line) => sum + line, 0))
  const discountAmount = roundToCents((subtotal * discountPercent) / 100)
  const netTotal = roundToCents(subtotal - discountAmount)
  const vatAmount = roundToCents((netTotal * VAT_RATE) / 100)
  return {
    subtotal,
    discountAmount,
    netTotal,
    vatAmount,
    grossTotal: roundToCents(netTotal + vatAmount),
  }
}
