import Typography from '@mui/material/Typography'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const { t } = useTranslation()
  const { user } = useAuth()

  return (
    <Typography variant="h4" component="h1">
      {t('home.welcome', { name: user?.firstName })}
    </Typography>
  )
}
