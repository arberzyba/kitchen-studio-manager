import LocalShippingIcon from '@mui/icons-material/LocalShipping'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { api, type Page } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatDate } from '../i18n/format'
import { SupplierOrderStatusChip } from './SupplierOrderStatusChip'
import {
  canManageSupplierOrders,
  type PendingSupplier,
  type SupplierOrder,
} from './types'

// Shown on a customer order: what has been ordered from suppliers and what is still open
export function OrderSupplierOrders({ orderId }: { orderId: number }) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [ordering, setOrdering] = useState<PendingSupplier>()
  const placed = useQuery({
    queryKey: ['supplierOrders', { orderId }],
    queryFn: () =>
      api<Page<SupplierOrder>>(`/supplier-orders?orderId=${orderId}&sort=id`),
  })
  const pending = useQuery({
    queryKey: ['supplierOrders', 'pending', orderId],
    queryFn: () =>
      api<PendingSupplier[]>(`/supplier-orders/pending?orderId=${orderId}`),
  })

  if (!placed.data || !pending.data) {
    return null
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" component="h2" gutterBottom>
        {t('supplierOrders.title')}
      </Typography>
      <Stack spacing={1.5}>
        {placed.data.content.map((order) => (
          <Stack
            key={order.id}
            direction="row"
            spacing={2}
            sx={{ alignItems: 'center' }}
          >
            <Link to={`/supplier-orders/${order.id}`}>{order.orderNumber}</Link>
            <Typography sx={{ flexGrow: 1 }}>{order.supplierName}</Typography>
            <Typography variant="body2" color="text.secondary">
              {order.actualDeliveryDate
                ? t('supplierOrders.deliveredAt', {
                    date: formatDate(order.actualDeliveryDate),
                  })
                : t('supplierOrders.expectedAt', {
                    date: formatDate(order.expectedDeliveryDate),
                  })}
            </Typography>
            <SupplierOrderStatusChip order={order} />
          </Stack>
        ))}
        {pending.data.map((supplier) => (
          <Stack
            key={supplier.supplierId}
            direction="row"
            spacing={2}
            sx={{ alignItems: 'center' }}
          >
            <Typography sx={{ flexGrow: 1 }}>
              {supplier.supplierName}
              <Typography
                component="span"
                variant="body2"
                color="text.secondary"
              >
                {' · '}
                {t('supplierOrders.itemsToOrder', {
                  count: supplier.itemCount,
                })}
              </Typography>
            </Typography>
            {canManageSupplierOrders(user) && (
              <Button
                variant="outlined"
                size="small"
                startIcon={<LocalShippingIcon />}
                onClick={() => setOrdering(supplier)}
              >
                {t('supplierOrders.orderNow')}
              </Button>
            )}
          </Stack>
        ))}
      </Stack>
      {ordering && (
        <OrderDialog
          orderId={orderId}
          supplier={ordering}
          onClose={() => setOrdering(undefined)}
        />
      )}
    </Paper>
  )
}

function OrderDialog({
  orderId,
  supplier,
  onClose,
}: {
  orderId: number
  supplier: PendingSupplier
  onClose: () => void
}) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [expected, setExpected] = useState('')
  const create = useMutation({
    mutationFn: () =>
      api<SupplierOrder>('/supplier-orders', {
        method: 'POST',
        body: {
          orderId,
          supplierId: supplier.supplierId,
          expectedDeliveryDate: expected,
        },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['supplierOrders'] })
      onClose()
    },
  })

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>
        {t('supplierOrders.orderFrom', { name: supplier.supplierName })}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {create.isError && (
            <Alert severity="error">{t('common.error')}</Alert>
          )}
          <Typography>
            {t('supplierOrders.orderExplanation', {
              count: supplier.itemCount,
            })}
          </Typography>
          <TextField
            type="date"
            label={t('supplierOrders.expected')}
            slotProps={{ inputLabel: { shrink: true } }}
            value={expected}
            onChange={(event) => setExpected(event.target.value)}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.cancel')}</Button>
        <Button
          variant="contained"
          disabled={!expected}
          loading={create.isPending}
          onClick={() => create.mutate()}
        >
          {t('supplierOrders.placeOrder')}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
