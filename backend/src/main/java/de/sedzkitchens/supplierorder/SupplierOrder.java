package de.sedzkitchens.supplierorder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import de.sedzkitchens.order.SalesOrder;
import de.sedzkitchens.supplier.Supplier;
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

// A purchase order placed with one supplier for the items of one customer order
@Entity
@Table(name = "supplier_orders")
@Getter
@Setter
@NoArgsConstructor
public class SupplierOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String orderNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_order_id")
	private SalesOrder salesOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id")
	private Supplier supplier;

	@Enumerated(EnumType.STRING)
	private SupplierOrderStatus status = SupplierOrderStatus.ORDERED;

	private LocalDate orderedDate;

	private LocalDate expectedDeliveryDate;

	// Null until the goods have arrived
	private LocalDate actualDeliveryDate;

	// Net purchase value of all items, in euros
	private BigDecimal total;

	private String notes;

	@OneToMany(mappedBy = "supplierOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<SupplierOrderItem> items = new ArrayList<>();

	@CreationTimestamp
	private Instant createdAt;

	// Not yet delivered although the expected date has passed
	public boolean isOverdue() {
		return status == SupplierOrderStatus.ORDERED && expectedDeliveryDate.isBefore(LocalDate.now());
	}

}
