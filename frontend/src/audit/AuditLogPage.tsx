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
import { api, type Page } from '../api/client'
import { formatDateTime } from '../i18n/format'

const ACTIONS = ['CREATE', 'UPDATE', 'DELETE', 'EXPORT', 'ANONYMIZE'] as const

// The kinds of record the backend logs, by their technical name
const ENTITY_TYPES = [
  'Customer',
  'CustomerContact',
  'Quote',
  'SalesOrder',
  'Appointment',
  'SupplierOrder',
  'Invoice',
  'Payment',
  'Product',
  'Supplier',
  'User',
] as const

type AuditEntry = {
  id: number
  occurredAt: string
  // Null when the system itself made the change, e.g. demo data
  userName: string | null
  action: (typeof ACTIONS)[number]
  entityType: (typeof ENTITY_TYPES)[number]
  entityId: number
  // Names of the changed fields; values are never logged
  changedFields: string | null
}

export function AuditLogPage() {
  const { t, i18n } = useTranslation()
  const [action, setAction] = useState('')
  const [entityType, setEntityType] = useState('')
  const [pagination, setPagination] = useState({ page: 0, pageSize: 50 })

  // Filtering and paging happen on the server; newest entries come first
  const entries = useQuery({
    queryKey: ['auditLog', action, entityType, pagination],
    queryFn: () =>
      api<Page<AuditEntry>>(
        `/audit-log?${new URLSearchParams({
          ...(action && { action }),
          ...(entityType && { entityType }),
          page: String(pagination.page),
          size: String(pagination.pageSize),
        })}`,
      ),
    placeholderData: keepPreviousData,
  })

  const columns: GridColDef<AuditEntry>[] = [
    {
      field: 'occurredAt',
      headerName: t('audit.time'),
      width: 170,
      valueFormatter: (value: string) => formatDateTime(value),
    },
    {
      field: 'userName',
      headerName: t('audit.user'),
      flex: 1,
      valueFormatter: (value: string | null) => value ?? t('audit.system'),
    },
    {
      field: 'action',
      headerName: t('audit.action'),
      width: 150,
      valueFormatter: (value: string) => t(`auditActions.${value}`),
    },
    {
      field: 'entityType',
      headerName: t('audit.record'),
      flex: 1,
      valueGetter: (_, row) =>
        `${t(`auditEntities.${row.entityType}`)} #${row.entityId}`,
    },
    {
      field: 'changedFields',
      headerName: t('audit.changedFields'),
      flex: 2,
    },
  ]

  return (
    <>
      <Typography variant="h4" component="h1" sx={{ mb: 1 }}>
        {t('audit.title')}
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 2 }}>
        {t('audit.explanation')}
      </Typography>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField
          select
          label={t('audit.action')}
          size="small"
          sx={{ width: 220 }}
          value={action}
          onChange={(event) => {
            setAction(event.target.value)
            setPagination({ ...pagination, page: 0 })
          }}
        >
          <MenuItem value="">{t('audit.allActions')}</MenuItem>
          {ACTIONS.map((option) => (
            <MenuItem key={option} value={option}>
              {t(`auditActions.${option}`)}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          select
          label={t('audit.record')}
          size="small"
          sx={{ width: 220 }}
          value={entityType}
          onChange={(event) => {
            setEntityType(event.target.value)
            setPagination({ ...pagination, page: 0 })
          }}
        >
          <MenuItem value="">{t('audit.allRecords')}</MenuItem>
          {ENTITY_TYPES.map((option) => (
            <MenuItem key={option} value={option}>
              {t(`auditEntities.${option}`)}
            </MenuItem>
          ))}
        </TextField>
      </Stack>
      {entries.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      <Box sx={{ display: 'flex', flexDirection: 'column' }}>
        <DataGrid
          rows={entries.data?.content ?? []}
          columns={columns}
          loading={entries.isFetching}
          paginationMode="server"
          rowCount={entries.data?.page.totalElements ?? 0}
          paginationModel={pagination}
          onPaginationModelChange={setPagination}
          pageSizeOptions={[25, 50, 100]}
          disableColumnSorting
          disableColumnMenu
          disableRowSelectionOnClick
          localeText={
            i18n.language === 'de'
              ? deDE.components.MuiDataGrid.defaultProps.localeText
              : undefined
          }
        />
      </Box>
    </>
  )
}
