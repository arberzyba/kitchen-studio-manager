import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useForm } from 'react-hook-form'
import { Navigate } from 'react-router'
import { z } from 'zod'
import { useAuth } from './AuthContext'

const schema = z.object({
  email: z.email('Enter a valid email address'),
  password: z.string().min(1, 'Enter your password'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
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
      setError('root', { message: (error as Error).message })
    }
  }

  return (
    <Container maxWidth="xs" sx={{ mt: 12 }}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" component="h1" gutterBottom>
          SedzKitchens
        </Typography>
        <Stack
          component="form"
          spacing={2}
          noValidate
          onSubmit={handleSubmit(onSubmit)}
        >
          {errors.root && <Alert severity="error">{errors.root.message}</Alert>}
          <TextField
            label="Email"
            type="email"
            autoComplete="username"
            autoFocus
            error={!!errors.email}
            helperText={errors.email?.message}
            {...register('email')}
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="current-password"
            error={!!errors.password}
            helperText={errors.password?.message}
            {...register('password')}
          />
          <Button type="submit" variant="contained" loading={isSubmitting}>
            Sign in
          </Button>
        </Stack>
      </Paper>
    </Container>
  )
}
