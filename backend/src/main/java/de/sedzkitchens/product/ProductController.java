package de.sedzkitchens.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Sales and office staff can browse the catalog; only admins maintain it
@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OFFICE')")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@GetMapping
	public Page<ProductResponse> search(@RequestParam(defaultValue = "") String search,
			@RequestParam(required = false) ProductCategory category,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return productService.search(search, category, pageable);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public ProductResponse create(@Valid @RequestBody ProductRequest request) {
		return productService.create(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return productService.update(id, request);
	}

}
