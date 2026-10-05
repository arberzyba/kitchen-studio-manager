package de.sedzkitchens.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactRequest(@NotNull ContactType contactType, @NotBlank @Size(max = 2000) String summary) {
}
