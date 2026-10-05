package de.sedzkitchens.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerRequest(@NotNull Salutation salutation, @NotBlank String firstName, @NotBlank String lastName,
		String companyName, @Email String email, String phone, @NotNull @Valid AddressDto billingAddress,
		@Valid AddressDto installationAddress) {
}
