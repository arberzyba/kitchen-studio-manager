import Chip from '@mui/material/Chip'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { useTranslation } from 'react-i18next'
import { formatCurrency, formatNumber } from '../i18n/format'
import type { Quote, QuoteStatus } from './types'

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

// Net / VAT / gross breakdown, shared by the editor (live preview) and the read-only views
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

// Read-only items, totals and notes of a quote; also shown on the order created from it
export function QuoteItemsCard({ quote }: { quote: Quote }) {
  const { t } = useTranslation()
  return (
    <Paper sx={{ p: 3 }}>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t('quotes.articleNumber')}</TableCell>
              <TableCell>{t('quotes.product')}</TableCell>
              <TableCell align="right">{t('quotes.quantity')}</TableCell>
              <TableCell align="right">{t('quotes.unitPrice')}</TableCell>
              <TableCell align="right">{t('quotes.discount')}</TableCell>
              <TableCell align="right">{t('quotes.lineTotal')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {quote.items.map((item, index) => (
              <TableRow key={index}>
                <TableCell>{item.sku}</TableCell>
                <TableCell>{item.description}</TableCell>
                <TableCell align="right">
                  {formatNumber(item.quantity)}{' '}
                  {t(`productUnitsShort.${item.unit}`)}
                </TableCell>
                <TableCell align="right">
                  {formatCurrency(item.unitPrice)}
                </TableCell>
                <TableCell align="right">
                  {item.discountPercent > 0 &&
                    `${formatNumber(item.discountPercent)} %`}
                </TableCell>
                <TableCell align="right">
                  {formatCurrency(item.lineTotal)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <Stack sx={{ mt: 2 }}>
        <QuoteTotals
          totals={quote}
          discountPercent={quote.discountPercent}
          vatRate={quote.vatRate}
        />
      </Stack>
      {quote.notes && (
        <Typography sx={{ mt: 2, whiteSpace: 'pre-wrap' }}>
          {quote.notes}
        </Typography>
      )}
    </Paper>
  )
}
