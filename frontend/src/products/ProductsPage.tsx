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
import { api, type Page } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatCurrency } from '../i18n/format'
import { ProductDialog } from './ProductDialog'
import { PRODUCT_CATEGORIES, type Product } from './types'

export function ProductsPage() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  // Mirrors the backend rule: only admins maintain the catalog
  const canEdit = user?.role === 'ADMIN'
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [pagination, setPagination] = useState({ page: 0, pageSize: 20 })
  // undefined = dialog closed, 'new' = creating, otherwise the product being edited
  const [dialog, setDialog] = useState<Product | 'new'>()

  // Searching, filtering and paging happen on the server
  const products = useQuery({
    queryKey: ['products', search, category, pagination],
    queryFn: () =>
      api<Page<Product>>(
        `/products?${new URLSearchParams({
          search,
          ...(category && { category }),
          page: String(pagination.page),
          size: String(pagination.pageSize),
        })}`,
      ),
    placeholderData: keepPreviousData,
  })

  const columns: GridColDef<Product>[] = [
    { field: 'sku', headerName: t('products.sku'), width: 130 },
    { field: 'name', headerName: t('products.name'), flex: 2 },
    {
      field: 'category',
      headerName: t('products.category'),
      flex: 1,
      valueFormatter: (value: string) => t(`productCategories.${value}`),
    },
    { field: 'supplierName', headerName: t('products.supplier'), flex: 1.5 },
    {
      field: 'purchasePrice',
      headerName: t('products.purchasePrice'),
      width: 150,
      align: 'right',
      headerAlign: 'right',
      valueFormatter: (value: number) => formatCurrency(value),
    },
    {
      field: 'sellingPrice',
      headerName: t('products.sellingPrice'),
      width: 170,
      align: 'right',
      headerAlign: 'right',
      valueFormatter: (value: number, row) =>
        `${formatCurrency(value)} / ${t(`productUnitsShort.${row.unit}`)}`,
    },
    {
      field: 'active',
      headerName: t('products.status'),
      width: 110,
      valueFormatter: (value) =>
        value ? t('products.active') : t('products.inactive'),
    },
  ]

  return (
    <>
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
      >
        <Typography variant="h4" component="h1">
          {t('products.title')}
        </Typography>
        {canEdit && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setDialog('new')}
          >
            {t('products.new')}
          </Button>
        )}
      </Stack>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField
          label={t('products.search')}
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
          label={t('products.category')}
          size="small"
          sx={{ width: 200 }}
          value={category}
          onChange={(event) => {
            setCategory(event.target.value)
            setPagination({ ...pagination, page: 0 })
          }}
        >
          <MenuItem value="">{t('products.allCategories')}</MenuItem>
          {PRODUCT_CATEGORIES.map((option) => (
            <MenuItem key={option} value={option}>
              {t(`productCategories.${option}`)}
            </MenuItem>
          ))}
        </TextField>
      </Stack>
      {products.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      <Box sx={{ display: 'flex', flexDirection: 'column' }}>
        <DataGrid
          rows={products.data?.content ?? []}
          columns={columns}
          loading={products.isFetching}
          paginationMode="server"
          rowCount={products.data?.page.totalElements ?? 0}
          paginationModel={pagination}
          onPaginationModelChange={setPagination}
          pageSizeOptions={[10, 20, 50]}
          disableColumnSorting
          disableColumnMenu
          disableRowSelectionOnClick
          onRowClick={canEdit ? (params) => setDialog(params.row) : undefined}
          localeText={
            i18n.language === 'de'
              ? deDE.components.MuiDataGrid.defaultProps.localeText
              : undefined
          }
          sx={canEdit ? { '& .MuiDataGrid-row': { cursor: 'pointer' } } : {}}
        />
      </Box>
      {dialog && (
        <ProductDialog
          product={dialog === 'new' ? undefined : dialog}
          onClose={() => setDialog(undefined)}
        />
      )}
    </>
  )
}
