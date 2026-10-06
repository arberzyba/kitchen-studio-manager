package de.sedzkitchens.supplierorder;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

// Office staff and admins place and track supplier orders; sales staff can look them up
@RestController
@RequestMapping("/api/supplier-orders")
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICE')")
@RequiredArgsConstructor
public class SupplierOrderController {

	private final SupplierOrderService supplierOrderService;

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'OFFICE', 'SALES')")
	public Page<SupplierOrderResponse> search(@RequestParam(defaultValue = "") String search,
			@RequestParam(required = false) SupplierOrderStatus status,
			@RequestParam(required = false) Long orderId,
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return supplierOrderService.search(search, status, orderId, pageable);
	}

	// Suppliers a customer order still has to be ordered from
	@GetMapping("/pending")
	@PreAuthorize("hasAnyRole('ADMIN', 'OFFICE', 'SALES')")
	public List<SupplierOrderResponse.Pending> findPending(@RequestParam Long orderId) {
		return supplierOrderService.findPending(orderId);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'OFFICE', 'SALES')")
	public SupplierOrderResponse getById(@PathVariable Long id) {
		return supplierOrderService.getById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SupplierOrderResponse create(@Valid @RequestBody SupplierOrderRequests.Create request) {
		return supplierOrderService.create(request);
	}

	@PutMapping("/{id}")
	public SupplierOrderResponse update(@PathVariable Long id,
			@Valid @RequestBody SupplierOrderRequests.Update request) {
		return supplierOrderService.update(id, request);
	}

	@PostMapping("/{id}/delivered")
	public SupplierOrderResponse markDelivered(@PathVariable Long id,
			@Valid @RequestBody SupplierOrderRequests.Delivered request) {
		return supplierOrderService.markDelivered(id, request);
	}

}
