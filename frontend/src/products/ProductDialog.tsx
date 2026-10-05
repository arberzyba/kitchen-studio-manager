import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControlLabel from '@mui/material/FormControlLabel'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { api, ApiError } from '../api/client'
import type { Supplier } from '../suppliers/types'
import { PRODUCT_CATEGORIES, PRODUCT_UNITS, type Product } from './types'

// Error messages are translation keys, resolved when rendered
const price = z.number('products.priceInvalid').min(0, 'products.priceInvalid')

const schema = z.object({
  sku: z.string().trim().min(1, 'products.required').max(50),
  name: z.string().trim().min(1, 'products.required'),
  description: z.string().max(1000),
  category: z.enum(PRODUCT_CATEGORIES),
  unit: z.enum(PRODUCT_UNITS),
  purchasePrice: price,
  sellingPrice: price,
  supplierId: z.number('products.supplierRequired'),
  active: z.boolean(),
})

type FormValues = z.infer<typeof schema>

// Pass a product to edit it, or none to create a new one
export function ProductDialog({
  product,
  onClose,
}: {
  product?: Product
  onClose: () => void
}) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const suppliers = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => api<Supplier[]>('/suppliers'),
  })
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      sku: product?.sku ?? '',
      name: product?.name ?? '',
      description: product?.description ?? '',
      category: product?.category ?? 'CABINET',
      unit: product?.unit ?? 'PIECE',
      purchasePrice: product?.purchasePrice,
      sellingPrice: product?.sellingPrice,
      supplierId: product?.supplierId,
      active: product?.active ?? true,
    },
  })

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      api<Product>(product ? `/products/${product.id}` : '/products', {
        method: product ? 'PUT' : 'POST',
        body: { ...values, description: values.description || null },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
      onClose()
    },
  })

  const skuTaken = save.error instanceof ApiError && save.error.status === 409
  const priceInput = { htmlInput: { step: '0.01', min: '0' } }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <form noValidate onSubmit={handleSubmit((values) => save.mutate(values))}>
        <DialogTitle>
          {product ? t('products.edit') : t('products.new')}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {save.isError && (
              <Alert severity="error">
                {t(skuTaken ? 'products.skuTaken' : 'common.error')}
              </Alert>
            )}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label={t('products.sku')}
                sx={{ minWidth: 180 }}
                error={!!errors.sku}
                helperText={errors.sku?.message && t(errors.sku.message)}
                {...register('sku')}
              />
              <TextField
                fullWidth
                label={t('products.name')}
                error={!!errors.name}
                helperText={errors.name?.message && t(errors.name.message)}
                {...register('name')}
              />
            </Stack>
            <TextField
              multiline
              label={t('products.descriptionOptional')}
              {...register('description')}
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <Controller
                name="category"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    fullWidth
                    label={t('products.category')}
                    {...field}
                  >
                    {PRODUCT_CATEGORIES.map((category) => (
                      <MenuItem key={category} value={category}>
                        {t(`productCategories.${category}`)}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
              <Controller
                name="unit"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    fullWidth
                    label={t('products.unit')}
                    {...field}
                  >
                    {PRODUCT_UNITS.map((unit) => (
                      <MenuItem key={unit} value={unit}>
                        {t(`productUnits.${unit}`)}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                fullWidth
                type="number"
                label={t('products.purchasePrice')}
                slotProps={priceInput}
                error={!!errors.purchasePrice}
                helperText={
                  errors.purchasePrice?.message &&
                  t(errors.purchasePrice.message)
                }
                {...register('purchasePrice', { valueAsNumber: true })}
              />
              <TextField
                fullWidth
                type="number"
                label={t('products.sellingPrice')}
                slotProps={priceInput}
                error={!!errors.sellingPrice}
                helperText={
                  errors.sellingPrice?.message && t(errors.sellingPrice.message)
                }
                {...register('sellingPrice', { valueAsNumber: true })}
              />
            </Stack>
            <Controller
              name="supplierId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  label={t('products.supplier')}
                  error={!!errors.supplierId}
                  helperText={
                    errors.supplierId?.message && t(errors.supplierId.message)
                  }
                  {...field}
                  value={field.value ?? ''}
                >
                  {suppliers.data?.map((supplier) => (
                    <MenuItem key={supplier.id} value={supplier.id}>
                      {supplier.name}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
            {product && (
              <Controller
                name="active"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    label={t('products.active')}
                    control={
                      <Switch
                        checked={field.value}
                        onChange={(event) =>
                          field.onChange(event.target.checked)
                        }
                      />
                    }
                  />
                )}
              />
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>{t('common.cancel')}</Button>
          <Button type="submit" variant="contained" loading={save.isPending}>
            {t('common.save')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
