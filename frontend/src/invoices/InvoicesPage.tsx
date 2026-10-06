import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Checkbox from '@mui/material/Checkbox'
import FormControlLabel from '@mui/material/FormControlLabel'
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
import { InvoiceStatusChip } from './InvoiceStatusChip'
import { INVOICE_STATUSES, type Invoice } from './types'

export function InvoicesPage() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [overdueOnly, setOverdueOnly] = useState(false)
  const [pagination, setPagination] = useState({ page: 0, pageSize: 20 })

  // Searching, filtering and paging happen on the server; newest invoices come first
  const invoices = useQuery({
    queryKey: ['invoices', search, status, overdueOnly, pagination],
    queryFn: () =>
      api<Page<Invoice>>(
        `/invoices?${new URLSearchParams({
          search,
          ...(status && { status }),
          ...(overdueOnly && { overdue: 'true' }),
          page: String(pagination.page),
          size: String(pagination.pageSize),
        })}`,
      ),
    placeholderData: keepPreviousData,
  })

  const columns: GridColDef<Invoice>[] = [
    { field: 'invoiceNumber', headerName: t('invoices.number'), width: 140 },
    {
      field: 'recipientName',
      headerName: t('invoices.customer'),
      flex: 1.5,
      valueGetter: (_, row) => row.recipientCompany ?? row.recipientName,
    },
    { field: 'orderNumber', headerName: t('invoices.order'), width: 140 },
    {
      field: 'invoiceDate',
      headerName: t('invoices.date'),
      width: 120,
      valueFormatter: (value: string) => formatDate(value),
    },
    {
      field: 'dueDate',
      headerName: t('invoices.dueDate'),
      width: 120,
      valueFormatter: (value: string) => formatDate(value),
    },
    {
      field: 'status',
      headerName: t('invoices.status'),
      width: 140,
      renderCell: (params) => <InvoiceStatusChip invoice={params.row} />,
    },
    {
      field: 'grossTotal',
      headerName: t('invoices.amount'),
      width: 130,
      align: 'right',
      headerAlign: 'right',
      valueFormatter: (value: number) => formatCurrency(value),
    },
    {
      field: 'openAmount',
      headerName: t('invoices.open'),
      width: 130,
      align: 'right',
      headerAlign: 'right',
      valueFormatter: (value: number) => formatCurrency(value),
    },
  ]

  return (
    <>
      <Typography variant="h4" component="h1" sx={{ mb: 2 }}>
        {t('invoices.title')}
      </Typography>
      <Stack direction="row" spacing={2} sx={{ mb: 2, alignItems: 'center' }}>
        <TextField
          label={t('invoices.search')}
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
          label={t('invoices.status')}
          size="small"
          sx={{ width: 200 }}
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPagination({ ...pagination, page: 0 })
          }}
        >
          <MenuItem value="">{t('invoices.allStatuses')}</MenuItem>
          {INVOICE_STATUSES.map((option) => (
            <MenuItem key={option} value={option}>
              {t(`invoiceStatuses.${option}`)}
            </MenuItem>
          ))}
        </TextField>
        <FormControlLabel
          label={t('invoices.overdueOnly')}
          control={
            <Checkbox
              checked={overdueOnly}
              onChange={(event) => {
                setOverdueOnly(event.target.checked)
                setPagination({ ...pagination, page: 0 })
              }}
            />
          }
        />
      </Stack>
      {invoices.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      <Box sx={{ display: 'flex', flexDirection: 'column' }}>
        <DataGrid
          rows={invoices.data?.content ?? []}
          columns={columns}
          loading={invoices.isFetching}
          paginationMode="server"
          rowCount={invoices.data?.page.totalElements ?? 0}
          paginationModel={pagination}
          onPaginationModelChange={setPagination}
          pageSizeOptions={[10, 20, 50]}
          disableColumnSorting
          disableColumnMenu
          disableRowSelectionOnClick
          onRowClick={(params) => navigate(`/invoices/${params.id}`)}
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
