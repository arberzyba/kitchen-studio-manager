package de.sedzkitchens.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(@NotBlank String name, @Email String email, String phone) {
}
