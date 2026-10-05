import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControlLabel from '@mui/material/FormControlLabel'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { api, ApiError } from '../api/client'
import { ROLES, type User } from './types'

// Email and password can only be set when creating; editing leaves them untouched.
// Error messages are translation keys, resolved when rendered.
function createSchema(isNew: boolean) {
  return z.object({
    email: z.email('users.emailInvalid'),
    password: isNew
      ? z.string().min(8, 'users.passwordTooShort').max(72)
      : z.string(),
    firstName: z.string().trim().min(1, 'users.firstNameRequired'),
    lastName: z.string().trim().min(1, 'users.lastNameRequired'),
    role: z.enum(ROLES),
    active: z.boolean(),
  })
}

type FormValues = z.infer<ReturnType<typeof createSchema>>

// Pass a user to edit it, or none to create a new one
export function UserDialog({
  user,
  onClose,
}: {
  user?: User
  onClose: () => void
}) {
  const { t } = useTranslation()
  const isNew = !user
  const queryClient = useQueryClient()
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(createSchema(isNew)),
    defaultValues: {
      email: user?.email ?? '',
      password: '',
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
      role: user?.role ?? 'SALES',
      active: user?.active ?? true,
    },
  })

  const save = useMutation({
    mutationFn: ({ email, password, ...rest }: FormValues) =>
      isNew
        ? api<User>('/users', {
            method: 'POST',
            body: { email, password, ...rest },
          })
        : api<User>(`/users/${user.id}`, { method: 'PUT', body: rest }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      onClose()
    },
  })

  // The backend answers 409 for a taken email (create) or a change to one's own account (edit)
  function saveErrorKey() {
    if (save.error instanceof ApiError && save.error.status === 409) {
      return isNew ? 'users.emailTaken' : 'users.ownAccount'
    }
    return 'common.error'
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <form noValidate onSubmit={handleSubmit((values) => save.mutate(values))}>
        <DialogTitle>{isNew ? t('users.new') : t('users.edit')}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {save.isError && (
              <Alert severity="error">{t(saveErrorKey())}</Alert>
            )}
            <TextField
              label={t('users.email')}
              type="email"
              disabled={!isNew}
              error={!!errors.email}
              helperText={errors.email?.message && t(errors.email.message)}
              {...register('email')}
            />
            {isNew && (
              <TextField
                label={t('users.password')}
                type="password"
                autoComplete="new-password"
                error={!!errors.password}
                helperText={
                  errors.password?.message && t(errors.password.message)
                }
                {...register('password')}
              />
            )}
            <TextField
              label={t('users.firstName')}
              error={!!errors.firstName}
              helperText={
                errors.firstName?.message && t(errors.firstName.message)
              }
              {...register('firstName')}
            />
            <TextField
              label={t('users.lastName')}
              error={!!errors.lastName}
              helperText={
                errors.lastName?.message && t(errors.lastName.message)
              }
              {...register('lastName')}
            />
            <Controller
              name="role"
              control={control}
              render={({ field }) => (
                <TextField select label={t('users.role')} {...field}>
                  {ROLES.map((role) => (
                    <MenuItem key={role} value={role}>
                      {t(`roles.${role}`)}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
            {!isNew && (
              <Controller
                name="active"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    label={t('users.active')}
                    control={
                      <Switch
                        checked={field.value}
                        onChange={(event) =>
                          field.onChange(event.target.checked)
                        }
                      />
                    }
                  />
                )}
              />
            )}
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
