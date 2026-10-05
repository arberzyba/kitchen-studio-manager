import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import de from './de.json'
import en from './en.json'

const LANGUAGE_KEY = 'language'

// Saved choice first, then the browser language; German is the default
function initialLanguage() {
  const stored = localStorage.getItem(LANGUAGE_KEY)
  if (stored) {
    return stored
  }
  return navigator.language.startsWith('en') ? 'en' : 'de'
}

i18n.use(initReactI18next).init({
  resources: { de: { translation: de }, en: { translation: en } },
  lng: initialLanguage(),
  fallbackLng: 'de',
  // React already escapes rendered text
  interpolation: { escapeValue: false },
})

document.documentElement.lang = i18n.language
i18n.on('languageChanged', (language) => {
  localStorage.setItem(LANGUAGE_KEY, language)
  document.documentElement.lang = language
})
