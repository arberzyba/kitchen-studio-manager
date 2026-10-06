package de.sedzkitchens.appointment;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.NotFoundException;
import de.sedzkitchens.order.OrderRepository;
import de.sedzkitchens.user.User;
import de.sedzkitchens.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

	private final AppointmentRepository appointmentRepository;

	private final OrderRepository orderRepository;

	private final UserRepository userRepository;

	public List<AppointmentResponse> findInPeriod(Instant from, Instant to, Long assigneeId) {
		return appointmentRepository.findInPeriod(from, to, assigneeId)
			.stream()
			.map(AppointmentResponse::from)
			.toList();
	}

	@Transactional
	public AppointmentResponse create(AppointmentRequest request) {
		Appointment appointment = new Appointment();
		apply(appointment, request);
		return AppointmentResponse.from(appointmentRepository.save(appointment));
	}

	@Transactional
	public AppointmentResponse update(Long id, AppointmentRequest request) {
		Appointment appointment = findAppointment(id);
		apply(appointment, request);
		return AppointmentResponse.from(appointment);
	}

	@Transactional
	public void delete(Long id) {
		appointmentRepository.delete(findAppointment(id));
	}

	private void apply(Appointment appointment, AppointmentRequest request) {
		User assignee = userRepository.findById(request.assigneeId())
			.orElseThrow(() -> new NotFoundException("Employee not found"));
		if (!assignee.isActive()) {
			throw new ConflictException("Appointments cannot be assigned to a deactivated employee");
		}
		appointment.setOrder(orderRepository.findById(request.orderId())
			.orElseThrow(() -> new NotFoundException("Order not found")));
		appointment.setType(request.type());
		appointment.setStartTime(request.startTime());
		appointment.setEndTime(request.endTime());
		appointment.setAssignee(assignee);
		appointment.setNotes(request.notes());
	}

	private Appointment findAppointment(Long id) {
		return appointmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Appointment not found"));
	}

}
