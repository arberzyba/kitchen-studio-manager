package de.sedzkitchens.quote;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class QuoteCalculatorTests {

	private static final BigDecimal VAT = new BigDecimal("19.00");

	@Test
	void lineTotalIsQuantityTimesUnitPrice() {
		assertThat(lineTotal("4", "189.00", "0")).isEqualByComparingTo("756.00");
	}

	@Test
	void lineTotalSupportsFractionalQuantitiesForWorktops() {
		assertThat(lineTotal("4.2", "369.00", "0")).isEqualByComparingTo("1549.80");
	}

	@Test
	void lineDiscountReducesTheLineAndRoundsToCents() {
		assertThat(lineTotal("1", "649.00", "10")).isEqualByComparingTo("584.10");
		// 3 x 33.33 = 99.99, less 12.5 % = 87.49125, rounded half up
		assertThat(lineTotal("3", "33.33", "12.5")).isEqualByComparingTo("87.49");
	}

	@Test
	void totalsWithoutDiscountAdd19PercentVat() {
		QuoteCalculator.Totals totals = QuoteCalculator.totals(amounts("756.00", "599.00"), BigDecimal.ZERO, VAT);

		assertThat(totals.subtotal()).isEqualByComparingTo("1355.00");
		assertThat(totals.discountAmount()).isEqualByComparingTo("0.00");
		assertThat(totals.netTotal()).isEqualByComparingTo("1355.00");
		assertThat(totals.vatAmount()).isEqualByComparingTo("257.45");
		assertThat(totals.grossTotal()).isEqualByComparingTo("1612.45");
	}

	@Test
	void overallDiscountIsTakenBeforeVat() {
		QuoteCalculator.Totals totals = QuoteCalculator.totals(amounts("1000.00"), new BigDecimal("5.00"), VAT);

		assertThat(totals.discountAmount()).isEqualByComparingTo("50.00");
		assertThat(totals.netTotal()).isEqualByComparingTo("950.00");
		assertThat(totals.vatAmount()).isEqualByComparingTo("180.50");
		assertThat(totals.grossTotal()).isEqualByComparingTo("1130.50");
	}

	@Test
	void vatIsRoundedHalfUpToCents() {
		// 19 % of 0.50 is 0.095, which rounds up to 0.10
		QuoteCalculator.Totals totals = QuoteCalculator.totals(amounts("0.50"), BigDecimal.ZERO, VAT);

		assertThat(totals.vatAmount()).isEqualByComparingTo("0.10");
		assertThat(totals.grossTotal()).isEqualByComparingTo("0.60");
	}

	private BigDecimal lineTotal(String quantity, String unitPrice, String discountPercent) {
		return QuoteCalculator.lineTotal(new BigDecimal(quantity), new BigDecimal(unitPrice),
				new BigDecimal(discountPercent));
	}

	private List<BigDecimal> amounts(String... values) {
		return List.of(values).stream().map(BigDecimal::new).toList();
	}

}
