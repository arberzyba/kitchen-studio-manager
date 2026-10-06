package de.sedzkitchens.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

// Sales and admins turn quotes into orders; office staff can also move an order through its steps
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OFFICE')")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	public record CreateOrderRequest(@NotNull Long quoteId) {
	}

	@GetMapping
	public Page<OrderSummaryResponse> search(@RequestParam(defaultValue = "") String search,
			@RequestParam(required = false) OrderStatus status, @RequestParam(required = false) Long quoteId,
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return orderService.search(search, status, quoteId, pageable);
	}

	@GetMapping("/{id}")
	public OrderResponse getById(@PathVariable Long id) {
		return orderService.getById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
	public OrderResponse create(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal Jwt jwt) {
		return orderService.createFromQuote(request.quoteId(), Long.valueOf(jwt.getSubject()));
	}

	@PostMapping("/{id}/advance")
	public OrderResponse advance(@PathVariable Long id) {
		return orderService.advance(id);
	}

}
