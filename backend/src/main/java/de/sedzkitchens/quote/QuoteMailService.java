package de.sedzkitchens.quote;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import de.sedzkitchens.config.CompanyProperties;
import de.sedzkitchens.customer.Customer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteMailService {

	// Only present when a mail server is configured (spring.mail.host)
	private final ObjectProvider<JavaMailSender> mailSender;

	private final CompanyProperties company;

	public void send(Quote quote, byte[] pdf) {
		Customer customer = quote.getCustomer();
		String subject = "Ihr Angebot " + quote.getQuoteNumber();
		String fileName = "Angebot-" + quote.getQuoteNumber() + ".pdf";

		JavaMailSender sender = mailSender.getIfAvailable();
		if (sender == null) {
			// Without a mail server (local development) the email is only logged
			log.info("No mail server configured. Would send '{}' to {} with attachment {} ({} bytes)", subject,
					customer.getEmail(), fileName, pdf.length);
			return;
		}

		try {
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			helper.setFrom(company.email());
			helper.setTo(customer.getEmail());
			helper.setSubject(subject);
			helper.setText(body(customer, quote));
			helper.addAttachment(fileName, new ByteArrayResource(pdf), "application/pdf");
			sender.send(message);
		}
		catch (MessagingException ex) {
			throw new IllegalStateException("Could not build the quote email", ex);
		}
	}

	private String body(Customer customer, Quote quote) {
		String greeting = switch (customer.getSalutation()) {
			case MR -> "Sehr geehrter Herr " + customer.getLastName();
			case MS -> "Sehr geehrte Frau " + customer.getLastName();
			case NONE -> "Guten Tag " + customer.getFirstName() + " " + customer.getLastName();
		};
		return """
				%s,

				vielen Dank für Ihr Interesse. Im Anhang erhalten Sie unser Angebot %s.
				Das Angebot ist gültig bis zum %s.

				Bei Fragen sind wir gerne für Sie da.

				Mit freundlichen Grüßen
				%s
				Telefon %s
				""".formatted(greeting, quote.getQuoteNumber(), QuotePdfService.DATE_FORMAT.format(quote.getValidUntil()),
				company.name(), company.phone());
	}

}
