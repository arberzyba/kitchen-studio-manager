import { zodResolver } from '@hookform/resolvers/zod'
import DeleteIcon from '@mui/icons-material/Delete'
import Alert from '@mui/material/Alert'
import Autocomplete from '@mui/material/Autocomplete'
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
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { useState } from 'react'
import { useFieldArray, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, Navigate, useNavigate, useParams } from 'react-router'
import { z } from 'zod'
import { api, type Page } from '../api/client'
import type { Customer } from '../customers/types'
import { formatCurrency } from '../i18n/format'
import { PRODUCT_UNITS, type Product } from '../products/types'
import { lineTotal, totals, VAT_RATE } from './calculation'
import { QuoteTotals } from './QuoteParts'
import type { Quote } from './types'

// Error messages are translation keys, resolved when rendered
const percent = z
  .number('quotes.percentInvalid')
  .min(0, 'quotes.percentInvalid')
  .max(100, 'quotes.percentInvalid')

const schema = z.object({
  customerId: z.number('quotes.customerRequired'),
  validUntil: z.string().min(1, 'quotes.dateRequired'),
  discountPercent: percent,
  notes: z.string().max(2000),
  items: z
    .array(
      z.object({
        productId: z.number(),
        // Shown in the row; the backend takes these from the product itself
        sku: z.string(),
        description: z.string(),
        unit: z.enum(PRODUCT_UNITS),
        quantity: z
          .number('quotes.quantityInvalid')
          .min(0.01, 'quotes.quantityInvalid'),
        unitPrice: z
          .number('quotes.priceInvalid')
          .min(0, 'quotes.priceInvalid'),
        discountPercent: percent,
      }),
    )
    .min(1, 'quotes.itemsRequired'),
})

type FormValues = z.infer<typeof schema>

type CustomerOption = { id: number; label: string }

// Quotes are valid for 30 days unless changed
function defaultValidUntil() {
  const date = new Date()
  date.setDate(date.getDate() + 30)
  return date.toISOString().slice(0, 10)
}

// Handles both /quotes/new and /quotes/:id/edit
export function QuoteFormPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const quote = useQuery({
    queryKey: ['quote', id],
    queryFn: () => api<Quote>(`/quotes/${id}`),
    enabled: !!id,
  })

  if (id && quote.isError) {
    return <Alert severity="error">{t('common.error')}</Alert>
  }
  if (id && !quote.data) {
    return null
  }
  // Only drafts can be edited
  if (quote.data && quote.data.status !== 'DRAFT') {
    return <Navigate to={`/quotes/${id}`} replace />
  }
  return <QuoteForm quote={quote.data} />
}

function QuoteForm({ quote }: { quote?: Quote }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [customer, setCustomer] = useState<CustomerOption | null>(
    quote ? { id: quote.customerId, label: quote.customerName } : null,
  )
  const [customerSearch, setCustomerSearch] = useState('')
  const [productSearch, setProductSearch] = useState('')

  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      customerId: quote?.customerId,
      validUntil: quote?.validUntil ?? defaultValidUntil(),
      discountPercent: quote?.discountPercent ?? 0,
      notes: quote?.notes ?? '',
      items: quote?.items ?? [],
    },
  })
  const items = useFieldArray({ control, name: 'items' })
  const watchedItems = useWatch({ control, name: 'items' })
  const watchedDiscount = useWatch({ control, name: 'discountPercent' })

  const customers = useQuery({
    queryKey: ['customers', customerSearch, 'options'],
    queryFn: () =>
      api<Page<Customer>>(
        `/customers?${new URLSearchParams({ search: customerSearch, size: '10' })}`,
      ),
    placeholderData: keepPreviousData,
  })
  const products = useQuery({
    queryKey: ['products', productSearch, 'options'],
    queryFn: () =>
      api<Page<Product>>(
        `/products?${new URLSearchParams({ search: productSearch, size: '20' })}`,
      ),
    placeholderData: keepPreviousData,
  })

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      api<Quote>(quote ? `/quotes/${quote.id}` : '/quotes', {
        method: quote ? 'PUT' : 'POST',
        body: {
          customerId: values.customerId,
          validUntil: values.validUntil,
          discountPercent: values.discountPercent,
          notes: values.notes || null,
          items: values.items.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
            unitPrice: item.unitPrice,
            discountPercent: item.discountPercent,
          })),
        },
      }),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['quotes'] })
      queryClient.setQueryData(['quote', String(saved.id)], saved)
      navigate(`/quotes/${saved.id}`)
    },
  })

  // Fields that are still empty or invalid count as 0 in the preview
  const lineTotals = watchedItems.map((item) =>
    lineTotal(
      item.quantity || 0,
      item.unitPrice || 0,
      item.discountPercent || 0,
    ),
  )
  const numberInput = { htmlInput: { step: '0.01', min: '0' } }

  return (
    <>
      <Typography variant="h4" component="h1" sx={{ mb: 2 }}>
        {quote
          ? t('quotes.editTitle', { number: quote.quoteNumber })
          : t('quotes.new')}
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Stack
          component="form"
          spacing={3}
          noValidate
          onSubmit={handleSubmit((values) => save.mutate(values))}
        >
          {save.isError && <Alert severity="error">{t('common.error')}</Alert>}
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
            <Autocomplete
              sx={{ flex: 2 }}
              options={
                customers.data?.content.map((option) => ({
                  id: option.id,
                  label: `${option.firstName} ${option.lastName}`,
                })) ?? []
              }
              // The server already filtered by the typed text
              filterOptions={(options) => options}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              value={customer}
              onChange={(_, option) => {
                setCustomer(option)
                setValue('customerId', option?.id as number, {
                  shouldValidate: true,
                })
              }}
              onInputChange={(_, value, reason) => {
                if (reason === 'input') {
                  setCustomerSearch(value)
                }
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label={t('quotes.customer')}
                  error={!!errors.customerId}
                  helperText={
                    errors.customerId?.message && t(errors.customerId.message)
                  }
                />
              )}
            />
            <TextField
              sx={{ flex: 1 }}
              type="date"
              label={t('quotes.validUntil')}
              slotProps={{ inputLabel: { shrink: true } }}
              error={!!errors.validUntil}
              helperText={
                errors.validUntil?.message && t(errors.validUntil.message)
              }
              {...register('validUntil')}
            />
          </Stack>

          <div>
            <Typography variant="h6" component="h2" gutterBottom>
              {t('quotes.items')}
            </Typography>
            <Autocomplete
              // Always empty: choosing a product adds a line and clears the field again
              value={null}
              inputValue={productSearch}
              options={products.data?.content.filter((p) => p.active) ?? []}
              filterOptions={(options) => options}
              getOptionLabel={(option) => `${option.sku} – ${option.name}`}
              onInputChange={(_, value, reason) => {
                if (reason !== 'reset') {
                  setProductSearch(value)
                }
              }}
              onChange={(_, product) => {
                if (product) {
                  items.append({
                    productId: product.id,
                    sku: product.sku,
                    description: product.name,
                    unit: product.unit,
                    quantity: 1,
                    unitPrice: product.sellingPrice,
                    discountPercent: 0,
                  })
                  setProductSearch('')
                }
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label={t('quotes.addProduct')}
                  error={!!errors.items?.root || !!errors.items?.message}
                  helperText={
                    (errors.items?.root?.message ?? errors.items?.message) &&
                    t(
                      (errors.items?.root?.message ??
                        errors.items?.message) as string,
                    )
                  }
                />
              )}
            />
            {items.fields.length > 0 && (
              <TableContainer sx={{ mt: 2 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>{t('quotes.product')}</TableCell>
                      <TableCell>{t('quotes.quantity')}</TableCell>
                      <TableCell>{t('quotes.unitPrice')}</TableCell>
                      <TableCell>{t('quotes.discount')}</TableCell>
                      <TableCell align="right">
                        {t('quotes.lineTotal')}
                      </TableCell>
                      <TableCell />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {items.fields.map((field, index) => {
                      const rowErrors = errors.items?.[index]
                      return (
                        <TableRow key={field.id}>
                          <TableCell>
                            <Typography variant="body2">
                              {field.description}
                            </Typography>
                            <Typography
                              variant="caption"
                              color="text.secondary"
                            >
                              {field.sku}
                            </Typography>
                          </TableCell>
                          <TableCell sx={{ width: 150 }}>
                            <TextField
                              size="small"
                              type="number"
                              slotProps={{
                                ...numberInput,
                                input: {
                                  endAdornment: t(
                                    `productUnitsShort.${field.unit}`,
                                  ),
                                },
                              }}
                              aria-label={t('quotes.quantity')}
                              error={!!rowErrors?.quantity}
                              helperText={
                                rowErrors?.quantity?.message &&
                                t(rowErrors.quantity.message)
                              }
                              {...register(`items.${index}.quantity`, {
                                valueAsNumber: true,
                              })}
                            />
                          </TableCell>
                          <TableCell sx={{ width: 150 }}>
                            <TextField
                              size="small"
                              type="number"
                              slotProps={{
                                ...numberInput,
                                input: { endAdornment: '€' },
                              }}
                              error={!!rowErrors?.unitPrice}
                              helperText={
                                rowErrors?.unitPrice?.message &&
                                t(rowErrors.unitPrice.message)
                              }
                              {...register(`items.${index}.unitPrice`, {
                                valueAsNumber: true,
                              })}
                            />
                          </TableCell>
                          <TableCell sx={{ width: 130 }}>
                            <TextField
                              size="small"
                              type="number"
                              slotProps={{
                                ...numberInput,
                                input: { endAdornment: '%' },
                              }}
                              error={!!rowErrors?.discountPercent}
                              helperText={
                                rowErrors?.discountPercent?.message &&
                                t(rowErrors.discountPercent.message)
                              }
                              {...register(`items.${index}.discountPercent`, {
                                valueAsNumber: true,
                              })}
                            />
                          </TableCell>
                          <TableCell align="right">
                            {formatCurrency(lineTotals[index] ?? 0)}
                          </TableCell>
                          <TableCell align="right">
                            <IconButton
                              aria-label={t('quotes.removeItem', {
                                name: field.description,
                              })}
                              onClick={() => items.remove(index)}
                            >
                              <DeleteIcon />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </div>

          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={3}
            sx={{ justifyContent: 'space-between' }}
          >
            <Stack spacing={2} sx={{ flex: 1, maxWidth: 480 }}>
              <TextField
                type="number"
                label={t('quotes.overallDiscount')}
                sx={{ width: 200 }}
                slotProps={{ ...numberInput, input: { endAdornment: '%' } }}
                error={!!errors.discountPercent}
                helperText={
                  errors.discountPercent?.message &&
                  t(errors.discountPercent.message)
                }
                {...register('discountPercent', { valueAsNumber: true })}
              />
              <TextField
                multiline
                minRows={2}
                label={t('quotes.notes')}
                helperText={t('quotes.notesHint')}
                {...register('notes')}
              />
            </Stack>
            <QuoteTotals
              totals={totals(lineTotals, watchedDiscount || 0)}
              discountPercent={watchedDiscount || 0}
              vatRate={quote?.vatRate ?? VAT_RATE}
            />
          </Stack>

          <Stack
            direction="row"
            spacing={1}
            sx={{ justifyContent: 'flex-end' }}
          >
            <Button
              component={Link}
              to={quote ? `/quotes/${quote.id}` : '/quotes'}
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
