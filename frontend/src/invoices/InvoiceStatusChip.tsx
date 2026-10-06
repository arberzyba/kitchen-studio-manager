import Chip from '@mui/material/Chip'
import { useTranslation } from 'react-i18next'
import type { Invoice } from './types'

const STATUS_COLORS = {
  OPEN: 'default',
  PARTIALLY_PAID: 'info',
  PAID: 'success',
} as const

export function InvoiceStatusChip({ invoice }: { invoice: Invoice }) {
  const { t } = useTranslation()
  if (invoice.overdue) {
    return <Chip size="small" color="error" label={t('invoices.overdue')} />
  }
  return (
    <Chip
      size="small"
      color={STATUS_COLORS[invoice.status]}
      label={t(`invoiceStatuses.${invoice.status}`)}
    />
  )
}
