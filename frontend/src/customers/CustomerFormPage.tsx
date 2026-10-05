import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import FormControlLabel from '@mui/material/FormControlLabel'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Controller, get, useForm, type Path } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router'
import { z } from 'zod'
import { api } from '../api/client'
import { SALUTATIONS, type Customer } from './types'

// Error messages are translation keys, resolved when rendered
const required = z.string().trim().min(1, 'customers.required')

const address = z.object({
  street: required,
  postalCode: z.string().regex(/^\d{5}$/, 'customers.postalCodeInvalid'),
  city: required,
})

const schema = z.object({
  salutation: z.enum(SALUTATIONS),
  firstName: required,
  lastName: required,
  companyName: z.string(),
  email: z.union([z.literal(''), z.email('customers.emailInvalid')]),
  phone: z.string(),
  billingAddress: address,
  // Only validated when a separate installation address is entered
  installationAddress: z.object({
    street: z.string(),
    postalCode: z.string(),
    city: z.string(),
  }),
})

const schemaWithInstallationAddress = schema.extend({
  installationAddress: address,
})

type FormValues = z.infer<typeof schema>

// Handles both /customers/new and /customers/:id/edit
export function CustomerFormPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const customer = useQuery({
    queryKey: ['customer', id],
    queryFn: () => api<Customer>(`/customers/${id}`),
    enabled: !!id,
  })

  if (id && customer.isError) {
    return <Alert severity="error">{t('common.error')}</Alert>
  }
  if (id && !customer.data) {
    return null
  }
  return <CustomerForm customer={customer.data} />
}

function CustomerForm({ customer }: { customer?: Customer }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [hasInstallationAddress, setHasInstallationAddress] = useState(
    !!customer?.installationAddress,
  )
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(
      hasInstallationAddress ? schemaWithInstallationAddress : schema,
    ),
    defaultValues: {
      salutation: customer?.salutation ?? 'NONE',
      firstName: customer?.firstName ?? '',
      lastName: customer?.lastName ?? '',
      companyName: customer?.companyName ?? '',
      email: customer?.email ?? '',
      phone: customer?.phone ?? '',
      billingAddress: customer?.billingAddress ?? {
        street: '',
        postalCode: '',
        city: '',
      },
      installationAddress: customer?.installationAddress ?? {
        street: '',
        postalCode: '',
        city: '',
      },
    },
  })

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      api<Customer>(customer ? `/customers/${customer.id}` : '/customers', {
        method: customer ? 'PUT' : 'POST',
        body: {
          ...values,
          // The backend expects null, not an empty string, for fields left blank
          companyName: values.companyName || null,
          email: values.email || null,
          phone: values.phone || null,
          installationAddress: hasInstallationAddress
            ? values.installationAddress
            : null,
        },
      }),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.setQueryData(['customer', String(saved.id)], saved)
      navigate(`/customers/${saved.id}`)
    },
  })

  function field(name: Path<FormValues>, labelKey: string) {
    const message: string | undefined = get(errors, name)?.message
    return {
      label: t(labelKey),
      error: !!message,
      helperText: message && t(message),
      ...register(name),
    }
  }

  return (
    <>
      <Typography variant="h4" component="h1" sx={{ mb: 2 }}>
        {customer ? t('customers.edit') : t('customers.new')}
      </Typography>
      <Paper sx={{ p: 3, maxWidth: 720 }}>
        <Stack
          component="form"
          spacing={2}
          noValidate
          onSubmit={handleSubmit((values) => save.mutate(values))}
        >
          {save.isError && <Alert severity="error">{t('common.error')}</Alert>}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <Controller
              name="salutation"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  label={t('customers.salutation')}
                  sx={{ minWidth: 140 }}
                  {...field}
                >
                  {SALUTATIONS.map((salutation) => (
                    <MenuItem key={salutation} value={salutation}>
                      {t(`salutations.${salutation}`)}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
            <TextField
              fullWidth
              {...field('firstName', 'customers.firstName')}
            />
            <TextField fullWidth {...field('lastName', 'customers.lastName')} />
          </Stack>
          <TextField {...field('companyName', 'customers.companyOptional')} />
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              fullWidth
              type="email"
              {...field('email', 'customers.email')}
            />
            <TextField fullWidth {...field('phone', 'customers.phone')} />
          </Stack>

          <Typography variant="h6" component="h2">
            {t('customers.billingAddress')}
          </Typography>
          <TextField {...field('billingAddress.street', 'customers.street')} />
          <Stack direction="row" spacing={2}>
            <TextField
              sx={{ width: 160 }}
              {...field('billingAddress.postalCode', 'customers.postalCode')}
            />
            <TextField
              fullWidth
              {...field('billingAddress.city', 'customers.city')}
            />
          </Stack>

          <FormControlLabel
            label={t('customers.differentInstallationAddress')}
            control={
              <Checkbox
                checked={hasInstallationAddress}
                onChange={(event) =>
                  setHasInstallationAddress(event.target.checked)
                }
              />
            }
          />
          {hasInstallationAddress && (
            <>
              <Typography variant="h6" component="h2">
                {t('customers.installationAddress')}
              </Typography>
              <TextField
                {...field('installationAddress.street', 'customers.street')}
              />
              <Stack direction="row" spacing={2}>
                <TextField
                  sx={{ width: 160 }}
                  {...field(
                    'installationAddress.postalCode',
                    'customers.postalCode',
                  )}
                />
                <TextField
                  fullWidth
                  {...field('installationAddress.city', 'customers.city')}
                />
              </Stack>
            </>
          )}

          <Stack
            direction="row"
            spacing={1}
            sx={{ justifyContent: 'flex-end' }}
          >
            <Button
              component={Link}
              to={customer ? `/customers/${customer.id}` : '/customers'}
            >
              {t('common.cancel')}
            </Button>
            <Button type="submit" variant="contained" loading={save.isPending}>
              {t('common.save')}
            </Button>
          </Stack>
        </Stack>
      </Paper>
    </>
  )
}
