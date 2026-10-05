export const ROLES = ['ADMIN', 'SALES', 'OFFICE', 'INSTALLER'] as const

export type Role = (typeof ROLES)[number]

export type User = {
  id: number
  email: string
  firstName: string
  lastName: string
  role: Role
  active: boolean
}
