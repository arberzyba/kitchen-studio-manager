package de.sedzkitchens.order;

import java.math.BigDecimal;
import java.time.LocalDate;

// One row of the order list
public record OrderSummaryResponse(Long id, String orderNumber, String customerName, OrderStatus status,
		LocalDate orderDate, BigDecimal grossTotal) {

	public static OrderSummaryResponse from(SalesOrder order) {
		return new OrderSummaryResponse(order.getId(), order.getOrderNumber(),
				order.getQuote().getCustomer().getDisplayName(), order.getStatus(), order.getOrderDate(),
				order.getQuote().getGrossTotal());
	}

}
