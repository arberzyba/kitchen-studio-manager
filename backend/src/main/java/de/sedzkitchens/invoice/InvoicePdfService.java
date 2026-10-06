package de.sedzkitchens.invoice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import de.sedzkitchens.document.LetterContent;
import de.sedzkitchens.document.LetterPdfRenderer;
import de.sedzkitchens.quote.Quote;
import de.sedzkitchens.quote.QuotePdfService;
import lombok.RequiredArgsConstructor;

// Builds the invoice letter (Rechnung) with the details German invoices must carry: sequential number,
// invoice date, date of delivery, net / VAT / gross, and the issuer's tax numbers in the footer
@Service
@RequiredArgsConstructor
public class InvoicePdfService {

	private final LetterPdfRenderer renderer;

	public byte[] createPdf(Invoice invoice) {
		Quote quote = invoice.getSalesOrder().getQuote();

		Map<String, String> details = new LinkedHashMap<>();
		details.put("Rechnungsnummer", invoice.getInvoiceNumber());
		details.put("Rechnungsdatum", LetterPdfRenderer.DATE_FORMAT.format(invoice.getInvoiceDate()));
		details.put("Leistungsdatum", LetterPdfRenderer.DATE_FORMAT.format(invoice.getServiceDate()));
		details.put("Auftragsnummer", invoice.getSalesOrder().getOrderNumber());

		String paymentTerms = "Bitte überweisen Sie den Gesamtbetrag von %s bis zum %s auf das unten genannte Konto. "
			.formatted(LetterPdfRenderer.euro(invoice.getGrossTotal()),
					LetterPdfRenderer.DATE_FORMAT.format(invoice.getDueDate()))
				+ "Verwendungszweck: " + invoice.getInvoiceNumber();

		return renderer.render(new LetterContent(
				new LetterContent.Recipient(invoice.getRecipientCompany(), invoice.getRecipientName(),
						invoice.getRecipientStreet(), invoice.getRecipientPostalCode(), invoice.getRecipientCity()),
				"Rechnung " + invoice.getInvoiceNumber(), details,
				"vielen Dank für Ihren Auftrag. Wir berechnen Ihnen folgende Lieferungen und Leistungen:",
				QuotePdfService.lines(quote), quote.calculateTotals(), quote.getDiscountPercent(),
				invoice.getVatRate(), List.of(paymentTerms)));
	}

}
