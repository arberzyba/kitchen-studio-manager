import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatCurrency, formatDate, formatNumber } from '../i18n/format'
import { SupplierOrderStatusChip } from './SupplierOrderStatusChip'
import { canManageSupplierOrders, today, type SupplierOrder } from './types'

export function SupplierOrderDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const { user } = useAuth()
  const order = useQuery({
    queryKey: ['supplierOrder', id],
    queryFn: () => api<SupplierOrder>(`/supplier-orders/${id}`),
  })

  if (order.isError) {
    return <Alert severity="error">{t('common.error')}</Alert>
  }
  if (!order.data) {
    return null
  }
  const { data } = order

  return (
    <Stack spacing={3}>
      <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
        <Typography variant="h4" component="h1">
          {t('supplierOrders.heading', { number: data.orderNumber })}
        </Typography>
        <SupplierOrderStatusChip order={data} />
      </Stack>

      <Paper sx={{ p: 3 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
          <Detail label={t('supplierOrders.supplier')}>
            {data.supplierName}
          </Detail>
          <Detail label={t('supplierOrders.forOrder')}>
            <Link to={`/orders/${data.salesOrderId}`}>
              {data.salesOrderNumber}
            </Link>
            {' · '}
            {data.customerName}
          </Detail>
          <Detail label={t('supplierOrders.ordered')}>
            {formatDate(data.orderedDate)}
          </Detail>
          <Detail label={t('supplierOrders.expected')}>
            {formatDate(data.expectedDeliveryDate)}
          </Detail>
          {data.actualDeliveryDate && (
            <Detail label={t('supplierOrders.delivered')}>
              {formatDate(data.actualDeliveryDate)}
            </Detail>
          )}
        </Stack>
        {data.notes && (
          <Typography sx={{ mt: 2, whiteSpace: 'pre-wrap' }}>
            {data.notes}
          </Typography>
        )}
      </Paper>

      {/* Remounts with fresh values whenever the saved order changes */}
      {canManageSupplierOrders(user) && data.status === 'ORDERED' && (
        <Tracking
          key={`${data.expectedDeliveryDate}|${data.notes}`}
          order={data}
        />
      )}

      <Paper sx={{ p: 3 }}>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('quotes.articleNumber')}</TableCell>
                <TableCell>{t('quotes.product')}</TableCell>
                <TableCell align="right">{t('quotes.quantity')}</TableCell>
                <TableCell align="right">
                  {t('products.purchasePrice')}
                </TableCell>
                <TableCell align="right">{t('quotes.lineTotal')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {data.items.map((item) => (
                <TableRow key={item.sku}>
                  <TableCell>{item.sku}</TableCell>
                  <TableCell>{item.description}</TableCell>
                  <TableCell align="right">
                    {formatNumber(item.quantity)}{' '}
                    {t(`productUnitsShort.${item.unit}`)}
                  </TableCell>
                  <TableCell align="right">
                    {formatCurrency(item.purchasePrice)}
                  </TableCell>
                  <TableCell align="right">
                    {formatCurrency(item.lineTotal)}
                  </TableCell>
                </TableRow>
              ))}
              <TableRow>
                <TableCell
                  colSpan={4}
                  align="right"
                  sx={{ fontWeight: 'bold' }}
                >
                  {t('supplierOrders.total')}
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                  {formatCurrency(data.total)}
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Stack>
  )
}

// Lets office staff move the expected date or record that the goods have arrived
function Tracking({ order }: { order: SupplierOrder }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [expected, setExpected] = useState(order.expectedDeliveryDate)
  const [notes, setNotes] = useState(order.notes ?? '')
  const [deliveredOn, setDeliveredOn] = useState(today())

  function onSaved(updated: SupplierOrder) {
    queryClient.setQueryData(['supplierOrder', String(order.id)], updated)
    queryClient.invalidateQueries({ queryKey: ['supplierOrders'] })
  }

  const update = useMutation({
    mutationFn: () =>
      api<SupplierOrder>(`/supplier-orders/${order.id}`, {
        method: 'PUT',
        body: { expectedDeliveryDate: expected, notes: notes || null },
      }),
    onSuccess: onSaved,
  })
  const deliver = useMutation({
    mutationFn: () =>
      api<SupplierOrder>(`/supplier-orders/${order.id}/delivered`, {
        method: 'POST',
        body: { actualDeliveryDate: deliveredOn },
      }),
    onSuccess: onSaved,
  })

  return (
    <Paper sx={{ p: 3 }}>
      {(update.isError || deliver.isError) && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
        <Stack spacing={2} sx={{ flex: 1 }}>
          <Typography variant="h6" component="h2">
            {t('supplierOrders.changeExpected')}
          </Typography>
          <TextField
            type="date"
            label={t('supplierOrders.expected')}
            slotProps={{ inputLabel: { shrink: true } }}
            value={expected}
            onChange={(event) => setExpected(event.target.value)}
          />
          <TextField
            multiline
            label={t('supplierOrders.notes')}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            slotProps={{ htmlInput: { maxLength: 1000 } }}
          />
          <Button
            variant="outlined"
            sx={{ alignSelf: 'flex-start' }}
            disabled={!expected}
            loading={update.isPending}
            onClick={() => update.mutate()}
          >
            {t('common.save')}
          </Button>
        </Stack>
        <Stack spacing={2} sx={{ flex: 1 }}>
          <Typography variant="h6" component="h2">
            {t('supplierOrders.recordDelivery')}
          </Typography>
          <TextField
            type="date"
            label={t('supplierOrders.deliveredOn')}
            slotProps={{
              inputLabel: { shrink: true },
              // Goods cannot arrive before they were ordered or in the future
              htmlInput: { min: order.orderedDate, max: today() },
            }}
            value={deliveredOn}
            onChange={(event) => setDeliveredOn(event.target.value)}
          />
          <Button
            variant="contained"
            color="success"
            sx={{ alignSelf: 'flex-start' }}
            disabled={
              !deliveredOn ||
              deliveredOn < order.orderedDate ||
              deliveredOn > today()
            }
            loading={deliver.isPending}
            onClick={() => deliver.mutate()}
          >
            {t('supplierOrders.markDelivered')}
          </Button>
        </Stack>
      </Stack>
    </Paper>
  )
}

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography component="div">{children}</Typography>
    </div>
  )
}
