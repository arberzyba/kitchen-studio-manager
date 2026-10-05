import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { api, ApiError } from '../api/client'
import type { Supplier } from './types'

// Error messages are translation keys, resolved when rendered
const schema = z.object({
  name: z.string().trim().min(1, 'suppliers.nameRequired'),
  email: z.union([z.literal(''), z.email('suppliers.emailInvalid')]),
  phone: z.string(),
})

type FormValues = z.infer<typeof schema>

// Pass a supplier to edit it, or none to create a new one
export function SupplierDialog({
  supplier,
  onClose,
}: {
  supplier?: Supplier
  onClose: () => void
}) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: supplier?.name ?? '',
      email: supplier?.email ?? '',
      phone: supplier?.phone ?? '',
    },
  })

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      api<Supplier>(supplier ? `/suppliers/${supplier.id}` : '/suppliers', {
        method: supplier ? 'PUT' : 'POST',
        body: {
          name: values.name,
          email: values.email || null,
          phone: values.phone || null,
        },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['suppliers'] })
      // Product rows show the supplier name
      queryClient.invalidateQueries({ queryKey: ['products'] })
      onClose()
    },
  })

  const nameTaken = save.error instanceof ApiError && save.error.status === 409

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <form noValidate onSubmit={handleSubmit((values) => save.mutate(values))}>
        <DialogTitle>
          {supplier ? t('suppliers.edit') : t('suppliers.new')}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {save.isError && (
              <Alert severity="error">
                {t(nameTaken ? 'suppliers.nameTaken' : 'common.error')}
              </Alert>
            )}
            <TextField
              label={t('suppliers.name')}
              error={!!errors.name}
              helperText={errors.name?.message && t(errors.name.message)}
              {...register('name')}
            />
            <TextField
              label={t('suppliers.email')}
              type="email"
              error={!!errors.email}
              helperText={errors.email?.message && t(errors.email.message)}
              {...register('email')}
            />
            <TextField label={t('suppliers.phone')} {...register('phone')} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>{t('common.cancel')}</Button>
          <Button type="submit" variant="contained" loading={save.isPending}>
            {t('common.save')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
