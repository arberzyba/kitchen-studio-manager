export const PRODUCT_CATEGORIES = ['CABINET', 'WORKTOP', 'APPLIANCE'] as const

export const PRODUCT_UNITS = ['PIECE', 'METER'] as const

export type ProductCategory = (typeof PRODUCT_CATEGORIES)[number]

export type Product = {
  id: number
  sku: string
  name: string
  description: string | null
  category: ProductCategory
  unit: (typeof PRODUCT_UNITS)[number]
  purchasePrice: number
  sellingPrice: number
  supplierId: number
  supplierName: string
  active: boolean
}
