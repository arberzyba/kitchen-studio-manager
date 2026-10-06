package de.sedzkitchens.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// Request bodies of the invoice endpoints
public final class InvoiceRequests {

	private InvoiceRequests() {
	}

	public record Create(@NotNull Long orderId, @NotNull LocalDate serviceDate, @NotNull LocalDate dueDate) {
	}

	public record AddPayment(@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
			@NotNull @PastOrPresent LocalDate paidOn, @Size(max = 500) String note) {
	}

}
