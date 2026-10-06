import DeleteForeverIcon from '@mui/icons-material/DeleteForever'
import DownloadIcon from '@mui/icons-material/Download'
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
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { api, downloadFile } from '../api/client'
import type { Customer } from './types'

// The two requests a customer can make under the GDPR: a copy of their data, and its erasure
export function CustomerPrivacy({ customer }: { customer: Customer }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [confirming, setConfirming] = useState(false)

  const exportData = useMutation({
    mutationFn: () =>
      downloadFile(
        `/customers/${customer.id}/export`,
        `customer-${customer.id}-data.json`,
      ),
  })
  const erase = useMutation({
    mutationFn: () =>
      api<{ outcome: 'DELETED' | 'ANONYMIZED' }>(`/customers/${customer.id}`, {
        method: 'DELETE',
      }),
    onSuccess: ({ outcome }) => {
      setConfirming(false)
      // Every list and page that showed this customer's name is now out of date
      queryClient.invalidateQueries()
      if (outcome === 'DELETED') {
        navigate('/customers')
      }
    },
  })

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" component="h2" gutterBottom>
        {t('privacy.title')}
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 2 }}>
        {t('privacy.explanation')}
      </Typography>
      {(exportData.isError || erase.isError) && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      <Stack direction="row" spacing={2}>
        <Button
          variant="outlined"
          startIcon={<DownloadIcon />}
          loading={exportData.isPending}
          onClick={() => exportData.mutate()}
        >
          {t('privacy.export')}
        </Button>
        <Button
          variant="outlined"
          color="error"
          startIcon={<DeleteForeverIcon />}
          onClick={() => setConfirming(true)}
        >
          {t('privacy.erase')}
        </Button>
      </Stack>

      <Dialog open={confirming} onClose={() => setConfirming(false)}>
        <DialogTitle>
          {t('privacy.confirmTitle', {
            name: `${customer.firstName} ${customer.lastName}`,
          })}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>{t('privacy.confirmText')}</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirming(false)}>
            {t('common.cancel')}
          </Button>
          <Button
            color="error"
            variant="contained"
            loading={erase.isPending}
            onClick={() => erase.mutate()}
          >
            {t('privacy.confirmButton')}
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  )
}
