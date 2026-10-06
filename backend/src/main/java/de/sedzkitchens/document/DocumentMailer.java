package de.sedzkitchens.document;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import de.sedzkitchens.config.CompanyProperties;
import de.sedzkitchens.customer.Customer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Emails a PDF document (quote or invoice) to a customer
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentMailer {

	// Only present when a mail server is configured (spring.mail.host)
	private final ObjectProvider<JavaMailSender> mailSender;

	private final CompanyProperties company;

	public void send(String to, String subject, String body, String fileName, byte[] pdf) {
		JavaMailSender sender = mailSender.getIfAvailable();
		if (sender == null) {
			// Without a mail server (local development) the email is only logged
			log.info("No mail server configured. Would send '{}' to {} with attachment {} ({} bytes)", subject, to,
					fileName, pdf.length);
			return;
		}

		try {
			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			helper.setFrom(company.email());
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(body);
			helper.addAttachment(fileName, new ByteArrayResource(pdf), "application/pdf");
			sender.send(message);
		}
		catch (MessagingException ex) {
			throw new IllegalStateException("Could not build the email", ex);
		}
	}

	// The opening line of a German business email, e.g. "Sehr geehrte Frau Müller"
	public static String greeting(Customer customer) {
		return switch (customer.getSalutation()) {
			case MR -> "Sehr geehrter Herr " + customer.getLastName();
			case MS -> "Sehr geehrte Frau " + customer.getLastName();
			case NONE -> "Guten Tag " + customer.getFirstName() + " " + customer.getLastName();
		};
	}

	// The closing lines with the company's name and phone number
	public String signature() {
		return "Mit freundlichen Grüßen\n%s\nTelefon %s".formatted(company.name(), company.phone());
	}

}
