import LanguageIcon from '@mui/icons-material/Language'
import Button from '@mui/material/Button'
import { useTranslation } from 'react-i18next'

// Toggles between German and English; the label names the language it switches to
export function LanguageButton() {
  const { t, i18n } = useTranslation()

  return (
    <Button
      color="inherit"
      startIcon={<LanguageIcon />}
      onClick={() => i18n.changeLanguage(i18n.language === 'de' ? 'en' : 'de')}
    >
      {t('language.switchTo')}
    </Button>
  )
}
