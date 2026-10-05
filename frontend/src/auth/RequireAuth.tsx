import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import { Navigate, Outlet } from 'react-router'
import type { Role } from '../users/types'
import { useAuth } from './AuthContext'

// Guards nested routes: requires a login and, if given, one of the listed roles
export function RequireAuth({ roles }: { roles?: Role[] }) {
  const { user, isLoading } = useAuth()

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    )
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
