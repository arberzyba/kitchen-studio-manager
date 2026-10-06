package de.sedzkitchens.order;

import java.time.LocalDate;

import de.sedzkitchens.quote.QuoteResponse;

// The quote is included in full because it holds the order's customer, items and totals
public record OrderResponse(Long id, String orderNumber, OrderStatus status, LocalDate orderDate,
		String createdByName, QuoteResponse quote) {

	public static OrderResponse from(SalesOrder order) {
		return new OrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(), order.getOrderDate(),
				order.getCreatedBy().getFirstName() + " " + order.getCreatedBy().getLastName(),
				QuoteResponse.from(order.getQuote()));
	}

}
