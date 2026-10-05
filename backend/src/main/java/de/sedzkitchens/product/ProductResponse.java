package de.sedzkitchens.product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String sku, String name, String description, ProductCategory category,
		ProductUnit unit, BigDecimal purchasePrice, BigDecimal sellingPrice, Long supplierId, String supplierName,
		boolean active) {
}
