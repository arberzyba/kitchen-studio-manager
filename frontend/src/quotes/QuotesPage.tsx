import AddIcon from '@mui/icons-material/Add'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { deDE } from '@mui/x-data-grid/locales'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { api, type Page } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatCurrency, formatDate } from '../i18n/format'
import { QuoteStatusChip } from './QuoteParts'
import { canEditQuotes, QUOTE_STATUSES, type QuoteSummary } from './types'

export function QuotesPage() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [pagination, setPagination] = useState({ page: 0, pageSize: 20 })

  // Searching, filtering and paging happen on the server; newest quotes come first
  const quotes = useQuery({
    queryKey: ['quotes', search, status, pagination],
    queryFn: () =>
      api<Page<QuoteSummary>>(
        `/quotes?${new URLSearchParams({
          search,
          ...(status && { status }),
          page: String(pagination.page),
          size: String(pagination.pageSize),
        })}`,
      ),
    placeholderData: keepPreviousData,
  })

  const columns: GridColDef<QuoteSummary>[] = [
    { field: 'quoteNumber', headerName: t('quotes.number'), width: 150 },
    { field: 'customerName', headerName: t('quotes.customer'), flex: 1 },
    {
      field: 'quoteDate',
      headerName: t('quotes.date'),
      width: 130,
      valueFormatter: (value: string) => formatDate(value),
    },
    {
      field: 'validUntil',
      headerName: t('quotes.validUntil'),
      width: 130,
      valueFormatter: (value: string) => formatDate(value),
    },
    {
      field: 'status',
      headerName: t('quotes.status'),
      width: 140,
      renderCell: (params) => <QuoteStatusChip status={params.row.status} />,
    },
    {
      field: 'grossTotal',
      headerName: t('quotes.grossTotal'),
      width: 160,
      align: 'right',
      headerAlign: 'right',
      valueFormatter: (value: number) => formatCurrency(value),
    },
  ]

  return (
    <>
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
      >
        <Typography variant="h4" component="h1">
          {t('quotes.title')}
        </Typography>
        {canEditQuotes(user) && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            component={Link}
            to="/quotes/new"
          >
            {t('quotes.new')}
          </Button>
        )}
      </Stack>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField
          label={t('quotes.search')}
          size="small"
          sx={{ width: 320 }}
          value={search}
          onChange={(event) => {
            setSearch(event.target.value)
            setPagination({ ...pagination, page: 0 })
          }}
        />
        <TextField
          select
          label={t('quotes.status')}
          size="small"
          sx={{ width: 200 }}
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPagination({ ...pagination, page: 0 })
          }}
        >
          <MenuItem value="">{t('quotes.allStatuses')}</MenuItem>
          {QUOTE_STATUSES.map((option) => (
            <MenuItem key={option} value={option}>
              {t(`quoteStatuses.${option}`)}
            </MenuItem>
          ))}
        </TextField>
      </Stack>
      {quotes.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      <Box sx={{ display: 'flex', flexDirection: 'column' }}>
        <DataGrid
          rows={quotes.data?.content ?? []}
          columns={columns}
          loading={quotes.isFetching}
          paginationMode="server"
          rowCount={quotes.data?.page.totalElements ?? 0}
          paginationModel={pagination}
          onPaginationModelChange={setPagination}
          pageSizeOptions={[10, 20, 50]}
          disableColumnSorting
          disableColumnMenu
          disableRowSelectionOnClick
          onRowClick={(params) => navigate(`/quotes/${params.id}`)}
          localeText={
            i18n.language === 'de'
              ? deDE.components.MuiDataGrid.defaultProps.localeText
              : undefined
          }
          sx={{ '& .MuiDataGrid-row': { cursor: 'pointer' } }}
        />
      </Box>
    </>
  )
}
