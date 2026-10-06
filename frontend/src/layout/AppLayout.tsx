import AssignmentIcon from '@mui/icons-material/Assignment'
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import ContactsIcon from '@mui/icons-material/Contacts'
import DescriptionIcon from '@mui/icons-material/Description'
import HomeIcon from '@mui/icons-material/Home'
import InventoryIcon from '@mui/icons-material/Inventory2'
import LocalShippingIcon from '@mui/icons-material/LocalShipping'
import LogoutIcon from '@mui/icons-material/Logout'
import PeopleIcon from '@mui/icons-material/People'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Drawer from '@mui/material/Drawer'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { LanguageButton } from '../i18n/LanguageButton'
import type { Role } from '../users/types'

const DRAWER_WIDTH = 220

// Entries without "roles" are visible to every employee
const NAV_ITEMS: {
  to: string
  labelKey: string
  icon: ReactNode
  roles?: Role[]
}[] = [
  { to: '/', labelKey: 'nav.home', icon: <HomeIcon /> },
  {
    to: '/customers',
    labelKey: 'nav.customers',
    icon: <ContactsIcon />,
    roles: ['ADMIN', 'SALES', 'OFFICE'],
  },
  {
    to: '/quotes',
    labelKey: 'nav.quotes',
    icon: <DescriptionIcon />,
    roles: ['ADMIN', 'SALES', 'OFFICE'],
  },
  {
    to: '/orders',
    labelKey: 'nav.orders',
    icon: <AssignmentIcon />,
    roles: ['ADMIN', 'SALES', 'OFFICE'],
  },
  { to: '/calendar', labelKey: 'nav.calendar', icon: <CalendarMonthIcon /> },
  {
    to: '/supplier-orders',
    labelKey: 'nav.supplierOrders',
    icon: <ShoppingCartIcon />,
    roles: ['ADMIN', 'SALES', 'OFFICE'],
  },
  {
    to: '/products',
    labelKey: 'nav.products',
    icon: <InventoryIcon />,
    roles: ['ADMIN', 'SALES', 'OFFICE'],
  },
  {
    to: '/suppliers',
    labelKey: 'nav.suppliers',
    icon: <LocalShippingIcon />,
    roles: ['ADMIN', 'SALES', 'OFFICE'],
  },
  {
    to: '/users',
    labelKey: 'nav.users',
    icon: <PeopleIcon />,
    roles: ['ADMIN'],
  },
]

export function AppLayout() {
  const { t } = useTranslation()
  const { user, logout } = useAuth()
  if (!user) {
    return null
  }

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}
      >
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            SedzKitchens
          </Typography>
          <Typography variant="body2" sx={{ mr: 2 }}>
            {user.firstName} {user.lastName} ({t(`roles.${user.role}`)})
          </Typography>
          <LanguageButton />
          <Button color="inherit" startIcon={<LogoutIcon />} onClick={logout}>
            {t('nav.signOut')}
          </Button>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
        }}
      >
        <Toolbar />
        <List component="nav">
          {NAV_ITEMS.filter(
            (item) => !item.roles || item.roles.includes(user.role),
          ).map((item) => (
            <ListItemButton
              key={item.to}
              component={NavLink}
              to={item.to}
              sx={{ '&.active': { bgcolor: 'action.selected' } }}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={t(item.labelKey)} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  )
}
