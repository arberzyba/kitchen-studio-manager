import EditIcon from '@mui/icons-material/Edit'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatDateTime } from '../i18n/format'
import { CustomerPrivacy } from './CustomerPrivacy'
import { canEditCustomers } from './permissions'
import {
  CONTACT_TYPES,
  type Address,
  type Contact,
  type Customer,
} from './types'

export function CustomerDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const { user } = useAuth()
  const customer = useQuery({
    queryKey: ['customer', id],
    queryFn: () => api<Customer>(`/customers/${id}`),
  })

  if (customer.isError) {
    return <Alert severity="error">{t('common.error')}</Alert>
  }
  if (!customer.data) {
    return null
  }
  const { salutation, firstName, lastName, companyName, email, phone } =
    customer.data

  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }}>
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'center' }}
      >
        <Typography variant="h4" component="h1">
          {salutation !== 'NONE' && `${t(`salutations.${salutation}`)} `}
          {firstName} {lastName}
        </Typography>
        {canEditCustomers(user) && !customer.data.anonymized && (
          <Button
            variant="outlined"
            startIcon={<EditIcon />}
            component={Link}
            to={`/customers/${id}/edit`}
          >
            {t('customers.edit')}
          </Button>
        )}
      </Stack>

      {customer.data.anonymized && (
        <Alert severity="info">{t('privacy.anonymizedNotice')}</Alert>
      )}

      <Paper sx={{ p: 3 }}>
        <Stack spacing={1.5}>
          {companyName && (
            <Detail label={t('customers.company')}>{companyName}</Detail>
          )}
          <Detail label={t('customers.email')}>{email ?? '–'}</Detail>
          <Detail label={t('customers.phone')}>{phone ?? '–'}</Detail>
          <Detail label={t('customers.billingAddress')}>
            <AddressLines address={customer.data.billingAddress} />
          </Detail>
          {customer.data.installationAddress && (
            <Detail label={t('customers.installationAddress')}>
              <AddressLines address={customer.data.installationAddress} />
            </Detail>
          )}
        </Stack>
      </Paper>

      {/* Erasing a customer's data removes the contact history and blocks new entries */}
      {!customer.data.anonymized && (
        <ContactHistory customerId={customer.data.id} />
      )}

      {/* Mirrors the backend rule: GDPR requests are handled by admins only */}
      {user?.role === 'ADMIN' && !customer.data.anonymized && (
        <CustomerPrivacy customer={customer.data} />
      )}
    </Stack>
  )
}

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography component="div">{children}</Typography>
    </div>
  )
}

function AddressLines({ address }: { address: Address }) {
  return (
    <>
      {address.street}
      <br />
      {address.postalCode} {address.city}
    </>
  )
}

function ContactHistory({ customerId }: { customerId: number }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [contactType, setContactType] = useState<Contact['contactType']>('CALL')
  const [summary, setSummary] = useState('')

  const contacts = useQuery({
    queryKey: ['customer', customerId, 'contacts'],
    queryFn: () => api<Contact[]>(`/customers/${customerId}/contacts`),
  })

  const add = useMutation({
    mutationFn: () =>
      api<Contact>(`/customers/${customerId}/contacts`, {
        method: 'POST',
        body: { contactType, summary },
      }),
    onSuccess: () => {
      setSummary('')
      queryClient.invalidateQueries({
        queryKey: ['customer', customerId, 'contacts'],
      })
    },
  })

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" component="h2" gutterBottom>
        {t('contacts.title')}
      </Typography>
      <Stack
        component="form"
        spacing={2}
        onSubmit={(event) => {
          event.preventDefault()
          add.mutate()
        }}
      >
        {add.isError && <Alert severity="error">{t('common.error')}</Alert>}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField
            select
            label={t('contacts.type')}
            sx={{ minWidth: 160 }}
            value={contactType}
            onChange={(event) =>
              setContactType(event.target.value as Contact['contactType'])
            }
          >
            {CONTACT_TYPES.map((type) => (
              <MenuItem key={type} value={type}>
                {t(`contactTypes.${type}`)}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            fullWidth
            multiline
            label={t('contacts.summary')}
            value={summary}
            onChange={(event) => setSummary(event.target.value)}
            slotProps={{ htmlInput: { maxLength: 2000 } }}
          />
        </Stack>
        <Button
          type="submit"
          variant="contained"
          sx={{ alignSelf: 'flex-end' }}
          disabled={!summary.trim()}
          loading={add.isPending}
        >
          {t('contacts.add')}
        </Button>
      </Stack>

      <Stack divider={<Divider />} spacing={2} sx={{ mt: 3 }}>
        {contacts.data?.length === 0 && (
          <Typography color="text.secondary">{t('contacts.empty')}</Typography>
        )}
        {contacts.data?.map((contact) => (
          <div key={contact.id}>
            <Stack
              direction="row"
              spacing={1}
              sx={{ alignItems: 'center', mb: 0.5 }}
            >
              <Chip
                size="small"
                label={t(`contactTypes.${contact.contactType}`)}
              />
              <Typography variant="caption" color="text.secondary">
                {formatDateTime(contact.createdAt)} · {contact.createdByName}
              </Typography>
            </Stack>
            <Typography sx={{ whiteSpace: 'pre-wrap' }}>
              {contact.summary}
            </Typography>
          </div>
        ))}
      </Stack>
    </Paper>
  )
}
