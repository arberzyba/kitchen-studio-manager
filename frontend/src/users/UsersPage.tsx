import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
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
import { api } from '../api/client'
import type { User } from './types'
import { UserDialog } from './UserDialog'

export function UsersPage() {
  const users = useQuery({
    queryKey: ['users'],
    queryFn: () => api<User[]>('/users'),
  })
  // undefined = dialog closed, 'new' = creating, otherwise the user being edited
  const [dialog, setDialog] = useState<User | 'new'>()

  return (
    <>
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
      >
        <Typography variant="h4" component="h1">
          Users
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setDialog('new')}
        >
          New user
        </Button>
      </Stack>
      {users.isError && <Alert severity="error">{users.error.message}</Alert>}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Role</TableCell>
              <TableCell>Status</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {users.data?.map((user) => (
              <TableRow key={user.id}>
                <TableCell>
                  {user.firstName} {user.lastName}
                </TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{user.role}</TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    label={user.active ? 'Active' : 'Inactive'}
                    color={user.active ? 'success' : 'default'}
                  />
                </TableCell>
                <TableCell align="right">
                  <IconButton
                    aria-label={`Edit ${user.email}`}
                    onClick={() => setDialog(user)}
                  >
                    <EditIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {dialog && (
        <UserDialog
          user={dialog === 'new' ? undefined : dialog}
          onClose={() => setDialog(undefined)}
        />
      )}
    </>
  )
}
