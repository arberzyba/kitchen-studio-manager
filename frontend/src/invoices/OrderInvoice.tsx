import ReceiptLongIcon from '@mui/icons-material/ReceiptLong'
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
import { Link, useNavigate } from 'react-router'
import { api, type Page } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatCurrency } from '../i18n/format'
import { InvoiceStatusChip } from './InvoiceStatusChip'
import { canManageInvoices, isoDate, type Invoice } from './types'

// Shown on a customer order: its invoice, or the option to issue one
export function OrderInvoice({ orderId }: { orderId: number }) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [creating, setCreating] = useState(false)
  const invoices = useQuery({
    queryKey: ['invoices', { orderId }],
    queryFn: () => api<Page<Invoice>>(`/invoices?orderId=${orderId}`),
  })

  if (!invoices.data) {
    return null
  }
  const invoice = invoices.data.content[0]

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" component="h2" gutterBottom>
        {t('invoices.single')}
      </Typography>
      {invoice ? (
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          <Link to={`/invoices/${invoice.id}`}>{invoice.invoiceNumber}</Link>
          <Typography sx={{ flexGrow: 1 }}>
            {formatCurrency(invoice.grossTotal)}
          </Typography>
          {invoice.openAmount > 0 && (
            <Typography variant="body2" color="text.secondary">
              {t('invoices.openAmount', {
                amount: formatCurrency(invoice.openAmount),
              })}
            </Typography>
          )}
          <InvoiceStatusChip invoice={invoice} />
        </Stack>
      ) : (
        <Stack
          direction="row"
          spacing={2}
          sx={{ alignItems: 'center', justifyContent: 'space-between' }}
        >
          <Typography color="text.secondary">
            {t('invoices.notInvoiced')}
          </Typography>
          {canManageInvoices(user) && (
            <Button
              variant="outlined"
              size="small"
              startIcon={<ReceiptLongIcon />}
              onClick={() => setCreating(true)}
            >
              {t('invoices.create')}
            </Button>
          )}
        </Stack>
      )}
      {creating && (
        <CreateDialog orderId={orderId} onClose={() => setCreating(false)} />
      )}
    </Paper>
  )
}

function CreateDialog({
  orderId,
  onClose,
}: {
  orderId: number
  onClose: () => void
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [serviceDate, setServiceDate] = useState(isoDate())
  // Payable within 14 days unless changed
  const [dueDate, setDueDate] = useState(isoDate(14))
  const create = useMutation({
    mutationFn: () =>
      api<Invoice>('/invoices', {
        method: 'POST',
        body: { orderId, serviceDate, dueDate },
      }),
    onSuccess: (invoice) => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      navigate(`/invoices/${invoice.id}`)
    },
  })

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{t('invoices.create')}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {create.isError && (
            <Alert severity="error">{t('common.error')}</Alert>
          )}
          <Typography>{t('invoices.createExplanation')}</Typography>
          <TextField
            type="date"
            label={t('invoices.serviceDate')}
            helperText={t('invoices.serviceDateHint')}
            slotProps={{ inputLabel: { shrink: true } }}
            value={serviceDate}
            onChange={(event) => setServiceDate(event.target.value)}
          />
          <TextField
            type="date"
            label={t('invoices.dueDate')}
            slotProps={{
              inputLabel: { shrink: true },
              htmlInput: { min: isoDate() },
            }}
            value={dueDate}
            onChange={(event) => setDueDate(event.target.value)}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.cancel')}</Button>
        <Button
          variant="contained"
          disabled={!serviceDate || !dueDate || dueDate < isoDate()}
          loading={create.isPending}
          onClick={() => create.mutate()}
        >
          {t('invoices.issue')}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
