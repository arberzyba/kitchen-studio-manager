package de.sedzkitchens.product;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import de.sedzkitchens.supplier.Supplier;
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
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String sku;

	private String name;

	private String description;

	@Enumerated(EnumType.STRING)
	private ProductCategory category;

	@Enumerated(EnumType.STRING)
	private ProductUnit unit;

	// Net prices in euros
	private BigDecimal purchasePrice;

	private BigDecimal sellingPrice;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id")
	private Supplier supplier;

	// Discontinued products are deactivated, not deleted, so existing quotes keep their reference
	private boolean active = true;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

}
