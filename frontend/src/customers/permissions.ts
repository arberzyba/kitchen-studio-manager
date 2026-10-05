import type { User } from '../users/types'

// Mirrors the backend rule: office staff can read customers but not change them
export function canEditCustomers(user: User | undefined) {
  return user?.role === 'ADMIN' || user?.role === 'SALES'
}
