package de.sedzkitchens.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.NotFoundException;
import de.sedzkitchens.supplier.Supplier;
import de.sedzkitchens.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	private final SupplierRepository supplierRepository;

	private final ProductMapper mapper;

	public Page<ProductResponse> search(String search, ProductCategory category, Pageable pageable) {
		String pattern = "%" + search.trim().toLowerCase() + "%";
		return productRepository.search(pattern, category, pageable).map(mapper::toResponse);
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		if (productRepository.existsBySkuIgnoreCase(request.sku())) {
			throw new ConflictException("A product with this article number already exists");
		}
		Product product = new Product();
		mapper.update(product, request);
		product.setSupplier(findSupplier(request.supplierId()));
		return mapper.toResponse(productRepository.save(product));
	}

	@Transactional
	public ProductResponse update(Long id, ProductRequest request) {
		Product product = productRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Product not found"));
		if (productRepository.existsBySkuIgnoreCaseAndIdNot(request.sku(), id)) {
			throw new ConflictException("A product with this article number already exists");
		}
		mapper.update(product, request);
		product.setSupplier(findSupplier(request.supplierId()));
		return mapper.toResponse(product);
	}

	private Supplier findSupplier(Long id) {
		return supplierRepository.findById(id).orElseThrow(() -> new NotFoundException("Supplier not found"));
	}

}
