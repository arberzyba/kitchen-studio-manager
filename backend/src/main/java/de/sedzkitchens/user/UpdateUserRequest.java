package de.sedzkitchens.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(@NotBlank String firstName, @NotBlank String lastName, @NotNull Role role,
		@NotNull Boolean active) {
}
