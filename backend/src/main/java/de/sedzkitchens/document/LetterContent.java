package de.sedzkitchens.document;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import de.sedzkitchens.customer.Address;
import de.sedzkitchens.customer.Customer;
import de.sedzkitchens.product.ProductUnit;
import de.sedzkitchens.quote.QuoteCalculator;

// Everything that varies between the business letters the system prints (quotes and invoices)
public record LetterContent(Recipient recipient, String title, Map<String, String> details, String intro,
		List<Line> lines, QuoteCalculator.Totals totals, BigDecimal discountPercent, BigDecimal vatRate,
		List<String> closingParagraphs) {

	// "name" already includes the form of address, e.g. "Frau Sabine Müller"
	public record Recipient(String company, String name, String street, String postalCode, String city) {

		public static Recipient of(Customer customer) {
			String salutation = switch (customer.getSalutation()) {
				case MR -> "Herr ";
				case MS -> "Frau ";
				case NONE -> "";
			};
			Address address = customer.getBillingAddress();
			return new Recipient(customer.getCompanyName(), salutation + customer.getDisplayName(),
					address.getStreet(), address.getPostalCode(), address.getCity());
		}

	}

	public record Line(int position, String sku, String description, BigDecimal quantity, ProductUnit unit,
			BigDecimal unitPrice, BigDecimal discountPercent, BigDecimal lineTotal) {
	}

}
