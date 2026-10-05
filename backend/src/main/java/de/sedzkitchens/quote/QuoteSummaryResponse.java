package de.sedzkitchens.quote;

import java.math.BigDecimal;
import java.time.LocalDate;

// One row of the quote list
public record QuoteSummaryResponse(Long id, String quoteNumber, String customerName, QuoteStatus status,
		LocalDate quoteDate, LocalDate validUntil, BigDecimal grossTotal) {

	public static QuoteSummaryResponse from(Quote quote) {
		return new QuoteSummaryResponse(quote.getId(), quote.getQuoteNumber(), quote.getCustomer().getDisplayName(),
				quote.getStatus(), quote.getQuoteDate(), quote.getValidUntil(), quote.getGrossTotal());
	}

}
