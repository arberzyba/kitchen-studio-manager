import type { Quote } from '../quotes/types'

// In the order the steps happen
export const ORDER_STATUSES = [
  'NEW',
  'MEASURED',
  'ORDERED_FROM_SUPPLIER',
  'DELIVERED',
  'INSTALLED',
  'COMPLETED',
] as const

export type OrderStatus = (typeof ORDER_STATUSES)[number]

// One row of the order list
export type OrderSummary = {
  id: number
  orderNumber: string
  customerName: string
  status: OrderStatus
  orderDate: string
  grossTotal: number
}

// The quote the order was created from supplies its customer, items and totals
export type Order = {
  id: number
  orderNumber: string
  status: OrderStatus
  orderDate: string
  createdByName: string
  quote: Quote
}
