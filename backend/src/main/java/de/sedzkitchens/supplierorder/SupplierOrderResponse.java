package de.sedzkitchens.supplierorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.sedzkitchens.product.ProductUnit;

public record SupplierOrderResponse(Long id, String orderNumber, SupplierOrderStatus status, boolean overdue,
		Long supplierId, String supplierName, Long salesOrderId, String salesOrderNumber, String customerName,
		LocalDate orderedDate, LocalDate expectedDeliveryDate, LocalDate actualDeliveryDate, BigDecimal total,
		String notes, List<Item> items) {

	public record Item(String sku, String description, ProductUnit unit, BigDecimal quantity,
			BigDecimal purchasePrice, BigDecimal lineTotal) {
	}

	// A supplier the customer order has items from, but which has not been ordered from yet
	public record Pending(Long supplierId, String supplierName, int itemCount) {
	}

	public static SupplierOrderResponse from(SupplierOrder order) {
		return from(order,
				order.getItems()
					.stream()
					.map(item -> new Item(item.getSku(), item.getDescription(), item.getUnit(), item.getQuantity(),
							item.getPurchasePrice(), item.getLineTotal()))
					.toList());
	}

	// For lists, which do not show the individual items
	public static SupplierOrderResponse summary(SupplierOrder order) {
		return from(order, List.of());
	}

	private static SupplierOrderResponse from(SupplierOrder order, List<Item> items) {
		return new SupplierOrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(), order.isOverdue(),
				order.getSupplier().getId(), order.getSupplier().getName(), order.getSalesOrder().getId(),
				order.getSalesOrder().getOrderNumber(),
				order.getSalesOrder().getQuote().getCustomer().getDisplayName(), order.getOrderedDate(),
				order.getExpectedDeliveryDate(), order.getActualDeliveryDate(), order.getTotal(), order.getNotes(),
				items);
	}

}
