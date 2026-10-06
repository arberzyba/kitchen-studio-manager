import Typography from '@mui/material/Typography'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { Dashboard } from '../dashboard/Dashboard'

export function HomePage() {
  const { t } = useTranslation()
  const { user } = useAuth()

  return (
    <>
      <Typography variant="h4" component="h1" sx={{ mb: 3 }}>
        {t('home.welcome', { name: user?.firstName })}
      </Typography>
      {/* Mirrors the backend rule: installers have no access to the business figures */}
      {user?.role !== 'INSTALLER' && <Dashboard />}
    </>
  )
}
