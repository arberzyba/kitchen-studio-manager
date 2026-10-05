package de.sedzkitchens.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(@NotBlank @Size(max = 50) String sku, @NotBlank String name,
		@Size(max = 1000) String description, @NotNull ProductCategory category, @NotNull ProductUnit unit,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal purchasePrice,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal sellingPrice,
		@NotNull Long supplierId, @NotNull Boolean active) {
}
