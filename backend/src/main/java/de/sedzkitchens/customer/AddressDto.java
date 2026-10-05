package de.sedzkitchens.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddressDto(@NotBlank String street,
		@NotBlank @Pattern(regexp = "\\d{5}", message = "must be a 5-digit postal code") String postalCode,
		@NotBlank String city) {
}
