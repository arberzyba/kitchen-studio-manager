package de.sedzkitchens.supplierorder;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

@Entity
@Table(name = "supplier_order_items")
@Getter
@Setter
@NoArgsConstructor
public class SupplierOrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_order_id")
	private SupplierOrder supplierOrder;

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

	// Net purchase price per unit in euros, as it was when the order was placed
	private BigDecimal purchasePrice;

	public BigDecimal getLineTotal() {
		return quantity.multiply(purchasePrice).setScale(2, RoundingMode.HALF_UP);
	}

}
