import Typography from '@mui/material/Typography'
import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const { user } = useAuth()

  return (
    <Typography variant="h4" component="h1">
      Welcome, {user?.firstName}
    </Typography>
  )
}
