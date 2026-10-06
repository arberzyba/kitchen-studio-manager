import AssignmentIcon from '@mui/icons-material/Assignment'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { api, type Page } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { canEditQuotes } from '../quotes/types'
import type { Order, OrderSummary } from './types'

// Shown on an accepted quote: links to its order, or offers to create one
export function QuoteOrderAction({ quoteId }: { quoteId: number }) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const orders = useQuery({
    queryKey: ['orders', { quoteId }],
    queryFn: () => api<Page<OrderSummary>>(`/orders?quoteId=${quoteId}`),
  })
  const create = useMutation({
    mutationFn: () =>
      api<Order>('/orders', { method: 'POST', body: { quoteId } }),
    onSuccess: (order) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      navigate(`/orders/${order.id}`)
    },
  })

  if (!orders.data) {
    return null
  }
  const order = orders.data.content[0]

  return (
    <Stack spacing={2} sx={{ alignItems: 'flex-end' }}>
      {create.isError && <Alert severity="error">{t('common.error')}</Alert>}
      {order ? (
        <Button
          variant="outlined"
          startIcon={<AssignmentIcon />}
          component={Link}
          to={`/orders/${order.id}`}
        >
          {t('orders.view', { number: order.orderNumber })}
        </Button>
      ) : (
        // Mirrors the backend rule: only sales and admins create orders
        canEditQuotes(user) && (
          <Button
            variant="contained"
            startIcon={<AssignmentIcon />}
            loading={create.isPending}
            onClick={() => create.mutate()}
          >
            {t('orders.create')}
          </Button>
        )
      )}
    </Stack>
  )
}
