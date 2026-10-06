import type { Quote } from '../quotes/types'
import type { User } from '../users/types'

export const INVOICE_STATUSES = ['OPEN', 'PARTIALLY_PAID', 'PAID'] as const

export type Payment = {
  id: number
  amount: number
  paidOn: string
  note: string | null
}

export type Invoice = {
  id: number
  invoiceNumber: string
  status: (typeof INVOICE_STATUSES)[number]
  // Not fully paid although the due date has passed
  overdue: boolean
  invoiceDate: string
  serviceDate: string
  dueDate: string
  orderId: number
  orderNumber: string
  recipientName: string
  recipientCompany: string | null
  grossTotal: number
  paidTotal: number
  openAmount: number
  // Null in list responses. The quote holds the invoiced items and totals.
  details: {
    customerId: number
    customerEmail: string | null
    payments: Payment[]
    quote: Quote
  } | null
}

// Mirrors the backend rule: sales staff can read invoices but not issue them or record payments
export function canManageInvoices(user: User | undefined) {
  return user?.role === 'ADMIN' || user?.role === 'OFFICE'
}

// A date the given number of days from today as 2026-10-06, the format date inputs and the backend use
export function isoDate(daysFromToday = 0) {
  const date = new Date()
  date.setDate(date.getDate() + daysFromToday)
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}
