import Chip from '@mui/material/Chip'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useTranslation } from 'react-i18next'
import { formatCurrency, formatNumber } from '../i18n/format'
import type { QuoteStatus } from './types'

const STATUS_COLORS = {
  DRAFT: 'default',
  SENT: 'info',
  ACCEPTED: 'success',
  REJECTED: 'error',
} as const

export function QuoteStatusChip({ status }: { status: QuoteStatus }) {
  const { t } = useTranslation()
  return (
    <Chip
      size="small"
      label={t(`quoteStatuses.${status}`)}
      color={STATUS_COLORS[status]}
    />
  )
}

// Net / VAT / gross breakdown, shared by the editor (live preview) and the detail page
export function QuoteTotals({
  totals,
  discountPercent,
  vatRate,
}: {
  totals: {
    subtotal: number
    discountAmount: number
    netTotal: number
    vatAmount: number
    grossTotal: number
  }
  discountPercent: number
  vatRate: number
}) {
  const { t } = useTranslation()
  const rows = [
    ...(totals.discountAmount > 0
      ? [
          [t('quotes.subtotal'), formatCurrency(totals.subtotal)],
          [
            t('quotes.discountOf', { percent: formatNumber(discountPercent) }),
            `− ${formatCurrency(totals.discountAmount)}`,
          ],
        ]
      : []),
    [t('quotes.netTotal'), formatCurrency(totals.netTotal)],
    [
      t('quotes.vatOf', { percent: formatNumber(vatRate) }),
      formatCurrency(totals.vatAmount),
    ],
  ]

  return (
    <Stack spacing={0.5} sx={{ width: 320, ml: 'auto' }}>
      {rows.map(([label, value]) => (
        <Stack
          key={label}
          direction="row"
          sx={{ justifyContent: 'space-between' }}
        >
          <Typography>{label}</Typography>
          <Typography>{value}</Typography>
        </Stack>
      ))}
      <Stack
        direction="row"
        sx={{
          justifyContent: 'space-between',
          borderTop: 1,
          borderColor: 'divider',
          pt: 0.5,
        }}
      >
        <Typography sx={{ fontWeight: 'bold' }}>
          {t('quotes.grossTotal')}
        </Typography>
        <Typography sx={{ fontWeight: 'bold' }}>
          {formatCurrency(totals.grossTotal)}
        </Typography>
      </Stack>
    </Stack>
  )
}
