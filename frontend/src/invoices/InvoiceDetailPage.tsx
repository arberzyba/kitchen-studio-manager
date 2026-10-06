import DeleteIcon from '@mui/icons-material/Delete'
import EmailIcon from '@mui/icons-material/Email'
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import IconButton from '@mui/material/IconButton'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { api, downloadFile } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatCurrency, formatDate } from '../i18n/format'
import { QuoteItemsCard } from '../quotes/QuoteParts'
import { InvoiceStatusChip } from './InvoiceStatusChip'
import { canManageInvoices, isoDate, type Invoice } from './types'

export function InvoiceDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const { user } = useAuth()
  const [confirmSend, setConfirmSend] = useState(false)
  const invoice = useQuery({
    queryKey: ['invoice', id],
    queryFn: () => api<Invoice>(`/invoices/${id}`),
  })
  const send = useMutation({
    mutationFn: () => api<void>(`/invoices/${id}/send`, { method: 'POST' }),
    onSettled: () => setConfirmSend(false),
  })
  const downloadPdf = useMutation({
    mutationFn: (invoiceNumber: string) =>
      downloadFile(`/invoices/${id}/pdf`, `Rechnung-${invoiceNumber}.pdf`),
  })

  if (invoice.isError) {
    return <Alert severity="error">{t('common.error')}</Alert>
  }
  if (!invoice.data?.details) {
    return null
  }
  const { data } = invoice
  const { customerId, customerEmail, quote } = invoice.data.details
  const canManage = canManageInvoices(user)

  return (
    <Stack spacing={3}>
      <Stack
        direction="row"
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: 'center' }}
      >
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          <Typography variant="h4" component="h1">
            {t('invoices.heading', { number: data.invoiceNumber })}
          </Typography>
          <InvoiceStatusChip invoice={data} />
        </Stack>
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            startIcon={<PictureAsPdfIcon />}
            loading={downloadPdf.isPending}
            onClick={() => downloadPdf.mutate(data.invoiceNumber)}
          >
            {t('quotes.pdf')}
          </Button>
          {canManage && (
            <Button
              variant="contained"
              startIcon={<EmailIcon />}
              disabled={!customerEmail}
              onClick={() => setConfirmSend(true)}
            >
              {t('quotes.send')}
            </Button>
          )}
        </Stack>
      </Stack>

      {canManage && !customerEmail && (
        <Alert severity="info">{t('invoices.noEmail')}</Alert>
      )}
      {send.isSuccess && (
        <Alert severity="success">
          {t('invoices.sentTo', { email: customerEmail })}
        </Alert>
      )}
      {(send.isError || downloadPdf.isError) && (
        <Alert severity="error">{t('common.error')}</Alert>
      )}

      <Paper sx={{ p: 3 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
          <Detail label={t('invoices.customer')}>
            <Link to={`/customers/${customerId}`}>{data.recipientName}</Link>
          </Detail>
          <Detail label={t('invoices.order')}>
            <Link to={`/orders/${data.orderId}`}>{data.orderNumber}</Link>
          </Detail>
          <Detail label={t('invoices.date')}>
            {formatDate(data.invoiceDate)}
          </Detail>
          <Detail label={t('invoices.serviceDate')}>
            {formatDate(data.serviceDate)}
          </Detail>
          <Detail label={t('invoices.dueDate')}>
            {formatDate(data.dueDate)}
          </Detail>
        </Stack>
      </Paper>

      <Payments invoice={data} canManage={canManage} />

      <QuoteItemsCard quote={quote} />

      <Dialog open={confirmSend} onClose={() => setConfirmSend(false)}>
        <DialogTitle>{t('quotes.send')}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t('invoices.confirmSend', {
              number: data.invoiceNumber,
              email: customerEmail,
            })}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmSend(false)}>
            {t('common.cancel')}
          </Button>
          <Button
            variant="contained"
            loading={send.isPending}
            onClick={() => send.mutate()}
          >
            {t('quotes.confirmSendButton')}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}

// What was invoiced, what has been paid and what is still open, plus a form to record a payment
function Payments({
  invoice,
  canManage,
}: {
  invoice: Invoice
  canManage: boolean
}) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  // Empty means "the whole open amount", which is the usual case
  const [amount, setAmount] = useState('')
  const [paidOn, setPaidOn] = useState(isoDate())
  const [note, setNote] = useState('')
  const payments = invoice.details?.payments ?? []
  const enteredAmount = amount === '' ? invoice.openAmount : Number(amount)
  const amountValid = enteredAmount > 0 && enteredAmount <= invoice.openAmount

  function onChanged(updated: Invoice) {
    queryClient.setQueryData(['invoice', String(invoice.id)], updated)
    queryClient.invalidateQueries({ queryKey: ['invoices'] })
  }

  const add = useMutation({
    mutationFn: () =>
      api<Invoice>(`/invoices/${invoice.id}/payments`, {
        method: 'POST',
        body: { amount: enteredAmount, paidOn, note: note || null },
      }),
    onSuccess: (updated) => {
      setAmount('')
      setNote('')
      onChanged(updated)
    },
  })
  const remove = useMutation({
    mutationFn: (paymentId: number) =>
      api<Invoice>(`/invoices/${invoice.id}/payments/${paymentId}`, {
        method: 'DELETE',
      }),
    onSuccess: onChanged,
  })

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" component="h2" gutterBottom>
        {t('invoices.payments')}
      </Typography>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={4} sx={{ mb: 2 }}>
        <Detail label={t('invoices.amount')}>
          {formatCurrency(invoice.grossTotal)}
        </Detail>
        <Detail label={t('invoices.paid')}>
          {formatCurrency(invoice.paidTotal)}
        </Detail>
        <Detail label={t('invoices.open')}>
          <strong>{formatCurrency(invoice.openAmount)}</strong>
        </Detail>
      </Stack>
      {(add.isError || remove.isError) && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      {payments.length > 0 && (
        <Table size="small" sx={{ mb: 3 }}>
          <TableHead>
            <TableRow>
              <TableCell>{t('invoices.paidOn')}</TableCell>
              <TableCell>{t('invoices.note')}</TableCell>
              <TableCell align="right">{t('invoices.paymentAmount')}</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {payments.map((payment) => (
              <TableRow key={payment.id}>
                <TableCell>{formatDate(payment.paidOn)}</TableCell>
                <TableCell>{payment.note}</TableCell>
                <TableCell align="right">
                  {formatCurrency(payment.amount)}
                </TableCell>
                <TableCell align="right">
                  {canManage && (
                    <IconButton
                      size="small"
                      aria-label={t('invoices.removePayment', {
                        amount: formatCurrency(payment.amount),
                      })}
                      onClick={() => remove.mutate(payment.id)}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      {canManage && invoice.openAmount > 0 && (
        <Stack
          component="form"
          direction={{ xs: 'column', md: 'row' }}
          spacing={2}
          sx={{ alignItems: 'flex-start' }}
          onSubmit={(event) => {
            event.preventDefault()
            add.mutate()
          }}
        >
          <TextField
            type="number"
            label={t('invoices.paymentAmount')}
            size="small"
            sx={{ width: 180 }}
            placeholder={String(invoice.openAmount)}
            slotProps={{
              inputLabel: { shrink: true },
              htmlInput: { step: '0.01', min: '0.01' },
              input: { endAdornment: '€' },
            }}
            error={!amountValid}
            helperText={!amountValid && t('invoices.amountInvalid')}
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
          />
          <TextField
            type="date"
            label={t('invoices.paidOn')}
            size="small"
            slotProps={{
              inputLabel: { shrink: true },
              htmlInput: { max: isoDate() },
            }}
            value={paidOn}
            onChange={(event) => setPaidOn(event.target.value)}
          />
          <TextField
            label={t('invoices.note')}
            size="small"
            sx={{ flexGrow: 1 }}
            slotProps={{ htmlInput: { maxLength: 500 } }}
            value={note}
            onChange={(event) => setNote(event.target.value)}
          />
          <Button
            type="submit"
            variant="contained"
            disabled={!amountValid || !paidOn || paidOn > isoDate()}
            loading={add.isPending}
          >
            {t('invoices.recordPayment')}
          </Button>
        </Stack>
      )}
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
