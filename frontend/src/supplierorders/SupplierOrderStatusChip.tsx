import Chip from '@mui/material/Chip'
import { useTranslation } from 'react-i18next'
import type { SupplierOrder } from './types'

export function SupplierOrderStatusChip({ order }: { order: SupplierOrder }) {
  const { t } = useTranslation()
  if (order.overdue) {
    return (
      <Chip size="small" color="error" label={t('supplierOrders.overdue')} />
    )
  }
  return (
    <Chip
      size="small"
      color={order.status === 'DELIVERED' ? 'success' : 'info'}
      label={t(`supplierOrderStatuses.${order.status}`)}
    />
  )
}
