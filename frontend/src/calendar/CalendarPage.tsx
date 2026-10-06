import deLocale from '@fullcalendar/core/locales/de'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import AddIcon from '@mui/icons-material/Add'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import {
  AppointmentDetailsDialog,
  AppointmentDialog,
} from './AppointmentDialog'
import {
  APPOINTMENT_COLORS,
  APPOINTMENT_TYPES,
  type Appointment,
} from './types'

const TIME_FORMAT = {
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
} as const

type DialogState =
  { appointment: Appointment } | { start?: Date; end?: Date } | undefined

export function CalendarPage() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  // Mirrors the backend rule: installers only read the appointments assigned to them
  const canPlan = user?.role !== 'INSTALLER'
  // The visible period, reported by the calendar whenever the user navigates
  const [period, setPeriod] = useState<{ from: string; to: string }>()
  const [dialog, setDialog] = useState<DialogState>()

  const appointments = useQuery({
    queryKey: ['appointments', period],
    queryFn: () =>
      api<Appointment[]>(
        `/appointments?${new URLSearchParams({ from: period!.from, to: period!.to })}`,
      ),
    enabled: !!period,
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
      >
        <Typography variant="h4" component="h1">
          {canPlan ? t('calendar.title') : t('calendar.myJobs')}
        </Typography>
        {canPlan && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setDialog({})}
          >
            {t('calendar.new')}
          </Button>
        )}
      </Stack>
      <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
        {APPOINTMENT_TYPES.map((type) => (
          <Chip
            key={type}
            size="small"
            label={t(`appointmentTypes.${type}`)}
            sx={{ bgcolor: APPOINTMENT_COLORS[type], color: 'common.white' }}
          />
        ))}
      </Stack>
      {appointments.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('common.error')}
        </Alert>
      )}
      <Paper sx={{ p: 2 }}>
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          initialView="timeGridWeek"
          headerToolbar={{
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay',
          }}
          locale={i18n.language === 'de' ? deLocale : 'en-gb'}
          firstDay={1}
          height="auto"
          allDaySlot={false}
          slotMinTime="06:00"
          slotMaxTime="20:00"
          slotLabelFormat={TIME_FORMAT}
          eventTimeFormat={TIME_FORMAT}
          nowIndicator
          selectable={canPlan}
          datesSet={(dates) =>
            setPeriod({
              from: dates.start.toISOString(),
              to: dates.end.toISOString(),
            })
          }
          select={(selection) =>
            setDialog({ start: selection.start, end: selection.end })
          }
          eventClick={(click) =>
            setDialog({ appointment: click.event.extendedProps as Appointment })
          }
          events={appointments.data?.map((appointment) => ({
            id: String(appointment.id),
            title: `${t(`appointmentTypes.${appointment.type}`)} · ${appointment.customerName}`,
            start: appointment.startTime,
            end: appointment.endTime,
            color: APPOINTMENT_COLORS[appointment.type],
            extendedProps: appointment,
          }))}
        />
      </Paper>
      {dialog &&
        ('appointment' in dialog && !canPlan ? (
          <AppointmentDetailsDialog
            appointment={dialog.appointment}
            onClose={() => setDialog(undefined)}
          />
        ) : (
          <AppointmentDialog
            appointment={
              'appointment' in dialog ? dialog.appointment : undefined
            }
            initialStart={'start' in dialog ? dialog.start : undefined}
            initialEnd={'end' in dialog ? dialog.end : undefined}
            onClose={() => setDialog(undefined)}
          />
        ))}
    </>
  )
}
