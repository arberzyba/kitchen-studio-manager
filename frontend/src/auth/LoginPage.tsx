import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Navigate } from 'react-router'
import { z } from 'zod'
import { ApiError } from '../api/client'
import { LanguageButton } from '../i18n/LanguageButton'
import { useAuth } from './AuthContext'

// Error messages are translation keys, resolved when rendered
const schema = z.object({
  email: z.email('login.emailInvalid'),
  password: z.string().min(1, 'login.passwordRequired'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
  const { t } = useTranslation()
  const { user, login } = useAuth()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  if (user) {
    return <Navigate to="/" replace />
  }

  async function onSubmit(values: FormValues) {
    try {
      await login(values.email, values.password)
    } catch (error) {
      const unauthorized = error instanceof ApiError && error.status === 401
      setError('root', {
        message: unauthorized ? 'login.invalid' : 'common.error',
      })
    }
  }

  return (
    <Container maxWidth="xs" sx={{ mt: 12 }}>
      <Paper sx={{ p: 4 }}>
        <Stack
          direction="row"
          sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
        >
          <Typography variant="h5" component="h1">
            SedzKitchens
          </Typography>
          <LanguageButton />
        </Stack>
        <Stack
          component="form"
          spacing={2}
          noValidate
          onSubmit={handleSubmit(onSubmit)}
        >
          {errors.root?.message && (
            <Alert severity="error">{t(errors.root.message)}</Alert>
          )}
          <TextField
            label={t('login.email')}
            type="email"
            autoComplete="username"
            autoFocus
            error={!!errors.email}
            helperText={errors.email?.message && t(errors.email.message)}
            {...register('email')}
          />
          <TextField
            label={t('login.password')}
            type="password"
            autoComplete="current-password"
            error={!!errors.password}
            helperText={errors.password?.message && t(errors.password.message)}
            {...register('password')}
          />
          <Button type="submit" variant="contained" loading={isSubmitting}>
            {t('login.submit')}
          </Button>
        </Stack>
      </Paper>
    </Container>
  )
}
