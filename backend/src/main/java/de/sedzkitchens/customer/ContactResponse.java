package de.sedzkitchens.customer;

import java.time.Instant;

public record ContactResponse(Long id, ContactType contactType, String summary, String createdByName,
		Instant createdAt) {
}
