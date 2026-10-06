package de.sedzkitchens.supplierorder;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// Request bodies of the supplier order endpoints
public final class SupplierOrderRequests {

	private SupplierOrderRequests() {
	}

	// Orders everything the customer order needs from this supplier
	public record Create(@NotNull Long orderId, @NotNull Long supplierId, @NotNull LocalDate expectedDeliveryDate,
			@Size(max = 1000) String notes) {
	}

	public record Update(@NotNull LocalDate expectedDeliveryDate, @Size(max = 1000) String notes) {
	}

	public record Delivered(@NotNull @PastOrPresent LocalDate actualDeliveryDate) {
	}

}
