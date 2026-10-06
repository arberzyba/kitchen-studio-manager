package de.sedzkitchens.appointment;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.sedzkitchens.user.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Admin, sales and office staff plan appointments; installers can only read the ones assigned to them
@RestController
@RequestMapping("/api/appointments")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OFFICE')")
@RequiredArgsConstructor
public class AppointmentController {

	private final AppointmentService appointmentService;

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public List<AppointmentResponse> findInPeriod(@RequestParam Instant from, @RequestParam Instant to,
			@RequestParam(required = false) Long assigneeId, @AuthenticationPrincipal Jwt jwt) {
		// An installer always gets their own appointments, whatever filter was requested
		if (Role.INSTALLER.name().equals(jwt.getClaimAsString("role"))) {
			assigneeId = Long.valueOf(jwt.getSubject());
		}
		return appointmentService.findInPeriod(from, to, assigneeId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AppointmentResponse create(@Valid @RequestBody AppointmentRequest request) {
		return appointmentService.create(request);
	}

	@PutMapping("/{id}")
	public AppointmentResponse update(@PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
		return appointmentService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		appointmentService.delete(id);
	}

}
