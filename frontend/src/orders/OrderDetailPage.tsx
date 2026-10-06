import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Step from '@mui/material/Step'
import StepLabel from '@mui/material/StepLabel'
import Stepper from '@mui/material/Stepper'
import Typography from '@mui/material/Typography'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { api } from '../api/client'
import { formatDate } from '../i18n/format'
import { QuoteItemsCard } from '../quotes/QuoteParts'
import { OrderSupplierOrders } from '../supplierorders/OrderSupplierOrders'
import { ORDER_STATUSES, type Order } from './types'

export function OrderDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const queryClient = useQueryClient()
  const order = useQuery({
    queryKey: ['order', id],
    queryFn: () => api<Order>(`/orders/${id}`),
  })
  const advance = useMutation({
    mutationFn: () => api<Order>(`/orders/${id}/advance`, { method: 'POST' }),
    onSuccess: (updated) => {
      queryClient.setQueryData(['order', id], updated)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })

  if (order.isError) {
    return <Alert severity="error">{t('common.error')}</Alert>
  }
  if (!order.data) {
    return null
  }
  const { quote } = order.data
  const step = ORDER_STATUSES.indexOf(order.data.status)
  // Undefined once the order is completed
  const nextStatus = ORDER_STATUSES[step + 1]

  return (
    <Stack spacing={3}>
      <Typography variant="h4" component="h1">
        {t('orders.heading', { number: order.data.orderNumber })}
      </Typography>

      <Paper sx={{ p: 3 }}>
        {/* A completed order shows every step as done */}
        <Stepper activeStep={nextStatus ? step : step + 1} alternativeLabel>
          {ORDER_STATUSES.map((status) => (
            <Step key={status}>
              <StepLabel>{t(`orderStatuses.${status}`)}</StepLabel>
            </Step>
          ))}
        </Stepper>
        {advance.isError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {t('common.error')}
          </Alert>
        )}
        {nextStatus && (
          <Stack sx={{ mt: 3, alignItems: 'flex-end' }}>
            <Button
              variant="contained"
              loading={advance.isPending}
              onClick={() => advance.mutate()}
            >
              {t('orders.advanceTo', {
                status: t(`orderStatuses.${nextStatus}`),
              })}
            </Button>
          </Stack>
        )}
      </Paper>

      <Paper sx={{ p: 3 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
          <Detail label={t('orders.customer')}>
            <Link to={`/customers/${quote.customerId}`}>
              {quote.customerName}
            </Link>
          </Detail>
          <Detail label={t('orders.date')}>
            {formatDate(order.data.orderDate)}
          </Detail>
          <Detail label={t('orders.quote')}>
            <Link to={`/quotes/${quote.id}`}>{quote.quoteNumber}</Link>
          </Detail>
          <Detail label={t('orders.createdBy')}>
            {order.data.createdByName}
          </Detail>
        </Stack>
      </Paper>

      <OrderSupplierOrders orderId={order.data.id} />

      <QuoteItemsCard quote={quote} />
    </Stack>
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
