package de.sedzkitchens.supplier;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Sales and office staff can look suppliers up; only admins maintain them
@RestController
@RequestMapping("/api/suppliers")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OFFICE')")
@RequiredArgsConstructor
public class SupplierController {

	private final SupplierService supplierService;

	@GetMapping
	public List<SupplierResponse> findAll() {
		return supplierService.findAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
		return supplierService.create(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
		return supplierService.update(id, request);
	}

}
