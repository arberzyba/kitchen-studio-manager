package de.sedzkitchens.customer;

import java.time.Instant;

public record CustomerResponse(Long id, Salutation salutation, String firstName, String lastName, String companyName,
		String email, String phone, AddressDto billingAddress, AddressDto installationAddress, Instant createdAt,
		Instant updatedAt, boolean anonymized) {
}
