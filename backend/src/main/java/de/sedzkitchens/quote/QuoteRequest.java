package de.sedzkitchens.quote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuoteRequest(@NotNull Long customerId, @NotNull LocalDate validUntil,
		@NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal discountPercent,
		@Size(max = 2000) String notes, @NotEmpty @Valid List<Item> items) {

	public record Item(@NotNull Long productId,
			@NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
			@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal unitPrice,
			@NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3,
					fraction = 2) BigDecimal discountPercent) {
	}

}
