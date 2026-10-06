package de.sedzkitchens.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.sedzkitchens.quote.Quote;
import de.sedzkitchens.quote.QuoteResponse;

public record InvoiceResponse(Long id, String invoiceNumber, InvoiceStatus status, boolean overdue,
		LocalDate invoiceDate, LocalDate serviceDate, LocalDate dueDate, Long orderId, String orderNumber,
		String recipientName, String recipientCompany, BigDecimal grossTotal, BigDecimal paidTotal,
		BigDecimal openAmount, Details details) {

	// Only present when a single invoice is requested: where to send it, what was paid and what it bills.
	// The quote is included because it holds the invoiced items and totals.
	public record Details(Long customerId, String customerEmail, List<PaymentResponse> payments,
			QuoteResponse quote) {
	}

	public record PaymentResponse(Long id, BigDecimal amount, LocalDate paidOn, String note) {
	}

	public static InvoiceResponse from(Invoice invoice) {
		Quote quote = invoice.getSalesOrder().getQuote();
		List<PaymentResponse> payments = invoice.getPayments()
			.stream()
			.map(payment -> new PaymentResponse(payment.getId(), payment.getAmount(), payment.getPaidOn(),
					payment.getNote()))
			.toList();
		return from(invoice, new Details(quote.getCustomer().getId(), quote.getCustomer().getEmail(), payments,
				QuoteResponse.from(quote)));
	}

	// For lists
	public static InvoiceResponse summary(Invoice invoice) {
		return from(invoice, null);
	}

	private static InvoiceResponse from(Invoice invoice, Details details) {
		return new InvoiceResponse(invoice.getId(), invoice.getInvoiceNumber(), invoice.getStatus(),
				invoice.isOverdue(), invoice.getInvoiceDate(), invoice.getServiceDate(), invoice.getDueDate(),
				invoice.getSalesOrder().getId(), invoice.getSalesOrder().getOrderNumber(),
				invoice.getRecipientName(), invoice.getRecipientCompany(), invoice.getGrossTotal(),
				invoice.getPaidTotal(), invoice.getOpenAmount(), details);
	}

}
