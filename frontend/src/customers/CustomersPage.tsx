import AddIcon from '@mui/icons-material/Add'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
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
import { canEditCustomers } from './permissions'
import type { Customer } from './types'

export function CustomersPage() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [pagination, setPagination] = useState({ page: 0, pageSize: 20 })

  // Searching and paging happen on the server; the previous page stays visible while the next one loads
  const customers = useQuery({
    queryKey: ['customers', search, pagination],
    queryFn: () =>
      api<Page<Customer>>(
        `/customers?${new URLSearchParams({
          search,
          page: String(pagination.page),
          size: String(pagination.pageSize),
        })}`,
      ),
    placeholderData: keepPreviousData,
  })

  const columns: GridColDef<Customer>[] = [
    {
      field: 'name',
      headerName: t('customers.name'),
      flex: 1,
      valueGetter: (_, row) => `${row.lastName}, ${row.firstName}`,
    },
    { field: 'companyName', headerName: t('customers.company'), flex: 1 },
    {
      field: 'city',
      headerName: t('customers.city'),
      flex: 1,
      valueGetter: (_, row) => row.billingAddress.city,
    },
    { field: 'email', headerName: t('customers.email'), flex: 1 },
    { field: 'phone', headerName: t('customers.phone'), flex: 1 },
  ]

  return (
    <>
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
      >
        <Typography variant="h4" component="h1">
          {t('customers.title')}
        </Typography>
        {canEditCustomers(user) && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            component={Link}
            to="/customers/new"
          >
            {t('customers.new')}
          </Button>
        )}
      </Stack>
      <TextField
        label={t('customers.search')}
        size="small"
        sx={{ mb: 2, width: 320 }}
        value={search}
        onChange={(event) => {
          setSearch(event.target.value)
          setPagination({ ...pagination, page: 0 })
        }}
      />
      {customers.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      <Box sx={{ display: 'flex', flexDirection: 'column' }}>
        <DataGrid
          rows={customers.data?.content ?? []}
          columns={columns}
          loading={customers.isFetching}
          paginationMode="server"
          rowCount={customers.data?.page.totalElements ?? 0}
          paginationModel={pagination}
          onPaginationModelChange={setPagination}
          pageSizeOptions={[10, 20, 50]}
          disableColumnSorting
          disableColumnMenu
          disableRowSelectionOnClick
          onRowClick={(params) => navigate(`/customers/${params.id}`)}
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
