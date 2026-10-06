package de.sedzkitchens.supplierorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.DocumentNumberService;
import de.sedzkitchens.common.NotFoundException;
import de.sedzkitchens.order.OrderRepository;
import de.sedzkitchens.order.SalesOrder;
import de.sedzkitchens.quote.QuoteItem;
import de.sedzkitchens.supplier.Supplier;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierOrderService {

	// "BE" for Bestellung, giving numbers like BE-2026-0001
	private static final String NUMBER_PREFIX = "BE";

	private final SupplierOrderRepository supplierOrderRepository;

	private final OrderRepository orderRepository;

	private final DocumentNumberService documentNumberService;

	public Page<SupplierOrderResponse> search(String search, SupplierOrderStatus status, Long salesOrderId,
			Pageable pageable) {
		String pattern = "%" + search.trim().toLowerCase() + "%";
		return supplierOrderRepository.search(pattern, status, salesOrderId, pageable)
			.map(SupplierOrderResponse::summary);
	}

	public SupplierOrderResponse getById(Long id) {
		return SupplierOrderResponse.from(findSupplierOrder(id));
	}

	// The suppliers a customer order still has to be ordered from
	public List<SupplierOrderResponse.Pending> findPending(Long salesOrderId) {
		Set<Long> alreadyOrdered = supplierOrderRepository.findBySalesOrderId(salesOrderId)
			.stream()
			.map(order -> order.getSupplier().getId())
			.collect(Collectors.toSet());
		Map<Supplier, Integer> itemCounts = new LinkedHashMap<>();
		for (QuoteItem item : findSalesOrder(salesOrderId).getQuote().getItems()) {
			itemCounts.merge(item.getProduct().getSupplier(), 1, Integer::sum);
		}
		return itemCounts.entrySet()
			.stream()
			.filter(entry -> !alreadyOrdered.contains(entry.getKey().getId()))
			.map(entry -> new SupplierOrderResponse.Pending(entry.getKey().getId(), entry.getKey().getName(),
					entry.getValue()))
			.toList();
	}

	@Transactional
	public SupplierOrderResponse create(SupplierOrderRequests.Create request) {
		SalesOrder salesOrder = findSalesOrder(request.orderId());
		List<QuoteItem> items = salesOrder.getQuote()
			.getItems()
			.stream()
			.filter(item -> item.getProduct().getSupplier().getId().equals(request.supplierId()))
			.toList();
		if (items.isEmpty()) {
			throw new ConflictException("This order has no items from that supplier");
		}
		boolean alreadyOrdered = supplierOrderRepository.findBySalesOrderId(salesOrder.getId())
			.stream()
			.anyMatch(existing -> existing.getSupplier().getId().equals(request.supplierId()));
		if (alreadyOrdered) {
			throw new ConflictException("This supplier has already been ordered from for this order");
		}

		SupplierOrder supplierOrder = new SupplierOrder();
		supplierOrder.setOrderNumber(documentNumberService.next(NUMBER_PREFIX));
		supplierOrder.setSalesOrder(salesOrder);
		supplierOrder.setSupplier(items.getFirst().getProduct().getSupplier());
		supplierOrder.setOrderedDate(LocalDate.now());
		supplierOrder.setExpectedDeliveryDate(request.expectedDeliveryDate());
		supplierOrder.setNotes(request.notes());
		for (QuoteItem quoteItem : items) {
			SupplierOrderItem item = new SupplierOrderItem();
			item.setSupplierOrder(supplierOrder);
			item.setProduct(quoteItem.getProduct());
			item.setPosition(supplierOrder.getItems().size() + 1);
			item.setSku(quoteItem.getSku());
			item.setDescription(quoteItem.getDescription());
			item.setUnit(quoteItem.getUnit());
			item.setQuantity(quoteItem.getQuantity());
			// The customer pays the quoted price; the supplier is paid the product's current purchase price
			item.setPurchasePrice(quoteItem.getProduct().getPurchasePrice());
			supplierOrder.getItems().add(item);
		}
		supplierOrder.setTotal(supplierOrder.getItems()
			.stream()
			.map(SupplierOrderItem::getLineTotal)
			.reduce(BigDecimal.ZERO, BigDecimal::add));
		return SupplierOrderResponse.from(supplierOrderRepository.save(supplierOrder));
	}

	@Transactional
	public SupplierOrderResponse update(Long id, SupplierOrderRequests.Update request) {
		SupplierOrder supplierOrder = findSupplierOrder(id);
		if (supplierOrder.getStatus() == SupplierOrderStatus.DELIVERED) {
			throw new ConflictException("A delivered supplier order can no longer be changed");
		}
		supplierOrder.setExpectedDeliveryDate(request.expectedDeliveryDate());
		supplierOrder.setNotes(request.notes());
		return SupplierOrderResponse.from(supplierOrder);
	}

	@Transactional
	public SupplierOrderResponse markDelivered(Long id, SupplierOrderRequests.Delivered request) {
		SupplierOrder supplierOrder = findSupplierOrder(id);
		if (supplierOrder.getStatus() == SupplierOrderStatus.DELIVERED) {
			throw new ConflictException("This supplier order has already been delivered");
		}
		if (request.actualDeliveryDate().isBefore(supplierOrder.getOrderedDate())) {
			throw new ConflictException("The delivery date cannot be before the order date");
		}
		supplierOrder.setStatus(SupplierOrderStatus.DELIVERED);
		supplierOrder.setActualDeliveryDate(request.actualDeliveryDate());
		return SupplierOrderResponse.from(supplierOrder);
	}

	private SupplierOrder findSupplierOrder(Long id) {
		return supplierOrderRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Supplier order not found"));
	}

	private SalesOrder findSalesOrder(Long id) {
		return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
	}

}
