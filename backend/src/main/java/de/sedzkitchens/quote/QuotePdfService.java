package de.sedzkitchens.quote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import de.sedzkitchens.document.LetterContent;
import de.sedzkitchens.document.LetterPdfRenderer;
import lombok.RequiredArgsConstructor;

// Builds the quote letter (Angebot)
@Service
@RequiredArgsConstructor
public class QuotePdfService {

	private final LetterPdfRenderer renderer;

	public byte[] createPdf(Quote quote) {
		Map<String, String> details = new LinkedHashMap<>();
		details.put("Angebotsnummer", quote.getQuoteNumber());
		details.put("Datum", LetterPdfRenderer.DATE_FORMAT.format(quote.getQuoteDate()));
		details.put("Gültig bis", LetterPdfRenderer.DATE_FORMAT.format(quote.getValidUntil()));
		details.put("Ansprechpartner", quote.getCreatedBy().getFirstName() + " " + quote.getCreatedBy().getLastName());

		List<String> closing = new ArrayList<>();
		if (quote.getNotes() != null && !quote.getNotes().isBlank()) {
			closing.add(quote.getNotes());
		}
		closing.add("Dieses Angebot ist gültig bis zum %s. Alle Preise verstehen sich in Euro."
			.formatted(LetterPdfRenderer.DATE_FORMAT.format(quote.getValidUntil())));

		return renderer.render(new LetterContent(LetterContent.Recipient.of(quote.getCustomer()),
				"Angebot " + quote.getQuoteNumber(), details,
				"vielen Dank für Ihre Anfrage. Gerne unterbreiten wir Ihnen folgendes Angebot für Ihre neue Küche:",
				lines(quote), quote.calculateTotals(), quote.getDiscountPercent(), quote.getVatRate(), closing));
	}

	// The quote's items as printable lines; an invoice prints the same lines
	public static List<LetterContent.Line> lines(Quote quote) {
		return quote.getItems()
			.stream()
			.map(item -> new LetterContent.Line(item.getPosition(), item.getSku(), item.getDescription(),
					item.getQuantity(), item.getUnit(), item.getUnitPrice(), item.getDiscountPercent(),
					item.getLineTotal()))
			.toList();
	}

}
