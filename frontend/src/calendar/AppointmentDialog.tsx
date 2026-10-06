import { zodResolver } from '@hookform/resolvers/zod'
import Alert from '@mui/material/Alert'
import Autocomplete from '@mui/material/Autocomplete'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { api, type Page } from '../api/client'
import { formatDateTime } from '../i18n/format'
import type { OrderSummary } from '../orders/types'
import {
  APPOINTMENT_TYPES,
  type Appointment,
  type AssignableUser,
} from './types'

// Error messages are translation keys, resolved when rendered
const schema = z
  .object({
    orderId: z.number('calendar.orderRequired'),
    type: z.enum(APPOINTMENT_TYPES),
    // Local date and time as produced by <input type="datetime-local">
    start: z.string().min(1, 'calendar.timeRequired'),
    end: z.string().min(1, 'calendar.timeRequired'),
    assigneeId: z.number('calendar.assigneeRequired'),
    notes: z.string().max(1000),
  })
  .refine(
    (values) => !values.start || !values.end || values.end > values.start,
    {
      path: ['end'],
      message: 'calendar.endBeforeStart',
    },
  )

type FormValues = z.infer<typeof schema>

type OrderOption = { id: number; label: string }

// Formats a date as the local "2026-10-06T13:00" that datetime-local inputs use
function toLocalInput(date: Date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}

// What the assigned employee needs on site
function JobDetails({ appointment }: { appointment: Appointment }) {
  const { t } = useTranslation()
  const { address } = appointment
  return (
    <Stack spacing={1.5}>
      <Detail label={t('calendar.customer')}>
        {appointment.customerName}
        {appointment.customerPhone && ` · ${appointment.customerPhone}`}
      </Detail>
      <Detail label={t('calendar.address')}>
        {address.street}, {address.postalCode} {address.city}
      </Detail>
      <Detail label={t('calendar.order')}>{appointment.orderNumber}</Detail>
    </Stack>
  )
}

function Detail({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography>{children}</Typography>
    </div>
  )
}

// Read-only view for installers, who cannot change appointments
export function AppointmentDetailsDialog({
  appointment,
  onClose,
}: {
  appointment: Appointment
  onClose: () => void
}) {
  const { t } = useTranslation()
  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{t(`appointmentTypes.${appointment.type}`)}</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5}>
          <Detail label={t('calendar.time')}>
            {formatDateTime(appointment.startTime)} –{' '}
            {formatDateTime(appointment.endTime)}
          </Detail>
          <JobDetails appointment={appointment} />
          {appointment.notes && (
            <Detail label={t('calendar.notes')}>{appointment.notes}</Detail>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.close')}</Button>
      </DialogActions>
    </Dialog>
  )
}

// Pass an appointment to edit it, or a start and end time to plan a new one
export function AppointmentDialog({
  appointment,
  initialStart,
  initialEnd,
  onClose,
}: {
  appointment?: Appointment
  initialStart?: Date
  initialEnd?: Date
  onClose: () => void
}) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [order, setOrder] = useState<OrderOption | null>(
    appointment
      ? {
          id: appointment.orderId,
          label: `${appointment.orderNumber} – ${appointment.customerName}`,
        }
      : null,
  )
  const [orderSearch, setOrderSearch] = useState('')
  const [confirmDelete, setConfirmDelete] = useState(false)

  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      orderId: appointment?.orderId,
      type: appointment?.type ?? 'MEASUREMENT',
      start: appointment
        ? toLocalInput(new Date(appointment.startTime))
        : initialStart
          ? toLocalInput(initialStart)
          : '',
      end: appointment
        ? toLocalInput(new Date(appointment.endTime))
        : initialEnd
          ? toLocalInput(initialEnd)
          : '',
      assigneeId: appointment?.assigneeId,
      notes: appointment?.notes ?? '',
    },
  })

  const orders = useQuery({
    queryKey: ['orders', orderSearch, 'options'],
    queryFn: () =>
      api<Page<OrderSummary>>(
        `/orders?${new URLSearchParams({ search: orderSearch, size: '10' })}`,
      ),
    placeholderData: keepPreviousData,
  })
  const employees = useQuery({
    queryKey: ['users', 'assignable'],
    queryFn: () => api<AssignableUser[]>('/users/assignable'),
  })

  function onDone() {
    queryClient.invalidateQueries({ queryKey: ['appointments'] })
    onClose()
  }

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      api<Appointment>(
        appointment ? `/appointments/${appointment.id}` : '/appointments',
        {
          method: appointment ? 'PUT' : 'POST',
          body: {
            orderId: values.orderId,
            type: values.type,
            // The inputs hold local time; the backend stores a point in time
            startTime: new Date(values.start).toISOString(),
            endTime: new Date(values.end).toISOString(),
            assigneeId: values.assigneeId,
            notes: values.notes || null,
          },
        },
      ),
    onSuccess: onDone,
  })
  const remove = useMutation({
    mutationFn: () =>
      api<void>(`/appointments/${appointment?.id}`, { method: 'DELETE' }),
    onSuccess: onDone,
  })

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <form noValidate onSubmit={handleSubmit((values) => save.mutate(values))}>
        <DialogTitle>
          {appointment ? t('calendar.edit') : t('calendar.new')}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {(save.isError || remove.isError) && (
              <Alert severity="error">{t('common.error')}</Alert>
            )}
            <Autocomplete
              options={
                orders.data?.content.map((option) => ({
                  id: option.id,
                  label: `${option.orderNumber} – ${option.customerName}`,
                })) ?? []
              }
              // The server already filtered by the typed text
              filterOptions={(options) => options}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              value={order}
              onChange={(_, option) => {
                setOrder(option)
                setValue('orderId', option?.id as number, {
                  shouldValidate: true,
                })
              }}
              onInputChange={(_, value, reason) => {
                if (reason === 'input') {
                  setOrderSearch(value)
                }
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label={t('calendar.order')}
                  error={!!errors.orderId}
                  helperText={
                    errors.orderId?.message && t(errors.orderId.message)
                  }
                />
              )}
            />
            {appointment && <JobDetails appointment={appointment} />}
            <Controller
              name="type"
              control={control}
              render={({ field }) => (
                <TextField select label={t('calendar.type')} {...field}>
                  {APPOINTMENT_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {t(`appointmentTypes.${type}`)}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                fullWidth
                type="datetime-local"
                label={t('calendar.start')}
                slotProps={{ inputLabel: { shrink: true } }}
                error={!!errors.start}
                helperText={errors.start?.message && t(errors.start.message)}
                {...register('start')}
              />
              <TextField
                fullWidth
                type="datetime-local"
                label={t('calendar.end')}
                slotProps={{ inputLabel: { shrink: true } }}
                error={!!errors.end}
                helperText={errors.end?.message && t(errors.end.message)}
                {...register('end')}
              />
            </Stack>
            <Controller
              name="assigneeId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  label={t('calendar.assignee')}
                  error={!!errors.assigneeId}
                  helperText={
                    errors.assigneeId?.message && t(errors.assigneeId.message)
                  }
                  {...field}
                  value={field.value ?? ''}
                >
                  {employees.data?.map((employee) => (
                    <MenuItem key={employee.id} value={employee.id}>
                      {employee.firstName} {employee.lastName} (
                      {t(`roles.${employee.role}`)})
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
            <TextField
              multiline
              minRows={2}
              label={t('calendar.notes')}
              {...register('notes')}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          {appointment &&
            (confirmDelete ? (
              <Button
                color="error"
                variant="contained"
                loading={remove.isPending}
                onClick={() => remove.mutate()}
              >
                {t('calendar.confirmDelete')}
              </Button>
            ) : (
              <Button color="error" onClick={() => setConfirmDelete(true)}>
                {t('calendar.delete')}
              </Button>
            ))}
          <Stack sx={{ flexGrow: 1 }} />
          <Button onClick={onClose}>{t('common.cancel')}</Button>
          <Button type="submit" variant="contained" loading={save.isPending}>
            {t('common.save')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
