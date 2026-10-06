package de.sedzkitchens.order;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.DocumentNumberService;
import de.sedzkitchens.common.NotFoundException;
import de.sedzkitchens.quote.Quote;
import de.sedzkitchens.quote.QuoteRepository;
import de.sedzkitchens.quote.QuoteStatus;
import de.sedzkitchens.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	// "AU" for Auftrag, giving numbers like AU-2026-0001
	private static final String NUMBER_PREFIX = "AU";

	private final OrderRepository orderRepository;

	private final QuoteRepository quoteRepository;

	private final UserRepository userRepository;

	private final DocumentNumberService documentNumberService;

	public Page<OrderSummaryResponse> search(String search, OrderStatus status, Long quoteId, Pageable pageable) {
		String pattern = "%" + search.trim().toLowerCase() + "%";
		return orderRepository.search(pattern, status, quoteId, pageable).map(OrderSummaryResponse::from);
	}

	public OrderResponse getById(Long id) {
		return OrderResponse.from(findOrder(id));
	}

	@Transactional
	public OrderResponse createFromQuote(Long quoteId, Long userId) {
		Quote quote = quoteRepository.findById(quoteId).orElseThrow(() -> new NotFoundException("Quote not found"));
		if (quote.getStatus() != QuoteStatus.ACCEPTED) {
			throw new ConflictException("Only accepted quotes can become orders");
		}
		if (orderRepository.existsByQuoteId(quoteId)) {
			throw new ConflictException("An order already exists for this quote");
		}
		SalesOrder order = new SalesOrder();
		order.setOrderNumber(documentNumberService.next(NUMBER_PREFIX));
		order.setQuote(quote);
		order.setOrderDate(LocalDate.now());
		order.setCreatedBy(userRepository.getReferenceById(userId));
		return OrderResponse.from(orderRepository.save(order));
	}

	// Moves the order one step forward; steps cannot be skipped or undone
	@Transactional
	public OrderResponse advance(Long id) {
		SalesOrder order = findOrder(id);
		order.setStatus(order.getStatus()
			.next()
			.orElseThrow(() -> new ConflictException("The order is already completed")));
		return OrderResponse.from(order);
	}

	private SalesOrder findOrder(Long id) {
		return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
	}

}
