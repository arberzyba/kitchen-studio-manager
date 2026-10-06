import type { Product } from '../products/types'
import type { User } from '../users/types'

export const SUPPLIER_ORDER_STATUSES = ['ORDERED', 'DELIVERED'] as const

export type SupplierOrder = {
  id: number
  orderNumber: string
  status: (typeof SUPPLIER_ORDER_STATUSES)[number]
  // Not delivered although the expected date has passed
  overdue: boolean
  supplierId: number
  supplierName: string
  salesOrderId: number
  salesOrderNumber: string
  customerName: string
  orderedDate: string
  expectedDeliveryDate: string
  actualDeliveryDate: string | null
  total: number
  notes: string | null
  // Empty in list responses
  items: {
    sku: string
    description: string
    unit: Product['unit']
    quantity: number
    purchasePrice: number
    lineTotal: number
  }[]
}

// A supplier a customer order has items from, but which has not been ordered from yet
export type PendingSupplier = {
  supplierId: number
  supplierName: string
  itemCount: number
}

// Mirrors the backend rule: sales staff can read supplier orders but not change them
export function canManageSupplierOrders(user: User | undefined) {
  return user?.role === 'ADMIN' || user?.role === 'OFFICE'
}

// Today as 2026-10-06, the format date inputs and the backend use
export function today() {
  const now = new Date()
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}
