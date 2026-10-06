import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { deDE } from '@mui/x-data-grid/locales'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { api, type Page } from '../api/client'
import { formatCurrency, formatDate } from '../i18n/format'
import { ORDER_STATUSES, type OrderSummary } from './types'

export function OrdersPage() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [pagination, setPagination] = useState({ page: 0, pageSize: 20 })

  // Searching, filtering and paging happen on the server; newest orders come first
  const orders = useQuery({
    queryKey: ['orders', search, status, pagination],
    queryFn: () =>
      api<Page<OrderSummary>>(
        `/orders?${new URLSearchParams({
          search,
          ...(status && { status }),
          page: String(pagination.page),
          size: String(pagination.pageSize),
        })}`,
      ),
    placeholderData: keepPreviousData,
  })

  const columns: GridColDef<OrderSummary>[] = [
    { field: 'orderNumber', headerName: t('orders.number'), width: 150 },
    { field: 'customerName', headerName: t('orders.customer'), flex: 1 },
    {
      field: 'orderDate',
      headerName: t('orders.date'),
      width: 130,
      valueFormatter: (value: string) => formatDate(value),
    },
    {
      field: 'status',
      headerName: t('orders.status'),
      width: 220,
      valueFormatter: (value: string) => t(`orderStatuses.${value}`),
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
      <Typography variant="h4" component="h1" sx={{ mb: 2 }}>
        {t('orders.title')}
      </Typography>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField
          label={t('orders.search')}
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
          label={t('orders.status')}
          size="small"
          sx={{ width: 240 }}
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPagination({ ...pagination, page: 0 })
          }}
        >
          <MenuItem value="">{t('orders.allStatuses')}</MenuItem>
          {ORDER_STATUSES.map((option) => (
            <MenuItem key={option} value={option}>
              {t(`orderStatuses.${option}`)}
            </MenuItem>
          ))}
        </TextField>
      </Stack>
      {orders.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      <Box sx={{ display: 'flex', flexDirection: 'column' }}>
        <DataGrid
          rows={orders.data?.content ?? []}
          columns={columns}
          loading={orders.isFetching}
          paginationMode="server"
          rowCount={orders.data?.page.totalElements ?? 0}
          paginationModel={pagination}
          onPaginationModelChange={setPagination}
          pageSizeOptions={[10, 20, 50]}
          disableColumnSorting
          disableColumnMenu
          disableRowSelectionOnClick
          onRowClick={(params) => navigate(`/orders/${params.id}`)}
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
