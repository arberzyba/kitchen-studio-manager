package de.sedzkitchens.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import de.sedzkitchens.order.SalesOrder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Invoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String invoiceNumber;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_order_id")
	private SalesOrder salesOrder;

	@Enumerated(EnumType.STRING)
	private InvoiceStatus status = InvoiceStatus.OPEN;

	private LocalDate invoiceDate;

	// When the goods and services were delivered (Leistungsdatum)
	private LocalDate serviceDate;

	private LocalDate dueDate;

	// Recipient and amounts are copied when the invoice is issued, so later changes to the customer
	// or the catalog cannot alter a document that has already been sent
	private String recipientCompany;

	private String recipientName;

	private String recipientStreet;

	private String recipientPostalCode;

	private String recipientCity;

	private BigDecimal vatRate;

	private BigDecimal netTotal;

	private BigDecimal vatAmount;

	private BigDecimal grossTotal;

	private BigDecimal paidTotal = BigDecimal.ZERO;

	@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("paidOn, id")
	private List<Payment> payments = new ArrayList<>();

	@CreationTimestamp
	private Instant createdAt;

	public BigDecimal getOpenAmount() {
		return grossTotal.subtract(paidTotal);
	}

	// Not fully paid although the due date has passed
	public boolean isOverdue() {
		return status != InvoiceStatus.PAID && dueDate.isBefore(LocalDate.now());
	}

	// Recalculates the paid total and the status from the recorded payments
	public void refreshPaymentStatus() {
		paidTotal = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (paidTotal.signum() == 0) {
			status = InvoiceStatus.OPEN;
		}
		else {
			status = paidTotal.compareTo(grossTotal) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
		}
	}

}
