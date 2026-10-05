package de.sedzkitchens.quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// Price arithmetic for quotes. Every amount is rounded to whole cents, half up, at each step.
public final class QuoteCalculator {

	public static final BigDecimal VAT_RATE = new BigDecimal("19.00");

	private static final BigDecimal HUNDRED = new BigDecimal("100");

	private QuoteCalculator() {
	}

	public record Totals(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal netTotal, BigDecimal vatAmount,
			BigDecimal grossTotal) {
	}

	// Net amount of one line: quantity x unit price, less the line discount
	public static BigDecimal lineTotal(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountPercent) {
		BigDecimal full = quantity.multiply(unitPrice);
		return full.subtract(percentOf(full, discountPercent)).setScale(2, RoundingMode.HALF_UP);
	}

	// The overall discount applies to the sum of the lines; VAT is charged on the discounted net amount
	public static Totals totals(List<BigDecimal> lineTotals, BigDecimal discountPercent, BigDecimal vatRate) {
		BigDecimal subtotal = lineTotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
		BigDecimal discountAmount = percentOf(subtotal, discountPercent).setScale(2, RoundingMode.HALF_UP);
		BigDecimal netTotal = subtotal.subtract(discountAmount);
		BigDecimal vatAmount = percentOf(netTotal, vatRate).setScale(2, RoundingMode.HALF_UP);
		return new Totals(subtotal, discountAmount, netTotal, vatAmount, netTotal.add(vatAmount));
	}

	private static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
		return amount.multiply(percent).divide(HUNDRED);
	}

}
