package de.sedzkitchens.privacy;

import java.time.Instant;
import java.util.List;

import de.sedzkitchens.appointment.AppointmentResponse;
import de.sedzkitchens.customer.ContactResponse;
import de.sedzkitchens.customer.CustomerResponse;
import de.sedzkitchens.invoice.InvoiceResponse;
import de.sedzkitchens.order.OrderSummaryResponse;
import de.sedzkitchens.quote.QuoteResponse;

// Everything the system stores about one customer, for a data subject access request (GDPR Art. 15 and 20)
public record CustomerDataExport(Instant exportedAt, CustomerResponse customer, List<ContactResponse> contactHistory,
		List<QuoteResponse> quotes, List<OrderSummaryResponse> orders, List<AppointmentResponse> appointments,
		List<InvoiceResponse> invoices) {
}
