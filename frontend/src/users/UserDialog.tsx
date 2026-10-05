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
import { z } from 'zod'
import { api } from '../api/client'
import { ROLES, type User } from './types'

// Email and password can only be set when creating; editing leaves them untouched
function createSchema(isNew: boolean) {
  return z.object({
    email: z.email('Enter a valid email address'),
    password: isNew
      ? z.string().min(8, 'Use at least 8 characters').max(72)
      : z.string(),
    firstName: z.string().trim().min(1, 'Enter a first name'),
    lastName: z.string().trim().min(1, 'Enter a last name'),
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

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <form noValidate onSubmit={handleSubmit((values) => save.mutate(values))}>
        <DialogTitle>{isNew ? 'New user' : 'Edit user'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {save.isError && (
              <Alert severity="error">{save.error.message}</Alert>
            )}
            <TextField
              label="Email"
              type="email"
              disabled={!isNew}
              error={!!errors.email}
              helperText={errors.email?.message}
              {...register('email')}
            />
            {isNew && (
              <TextField
                label="Password"
                type="password"
                autoComplete="new-password"
                error={!!errors.password}
                helperText={errors.password?.message}
                {...register('password')}
              />
            )}
            <TextField
              label="First name"
              error={!!errors.firstName}
              helperText={errors.firstName?.message}
              {...register('firstName')}
            />
            <TextField
              label="Last name"
              error={!!errors.lastName}
              helperText={errors.lastName?.message}
              {...register('lastName')}
            />
            <Controller
              name="role"
              control={control}
              render={({ field }) => (
                <TextField select label="Role" {...field}>
                  {ROLES.map((role) => (
                    <MenuItem key={role} value={role}>
                      {role}
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
                    label="Active"
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
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" loading={save.isPending}>
            Save
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
