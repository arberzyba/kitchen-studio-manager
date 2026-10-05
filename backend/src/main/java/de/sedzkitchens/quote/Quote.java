package de.sedzkitchens.quote;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import de.sedzkitchens.customer.Customer;
import de.sedzkitchens.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
public class Quote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String quoteNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@Enumerated(EnumType.STRING)
	private QuoteStatus status = QuoteStatus.DRAFT;

	private LocalDate quoteDate;

	private LocalDate validUntil;

	// Overall discount on the sum of all lines, in percent
	private BigDecimal discountPercent;

	// Stored with the quote so a later change of the legal VAT rate does not alter old quotes
	private BigDecimal vatRate;

	private BigDecimal netTotal;

	private BigDecimal vatAmount;

	private BigDecimal grossTotal;

	private String notes;

	@OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<QuoteItem> items = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	public QuoteCalculator.Totals calculateTotals() {
		return QuoteCalculator.totals(items.stream().map(QuoteItem::getLineTotal).toList(), discountPercent, vatRate);
	}

}
