import type { Address } from '../customers/types'
import type { Role } from '../users/types'

export const APPOINTMENT_TYPES = [
  'MEASUREMENT',
  'DELIVERY',
  'INSTALLATION',
] as const

export type AppointmentType = (typeof APPOINTMENT_TYPES)[number]

export const APPOINTMENT_COLORS: Record<AppointmentType, string> = {
  MEASUREMENT: '#0277bd',
  DELIVERY: '#e65100',
  INSTALLATION: '#2e7d32',
}

export type Appointment = {
  id: number
  type: AppointmentType
  startTime: string
  endTime: string
  notes: string | null
  assigneeId: number
  assigneeName: string
  orderId: number
  orderNumber: string
  customerName: string
  customerPhone: string | null
  address: Address
}

// An active employee an appointment can be assigned to
export type AssignableUser = {
  id: number
  firstName: string
  lastName: string
  role: Role
}
