export const SALUTATIONS = ['MR', 'MS', 'NONE'] as const

export const CONTACT_TYPES = ['CALL', 'EMAIL', 'MEETING', 'NOTE'] as const

export type Address = {
  street: string
  postalCode: string
  city: string
}

export type Customer = {
  id: number
  salutation: (typeof SALUTATIONS)[number]
  firstName: string
  lastName: string
  companyName: string | null
  email: string | null
  phone: string | null
  billingAddress: Address
  installationAddress: Address | null
  // True once the personal data was erased on request; only placeholders remain
  anonymized: boolean
}

export type Contact = {
  id: number
  contactType: (typeof CONTACT_TYPES)[number]
  summary: string
  createdByName: string
  createdAt: string
}
