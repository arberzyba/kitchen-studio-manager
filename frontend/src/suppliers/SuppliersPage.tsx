import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { SupplierDialog } from './SupplierDialog'
import type { Supplier } from './types'

export function SuppliersPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  // Mirrors the backend rule: only admins maintain suppliers
  const canEdit = user?.role === 'ADMIN'
  const suppliers = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => api<Supplier[]>('/suppliers'),
  })
  // undefined = dialog closed, 'new' = creating, otherwise the supplier being edited
  const [dialog, setDialog] = useState<Supplier | 'new'>()

  return (
    <>
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
      >
        <Typography variant="h4" component="h1">
          {t('suppliers.title')}
        </Typography>
        {canEdit && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setDialog('new')}
          >
            {t('suppliers.new')}
          </Button>
        )}
      </Stack>
      {suppliers.isError && <Alert severity="error">{t('common.error')}</Alert>}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>{t('suppliers.name')}</TableCell>
              <TableCell>{t('suppliers.email')}</TableCell>
              <TableCell>{t('suppliers.phone')}</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {suppliers.data?.map((supplier) => (
              <TableRow key={supplier.id}>
                <TableCell>{supplier.name}</TableCell>
                <TableCell>{supplier.email}</TableCell>
                <TableCell>{supplier.phone}</TableCell>
                <TableCell align="right">
                  {canEdit && (
                    <IconButton
                      aria-label={t('suppliers.editSupplier', {
                        name: supplier.name,
                      })}
                      onClick={() => setDialog(supplier)}
                    >
                      <EditIcon />
                    </IconButton>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {dialog && (
        <SupplierDialog
          supplier={dialog === 'new' ? undefined : dialog}
          onClose={() => setDialog(undefined)}
        />
      )}
    </>
  )
}
