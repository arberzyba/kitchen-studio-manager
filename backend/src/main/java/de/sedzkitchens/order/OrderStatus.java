package de.sedzkitchens.order;

import java.util.Optional;

// The steps of a kitchen order, in the order they happen
public enum OrderStatus {

	NEW, MEASURED, ORDERED_FROM_SUPPLIER, DELIVERED, INSTALLED, COMPLETED;

	// The status that follows this one; empty once the order is completed
	public Optional<OrderStatus> next() {
		OrderStatus[] all = values();
		return ordinal() + 1 < all.length ? Optional.of(all[ordinal() + 1]) : Optional.empty();
	}

}
