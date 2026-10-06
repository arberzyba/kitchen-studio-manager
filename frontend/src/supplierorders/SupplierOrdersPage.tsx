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
import { SupplierOrderStatusChip } from './SupplierOrderStatusChip'
import { SUPPLIER_ORDER_STATUSES, type SupplierOrder } from './types'

export function SupplierOrdersPage() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [pagination, setPagination] = useState({ page: 0, pageSize: 20 })

  // Searching, filtering and paging happen on the server; newest supplier orders come first
  const orders = useQuery({
    queryKey: ['supplierOrders', search, status, pagination],
    queryFn: () =>
      api<Page<SupplierOrder>>(
        `/supplier-orders?${new URLSearchParams({
          search,
          ...(status && { status }),
          page: String(pagination.page),
          size: String(pagination.pageSize),
        })}`,
      ),
    placeholderData: keepPreviousData,
  })

  const columns: GridColDef<SupplierOrder>[] = [
    {
      field: 'orderNumber',
      headerName: t('supplierOrders.number'),
      width: 140,
    },
    {
      field: 'supplierName',
      headerName: t('supplierOrders.supplier'),
      flex: 1.5,
    },
    {
      field: 'salesOrderNumber',
      headerName: t('supplierOrders.forOrder'),
      flex: 1.5,
      valueGetter: (_, row) => `${row.salesOrderNumber} · ${row.customerName}`,
    },
    {
      field: 'expectedDeliveryDate',
      headerName: t('supplierOrders.expected'),
      width: 130,
      valueFormatter: (value: string) => formatDate(value),
    },
    {
      field: 'actualDeliveryDate',
      headerName: t('supplierOrders.delivered'),
      width: 130,
      valueFormatter: (value: string | null) =>
        value ? formatDate(value) : '',
    },
    {
      field: 'status',
      headerName: t('supplierOrders.status'),
      width: 130,
      renderCell: (params) => <SupplierOrderStatusChip order={params.row} />,
    },
    {
      field: 'total',
      headerName: t('supplierOrders.total'),
      width: 150,
      align: 'right',
      headerAlign: 'right',
      valueFormatter: (value: number) => formatCurrency(value),
    },
  ]

  return (
    <>
      <Typography variant="h4" component="h1" sx={{ mb: 2 }}>
        {t('supplierOrders.title')}
      </Typography>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField
          label={t('supplierOrders.search')}
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
          label={t('supplierOrders.status')}
          size="small"
          sx={{ width: 200 }}
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPagination({ ...pagination, page: 0 })
          }}
        >
          <MenuItem value="">{t('supplierOrders.allStatuses')}</MenuItem>
          {SUPPLIER_ORDER_STATUSES.map((option) => (
            <MenuItem key={option} value={option}>
              {t(`supplierOrderStatuses.${option}`)}
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
          onRowClick={(params) => navigate(`/supplier-orders/${params.id}`)}
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
