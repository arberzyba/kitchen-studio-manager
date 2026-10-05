import type { Product } from '../products/types'
import type { User } from '../users/types'

export const QUOTE_STATUSES = ['DRAFT', 'SENT', 'ACCEPTED', 'REJECTED'] as const

export type QuoteStatus = (typeof QUOTE_STATUSES)[number]

// One row of the quote list
export type QuoteSummary = {
  id: number
  quoteNumber: string
  customerName: string
  status: QuoteStatus
  quoteDate: string
  validUntil: string
  grossTotal: number
}

export type QuoteItem = {
  productId: number
  sku: string
  description: string
  unit: Product['unit']
  quantity: number
  unitPrice: number
  discountPercent: number
  lineTotal: number
}

export type Quote = {
  id: number
  quoteNumber: string
  status: QuoteStatus
  quoteDate: string
  validUntil: string
  customerId: number
  customerName: string
  customerEmail: string | null
  discountPercent: number
  vatRate: number
  notes: string | null
  items: QuoteItem[]
  subtotal: number
  discountAmount: number
  netTotal: number
  vatAmount: number
  grossTotal: number
  createdByName: string
}

// Mirrors the backend rule: office staff can read quotes but not change them
export function canEditQuotes(user: User | undefined) {
  return user?.role === 'ADMIN' || user?.role === 'SALES'
}
