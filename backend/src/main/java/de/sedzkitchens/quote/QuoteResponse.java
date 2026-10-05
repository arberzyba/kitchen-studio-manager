package de.sedzkitchens.quote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.sedzkitchens.customer.Customer;
import de.sedzkitchens.product.ProductUnit;

public record QuoteResponse(Long id, String quoteNumber, QuoteStatus status, LocalDate quoteDate, LocalDate validUntil,
		Long customerId, String customerName, String customerEmail, BigDecimal discountPercent, BigDecimal vatRate,
		String notes, List<Item> items, BigDecimal subtotal, BigDecimal discountAmount, BigDecimal netTotal,
		BigDecimal vatAmount, BigDecimal grossTotal, String createdByName) {

	public record Item(Long productId, String sku, String description, ProductUnit unit, BigDecimal quantity,
			BigDecimal unitPrice, BigDecimal discountPercent, BigDecimal lineTotal) {
	}

	public static QuoteResponse from(Quote quote) {
		Customer customer = quote.getCustomer();
		QuoteCalculator.Totals totals = quote.calculateTotals();
		List<Item> items = quote.getItems()
			.stream()
			.map(item -> new Item(item.getProduct().getId(), item.getSku(), item.getDescription(), item.getUnit(),
					item.getQuantity(), item.getUnitPrice(), item.getDiscountPercent(), item.getLineTotal()))
			.toList();
		return new QuoteResponse(quote.getId(), quote.getQuoteNumber(), quote.getStatus(), quote.getQuoteDate(),
				quote.getValidUntil(), customer.getId(), customer.getDisplayName(), customer.getEmail(),
				quote.getDiscountPercent(), quote.getVatRate(), quote.getNotes(), items, totals.subtotal(),
				totals.discountAmount(), totals.netTotal(), totals.vatAmount(), totals.grossTotal(),
				quote.getCreatedBy().getFirstName() + " " + quote.getCreatedBy().getLastName());
	}

}
