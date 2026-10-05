package de.sedzkitchens.quote;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import de.sedzkitchens.config.CompanyProperties;
import de.sedzkitchens.customer.Address;
import de.sedzkitchens.customer.Customer;
import de.sedzkitchens.product.ProductUnit;
import lombok.RequiredArgsConstructor;

// Renders a quote as a German business letter (Angebot): sender, recipient, line items,
// net / VAT / gross breakdown and the legally required company details in the footer.
@Service
@RequiredArgsConstructor
public class QuotePdfService {

	static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);

	private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f);

	private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9.5f);

	private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);

	private final CompanyProperties company;

	public byte[] createPdf(Quote quote) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document document = new Document(PageSize.A4, 60, 50, 50, 90);
		PdfWriter writer = PdfWriter.getInstance(document, out);
		writer.setPageEvent(new Footer());
		document.open();

		addAddressBlock(document, quote.getCustomer());
		addQuoteDetails(document, quote);

		Paragraph title = new Paragraph("Angebot " + quote.getQuoteNumber(), TITLE);
		title.setSpacingBefore(20);
		title.setSpacingAfter(10);
		document.add(title);
		document.add(new Paragraph(
				"vielen Dank für Ihre Anfrage. Gerne unterbreiten wir Ihnen folgendes Angebot für Ihre neue Küche:",
				NORMAL));

		addItems(document, quote);
		addTotals(document, quote);

		if (quote.getNotes() != null && !quote.getNotes().isBlank()) {
			Paragraph notes = new Paragraph(quote.getNotes(), NORMAL);
			notes.setSpacingBefore(15);
			document.add(notes);
		}
		Paragraph closing = new Paragraph(
				"Dieses Angebot ist gültig bis zum %s. Alle Preise verstehen sich in Euro."
					.formatted(DATE_FORMAT.format(quote.getValidUntil())),
				NORMAL);
		closing.setSpacingBefore(15);
		document.add(closing);

		document.close();
		return out.toByteArray();
	}

	private void addAddressBlock(Document document, Customer customer) {
		document.add(new Paragraph("%s · %s · %s %s".formatted(company.name(), company.street(),
				company.postalCode(), company.city()), SMALL));

		Address address = customer.getBillingAddress();
		Paragraph recipient = new Paragraph();
		recipient.setSpacingBefore(8);
		if (customer.getCompanyName() != null) {
			recipient.add(new Phrase(customer.getCompanyName() + "\n", NORMAL));
		}
		String salutation = switch (customer.getSalutation()) {
			case MR -> "Herr ";
			case MS -> "Frau ";
			case NONE -> "";
		};
		recipient.add(new Phrase(salutation + customer.getDisplayName() + "\n", NORMAL));
		recipient.add(new Phrase(address.getStreet() + "\n", NORMAL));
		recipient.add(new Phrase(address.getPostalCode() + " " + address.getCity(), NORMAL));
		document.add(recipient);
	}

	private void addQuoteDetails(Document document, Quote quote) {
		PdfPTable details = new PdfPTable(new float[] { 1, 1 });
		details.setWidthPercentage(42);
		details.setHorizontalAlignment(Element.ALIGN_RIGHT);
		details.setSpacingBefore(10);
		addDetail(details, "Angebotsnummer", quote.getQuoteNumber());
		addDetail(details, "Datum", DATE_FORMAT.format(quote.getQuoteDate()));
		addDetail(details, "Gültig bis", DATE_FORMAT.format(quote.getValidUntil()));
		addDetail(details, "Ansprechpartner",
				quote.getCreatedBy().getFirstName() + " " + quote.getCreatedBy().getLastName());
		document.add(details);
	}

	private void addDetail(PdfPTable table, String label, String value) {
		table.addCell(cell(label, NORMAL, Element.ALIGN_LEFT, Rectangle.NO_BORDER));
		table.addCell(cell(value, NORMAL, Element.ALIGN_RIGHT, Rectangle.NO_BORDER));
	}

	private void addItems(Document document, Quote quote) {
		PdfPTable table = new PdfPTable(new float[] { 7, 13, 32, 12, 14, 9, 14 });
		table.setWidthPercentage(100);
		table.setSpacingBefore(15);
		table.setHeaderRows(1);
		String[] headers = { "Pos.", "Art.-Nr.", "Bezeichnung", "Menge", "Einzelpreis", "Rabatt", "Gesamt" };
		for (int column = 0; column < headers.length; column++) {
			table.addCell(cell(headers[column], BOLD, column < 3 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT,
					Rectangle.BOTTOM));
		}
		for (QuoteItem item : quote.getItems()) {
			table.addCell(cell(String.valueOf(item.getPosition()), NORMAL, Element.ALIGN_LEFT, Rectangle.NO_BORDER));
			table.addCell(cell(item.getSku(), NORMAL, Element.ALIGN_LEFT, Rectangle.NO_BORDER));
			table.addCell(cell(item.getDescription(), NORMAL, Element.ALIGN_LEFT, Rectangle.NO_BORDER));
			table.addCell(cell(number(item.getQuantity()) + (item.getUnit() == ProductUnit.METER ? " m" : " Stk."),
					NORMAL, Element.ALIGN_RIGHT, Rectangle.NO_BORDER));
			table.addCell(cell(euro(item.getUnitPrice()), NORMAL, Element.ALIGN_RIGHT, Rectangle.NO_BORDER));
			table.addCell(cell(item.getDiscountPercent().signum() == 0 ? "" : percent(item.getDiscountPercent()),
					NORMAL, Element.ALIGN_RIGHT, Rectangle.NO_BORDER));
			table.addCell(cell(euro(item.getLineTotal()), NORMAL, Element.ALIGN_RIGHT, Rectangle.NO_BORDER));
		}
		document.add(table);
	}

	private void addTotals(Document document, Quote quote) {
		QuoteCalculator.Totals totals = quote.calculateTotals();
		PdfPTable table = new PdfPTable(new float[] { 3, 2 });
		table.setWidthPercentage(45);
		table.setHorizontalAlignment(Element.ALIGN_RIGHT);
		table.setSpacingBefore(10);
		if (totals.discountAmount().signum() != 0) {
			addTotal(table, "Zwischensumme", euro(totals.subtotal()), NORMAL, Rectangle.TOP);
			addTotal(table, "Rabatt " + percent(quote.getDiscountPercent()), "- " + euro(totals.discountAmount()),
					NORMAL, Rectangle.NO_BORDER);
			addTotal(table, "Nettobetrag", euro(totals.netTotal()), NORMAL, Rectangle.NO_BORDER);
		}
		else {
			addTotal(table, "Nettobetrag", euro(totals.netTotal()), NORMAL, Rectangle.TOP);
		}
		addTotal(table, "zzgl. " + percent(quote.getVatRate()) + " MwSt.", euro(totals.vatAmount()), NORMAL,
				Rectangle.NO_BORDER);
		addTotal(table, "Gesamtbetrag", euro(totals.grossTotal()), BOLD, Rectangle.TOP);
		document.add(table);
	}

	private void addTotal(PdfPTable table, String label, String value, Font font, int border) {
		table.addCell(cell(label, font, Element.ALIGN_LEFT, border));
		table.addCell(cell(value, font, Element.ALIGN_RIGHT, border));
	}

	private static PdfPCell cell(String text, Font font, int alignment, int border) {
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		cell.setHorizontalAlignment(alignment);
		cell.setBorder(border);
		cell.setPadding(4);
		return cell;
	}

	// German formats: 1.299,00 €, 2,5 and 19 %
	private static String euro(BigDecimal amount) {
		return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(amount);
	}

	private static String number(BigDecimal value) {
		return NumberFormat.getNumberInstance(Locale.GERMANY).format(value);
	}

	private static String percent(BigDecimal value) {
		return number(value) + " %";
	}

	// Company details required on German business letters, repeated at the bottom of every page
	private class Footer extends PdfPageEventHelper {

		@Override
		public void onEndPage(PdfWriter writer, Document document) {
			PdfPTable footer = new PdfPTable(3);
			footer.setTotalWidth(document.right() - document.left());
			footer.addCell(footerCell("%s\n%s\n%s %s\nTelefon %s\n%s".formatted(company.name(), company.street(),
					company.postalCode(), company.city(), company.phone(), company.email())));
			footer.addCell(footerCell("Geschäftsführung: %s\n%s %s\nSteuernummer: %s\nUSt-IdNr.: %s".formatted(
					company.managingDirector(), company.registerCourt(), company.registerNumber(), company.taxNumber(),
					company.vatId())));
			footer.addCell(footerCell(
					"%s\nIBAN: %s\nBIC: %s".formatted(company.bankName(), company.iban(), company.bic())));
			footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 10, writer.getDirectContent());
		}

		private PdfPCell footerCell(String text) {
			return cell(text, SMALL, Element.ALIGN_LEFT, Rectangle.TOP);
		}

	}

}
