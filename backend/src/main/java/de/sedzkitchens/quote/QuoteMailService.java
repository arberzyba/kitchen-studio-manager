package de.sedzkitchens.quote;

import org.springframework.stereotype.Service;

import de.sedzkitchens.customer.Customer;
import de.sedzkitchens.document.DocumentMailer;
import de.sedzkitchens.document.LetterPdfRenderer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuoteMailService {

	private final DocumentMailer mailer;

	public void send(Quote quote, byte[] pdf) {
		Customer customer = quote.getCustomer();
		String body = """
				%s,

				vielen Dank für Ihr Interesse. Im Anhang erhalten Sie unser Angebot %s.
				Das Angebot ist gültig bis zum %s.

				Bei Fragen sind wir gerne für Sie da.

				%s
				""".formatted(DocumentMailer.greeting(customer), quote.getQuoteNumber(),
				LetterPdfRenderer.DATE_FORMAT.format(quote.getValidUntil()), mailer.signature());
		mailer.send(customer.getEmail(), "Ihr Angebot " + quote.getQuoteNumber(), body,
				"Angebot-" + quote.getQuoteNumber() + ".pdf", pdf);
	}

}
