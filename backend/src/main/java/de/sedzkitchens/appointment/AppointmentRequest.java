package de.sedzkitchens.appointment;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppointmentRequest(@NotNull Long orderId, @NotNull AppointmentType type, @NotNull Instant startTime,
		@NotNull Instant endTime, @NotNull Long assigneeId, @Size(max = 1000) String notes) {

	// Reported as a validation error on "endAfterStart"
	@JsonIgnore
	@AssertTrue(message = "must be after the start time")
	public boolean isEndAfterStart() {
		return startTime == null || endTime == null || endTime.isAfter(startTime);
	}

}
