package de.sedzkitchens.customer;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

// Installers have no access to customer data; office staff can read and log contacts but not edit customers
@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OFFICE')")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	@GetMapping
	public Page<CustomerResponse> search(@RequestParam(defaultValue = "") String search,
			@PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
		return customerService.search(search, pageable);
	}

	@GetMapping("/{id}")
	public CustomerResponse getById(@PathVariable Long id) {
		return customerService.getById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
	public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
		return customerService.create(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
	public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
		return customerService.update(id, request);
	}

	@GetMapping("/{id}/contacts")
	public List<ContactResponse> findContacts(@PathVariable Long id) {
		return customerService.findContacts(id);
	}

	@PostMapping("/{id}/contacts")
	@ResponseStatus(HttpStatus.CREATED)
	public ContactResponse addContact(@PathVariable Long id, @Valid @RequestBody ContactRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return customerService.addContact(id, request, Long.valueOf(jwt.getSubject()));
	}

}
