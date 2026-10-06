import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { api } from '../api/client'
import {
  formatCurrency,
  formatDate,
  formatDateTime,
  formatMonth,
} from '../i18n/format'
import type { Invoice } from '../invoices/types'

type DashboardData = {
  openQuotes: { draftCount: number; sentCount: number; sentGrossTotal: number }
  // Net amount invoiced per calendar month, oldest first; month is e.g. "2026-10"
  monthlyRevenue: { month: string; netTotal: number }[]
  upcomingInstallations: {
    appointmentId: number
    startTime: string
    customerName: string
    city: string
    assigneeName: string
    orderId: number
    orderNumber: string
  }[]
  overdueInvoices: { count: number; openAmount: number; invoices: Invoice[] }
}

export function Dashboard() {
  const { t } = useTranslation()
  const dashboard = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => api<DashboardData>('/dashboard'),
  })

  if (dashboard.isError) {
    return <Alert severity="error">{t('common.error')}</Alert>
  }
  if (!dashboard.data) {
    return null
  }
  const { openQuotes, monthlyRevenue, upcomingInstallations, overdueInvoices } =
    dashboard.data
  const thisMonth = monthlyRevenue[monthlyRevenue.length - 1]

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
        <StatCard
          title={t('dashboard.openQuotes')}
          value={String(openQuotes.sentCount)}
          to="/quotes"
        >
          {t('dashboard.openQuotesDetail', {
            amount: formatCurrency(openQuotes.sentGrossTotal),
            drafts: openQuotes.draftCount,
          })}
        </StatCard>
        <StatCard
          title={t('dashboard.revenueThisMonth')}
          value={formatCurrency(thisMonth.netTotal)}
          to="/invoices"
        >
          {t('dashboard.revenueDetail')}
        </StatCard>
        <StatCard
          title={t('dashboard.overduePayments')}
          value={formatCurrency(overdueInvoices.openAmount)}
          to="/invoices"
          warning={overdueInvoices.count > 0}
        >
          {t('dashboard.overdueDetail', { count: overdueInvoices.count })}
        </StatCard>
      </Stack>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" component="h2" gutterBottom>
          {t('dashboard.monthlyRevenue')}
        </Typography>
        <Box sx={{ height: 280 }}>
          <ResponsiveContainer>
            <BarChart
              data={monthlyRevenue.map((entry) => ({
                month: formatMonth(entry.month),
                netTotal: entry.netTotal,
              }))}
            >
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="month" />
              <YAxis
                width={90}
                tickFormatter={(value: number) => formatCurrency(value)}
              />
              <Tooltip
                formatter={(value) => [
                  formatCurrency(Number(value)),
                  t('dashboard.netRevenue'),
                ]}
              />
              <Bar dataKey="netTotal" fill="#1976d2" />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      </Paper>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
        <Paper sx={{ p: 3, flex: 1 }}>
          <Typography variant="h6" component="h2" gutterBottom>
            {t('dashboard.upcomingInstallations')}
          </Typography>
          {upcomingInstallations.length === 0 && (
            <Typography color="text.secondary">
              {t('dashboard.noInstallations')}
            </Typography>
          )}
          <Stack spacing={1.5}>
            {upcomingInstallations.map((installation) => (
              <div key={installation.appointmentId}>
                <Typography>
                  {formatDateTime(installation.startTime)} ·{' '}
                  {installation.customerName}, {installation.city}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  <Link to={`/orders/${installation.orderId}`}>
                    {installation.orderNumber}
                  </Link>
                  {' · '}
                  {installation.assigneeName}
                </Typography>
              </div>
            ))}
          </Stack>
        </Paper>
        <Paper sx={{ p: 3, flex: 1 }}>
          <Typography variant="h6" component="h2" gutterBottom>
            {t('dashboard.overdueInvoices')}
          </Typography>
          {overdueInvoices.invoices.length === 0 && (
            <Typography color="text.secondary">
              {t('dashboard.noOverdue')}
            </Typography>
          )}
          <Stack spacing={1.5}>
            {overdueInvoices.invoices.map((invoice) => (
              <Stack
                key={invoice.id}
                direction="row"
                spacing={2}
                sx={{ justifyContent: 'space-between' }}
              >
                <div>
                  <Typography>
                    <Link to={`/invoices/${invoice.id}`}>
                      {invoice.invoiceNumber}
                    </Link>
                    {' · '}
                    {invoice.recipientCompany ?? invoice.recipientName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t('dashboard.dueSince', {
                      date: formatDate(invoice.dueDate),
                    })}
                  </Typography>
                </div>
                <Typography color="error">
                  {formatCurrency(invoice.openAmount)}
                </Typography>
              </Stack>
            ))}
          </Stack>
        </Paper>
      </Stack>
    </Stack>
  )
}

// One headline figure; the whole card links to the page with the details
function StatCard({
  title,
  value,
  to,
  warning,
  children,
}: {
  title: string
  value: string
  to: string
  warning?: boolean
  children: ReactNode
}) {
  return (
    <Paper
      component={Link}
      to={to}
      sx={{ p: 3, flex: 1, textDecoration: 'none', color: 'inherit' }}
    >
      <Typography variant="body2" color="text.secondary">
        {title}
      </Typography>
      <Typography variant="h4" color={warning ? 'error' : 'inherit'}>
        {value}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {children}
      </Typography>
    </Paper>
  )
}
