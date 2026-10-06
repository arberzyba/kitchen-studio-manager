package de.sedzkitchens.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import de.sedzkitchens.invoice.InvoiceResponse;

public record DashboardResponse(OpenQuotes openQuotes, List<MonthRevenue> monthlyRevenue,
		List<Installation> upcomingInstallations, OverdueInvoices overdueInvoices) {

	// Quotes that have not been decided yet: still being written, or sent and awaiting the customer's answer
	public record OpenQuotes(long draftCount, long sentCount, BigDecimal sentGrossTotal) {
	}

	// Net amount invoiced in a calendar month, e.g. "2026-10"
	public record MonthRevenue(String month, BigDecimal netTotal) {
	}

	public record Installation(Long appointmentId, Instant startTime, String customerName, String city,
			String assigneeName, Long orderId, String orderNumber) {
	}

	// The count and open amount cover all overdue invoices; the list shows the longest overdue ones
	public record OverdueInvoices(long count, BigDecimal openAmount, List<InvoiceResponse> invoices) {
	}

}
