package de.sedzkitchens.appointment;

import java.time.Instant;

import de.sedzkitchens.customer.Address;
import de.sedzkitchens.customer.AddressDto;
import de.sedzkitchens.customer.Customer;

// Includes what the assigned employee needs on site: who the customer is, how to reach them and where to go
public record AppointmentResponse(Long id, AppointmentType type, Instant startTime, Instant endTime, String notes,
		Long assigneeId, String assigneeName, Long orderId, String orderNumber, String customerName,
		String customerPhone, AddressDto address) {

	public static AppointmentResponse from(Appointment appointment) {
		Customer customer = appointment.getOrder().getQuote().getCustomer();
		// The kitchen goes to the installation address if the customer has a separate one
		Address address = customer.getInstallationAddress() != null ? customer.getInstallationAddress()
				: customer.getBillingAddress();
		return new AppointmentResponse(appointment.getId(), appointment.getType(), appointment.getStartTime(),
				appointment.getEndTime(), appointment.getNotes(), appointment.getAssignee().getId(),
				appointment.getAssignee().getFirstName() + " " + appointment.getAssignee().getLastName(),
				appointment.getOrder().getId(), appointment.getOrder().getOrderNumber(), customer.getDisplayName(),
				customer.getPhone(), new AddressDto(address.getStreet(), address.getPostalCode(), address.getCity()));
	}

}
