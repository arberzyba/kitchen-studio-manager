import EditIcon from '@mui/icons-material/Edit'
import EmailIcon from '@mui/icons-material/Email'
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { api, downloadFile } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatDate } from '../i18n/format'
import { QuoteOrderAction } from '../orders/QuoteOrderAction'
import { QuoteItemsCard, QuoteStatusChip } from './QuoteParts'
import { canEditQuotes, type Quote, type QuoteStatus } from './types'

export function QuoteDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [confirmSend, setConfirmSend] = useState(false)
  const quote = useQuery({
    queryKey: ['quote', id],
    queryFn: () => api<Quote>(`/quotes/${id}`),
  })

  function onChanged(updated: Quote) {
    queryClient.setQueryData(['quote', id], updated)
    queryClient.invalidateQueries({ queryKey: ['quotes'] })
  }

  const changeStatus = useMutation({
    mutationFn: (status: QuoteStatus) =>
      api<Quote>(`/quotes/${id}/status`, { method: 'POST', body: { status } }),
    onSuccess: onChanged,
  })
  const send = useMutation({
    mutationFn: () => api<Quote>(`/quotes/${id}/send`, { method: 'POST' }),
    onSuccess: onChanged,
    onSettled: () => setConfirmSend(false),
  })
  const downloadPdf = useMutation({
    mutationFn: (quoteNumber: string) =>
      downloadFile(`/quotes/${id}/pdf`, `Angebot-${quoteNumber}.pdf`),
  })

  if (quote.isError) {
    return <Alert severity="error">{t('common.error')}</Alert>
  }
  if (!quote.data) {
    return null
  }
  const { status, customerEmail } = quote.data
  const canEdit = canEditQuotes(user)
  const canSend = canEdit && (status === 'DRAFT' || status === 'SENT')

  return (
    <Stack spacing={3}>
      <Stack
        direction="row"
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: 'center' }}
      >
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          <Typography variant="h4" component="h1">
            {t('quotes.heading', { number: quote.data.quoteNumber })}
          </Typography>
          <QuoteStatusChip status={status} />
        </Stack>
        <Stack direction="row" spacing={1}>
          {canEdit && status === 'DRAFT' && (
            <Button
              variant="outlined"
              startIcon={<EditIcon />}
              component={Link}
              to={`/quotes/${id}/edit`}
            >
              {t('quotes.edit')}
            </Button>
          )}
          <Button
            variant="outlined"
            startIcon={<PictureAsPdfIcon />}
            loading={downloadPdf.isPending}
            onClick={() => downloadPdf.mutate(quote.data.quoteNumber)}
          >
            {t('quotes.pdf')}
          </Button>
          {canSend && (
            <Button
              variant="contained"
              startIcon={<EmailIcon />}
              disabled={!customerEmail}
              onClick={() => setConfirmSend(true)}
            >
              {status === 'SENT' ? t('quotes.sendAgain') : t('quotes.send')}
            </Button>
          )}
        </Stack>
      </Stack>

      {canSend && !customerEmail && (
        <Alert severity="info">{t('quotes.noEmail')}</Alert>
      )}
      {send.isSuccess && (
        <Alert severity="success">
          {t('quotes.sentTo', { email: customerEmail })}
        </Alert>
      )}
      {(send.isError || changeStatus.isError || downloadPdf.isError) && (
        <Alert severity="error">{t('common.error')}</Alert>
      )}

      <Paper sx={{ p: 3 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
          <Detail label={t('quotes.customer')}>
            <Link to={`/customers/${quote.data.customerId}`}>
              {quote.data.customerName}
            </Link>
          </Detail>
          <Detail label={t('quotes.date')}>
            {formatDate(quote.data.quoteDate)}
          </Detail>
          <Detail label={t('quotes.validUntil')}>
            {formatDate(quote.data.validUntil)}
          </Detail>
          <Detail label={t('quotes.createdBy')}>
            {quote.data.createdByName}
          </Detail>
        </Stack>
      </Paper>

      <QuoteItemsCard quote={quote.data} />

      {status === 'ACCEPTED' && <QuoteOrderAction quoteId={quote.data.id} />}

      {canEdit && (status === 'DRAFT' || status === 'SENT') && (
        <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
          {status === 'DRAFT' && (
            <Button
              loading={changeStatus.isPending}
              onClick={() => changeStatus.mutate('SENT')}
            >
              {t('quotes.markSent')}
            </Button>
          )}
          {status === 'SENT' && (
            <>
              <Button
                color="error"
                loading={changeStatus.isPending}
                onClick={() => changeStatus.mutate('REJECTED')}
              >
                {t('quotes.markRejected')}
              </Button>
              <Button
                color="success"
                variant="contained"
                loading={changeStatus.isPending}
                onClick={() => changeStatus.mutate('ACCEPTED')}
              >
                {t('quotes.markAccepted')}
              </Button>
            </>
          )}
        </Stack>
      )}

      <Dialog open={confirmSend} onClose={() => setConfirmSend(false)}>
        <DialogTitle>{t('quotes.send')}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t('quotes.confirmSend', {
              number: quote.data.quoteNumber,
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

function Detail({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography component="div">{children}</Typography>
    </div>
  )
}
