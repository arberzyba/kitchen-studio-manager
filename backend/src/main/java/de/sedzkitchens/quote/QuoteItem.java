package de.sedzkitchens.quote;

import java.math.BigDecimal;

import de.sedzkitchens.product.Product;
import de.sedzkitchens.product.ProductUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// One line of a quote. Article number, description and unit are copied from the product when the line is saved.
@Entity
@Table(name = "quote_items")
@Getter
@Setter
@NoArgsConstructor
public class QuoteItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "quote_id")
	private Quote quote;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "line_number")
	private int position;

	private String sku;

	private String description;

	@Enumerated(EnumType.STRING)
	private ProductUnit unit;

	private BigDecimal quantity;

	// Net price per unit in euros
	private BigDecimal unitPrice;

	private BigDecimal discountPercent;

	public BigDecimal getLineTotal() {
		return QuoteCalculator.lineTotal(quantity, unitPrice, discountPercent);
	}

}
