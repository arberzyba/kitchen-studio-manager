package de.sedzkitchens.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.customer.CustomerRepository;
import de.sedzkitchens.invoice.Invoice;
import de.sedzkitchens.invoice.InvoiceRepository;
import de.sedzkitchens.invoice.InvoiceRequests;
import de.sedzkitchens.invoice.InvoiceResponse;
import de.sedzkitchens.invoice.InvoiceService;
import de.sedzkitchens.order.OrderRepository;
import de.sedzkitchens.order.OrderService;
import de.sedzkitchens.order.OrderStatus;
import de.sedzkitchens.product.Product;
import de.sedzkitchens.product.ProductRepository;
import de.sedzkitchens.quote.Quote;
import de.sedzkitchens.quote.QuoteRepository;
import de.sedzkitchens.quote.QuoteRequest;
import de.sedzkitchens.quote.QuoteService;
import de.sedzkitchens.quote.QuoteStatus;
import de.sedzkitchens.supplierorder.SupplierOrder;
import de.sedzkitchens.supplierorder.SupplierOrderRepository;
import de.sedzkitchens.supplierorder.SupplierOrderRequests;
import de.sedzkitchens.supplierorder.SupplierOrderResponse;
import de.sedzkitchens.supplierorder.SupplierOrderService;
import de.sedzkitchens.supplierorder.SupplierOrderStatus;
import de.sedzkitchens.user.UserRepository;
import lombok.RequiredArgsConstructor;

// Demo data from earlier months: kitchens that were quoted, ordered, delivered, installed and invoiced.
// It gives the dashboard a revenue history and one overdue invoice. Each kitchen goes through the normal
// services; afterwards its dates are moved into the past, which the application itself does not allow.
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoHistorySeeder {

	private final UserRepository userRepository;

	private final CustomerRepository customerRepository;

	private final ProductRepository productRepository;

	private final QuoteRepository quoteRepository;

	private final QuoteService quoteService;

	private final OrderRepository orderRepository;

	private final OrderService orderService;

	private final SupplierOrderRepository supplierOrderRepository;

	private final SupplierOrderService supplierOrderService;

	private final InvoiceRepository invoiceRepository;

	private final InvoiceService invoiceService;

	private Map<String, Product> products;

	// One transaction, so the entities stay attached while their dates are moved into the past
	@Transactional
	public void seed() {
		products = productRepository.findAll().stream().collect(Collectors.toMap(Product::getSku, product -> product));

		// Invoiced 150 to 40 days ago and paid
		completedKitchen("andreas.fischer@example.de", 150, true, "0.00", item("US-60-W", "5"), item("HS-60-W", "4"),
				item("SP-60-W", "1"), item("AP-EI-38", "3.6"), item("EG-BO-60", "1"), item("EG-IK-80", "1"));
		completedKitchen("praxis@dr-wagner.example.de", 125, true, "3.00", item("US-60-W", "2"), item("SP-60-W", "1"),
				item("AP-EI-38", "1.8"), item("EG-GS-60", "1"));
		completedKitchen("stefan.zimmermann@example.de", 110, true, "0.00", item("US-90-W", "3"),
				item("HO-60-W", "2"), item("HS-60-W", "6"), item("AP-QZ-20", "5.1"), item("EG-BO-60", "1"),
				item("EG-IK-80", "1"), item("EG-KS-178", "1"), item("EG-DA-90", "1"));
		completedKitchen("elif.yilmaz@example.de", 85, true, "5.00", item("US-60-W", "4"), item("US-90-W", "1"),
				item("HS-60-W", "3"), item("AP-MA-12", "3.2"), item("EG-IK-80", "1"), item("EG-GS-60", "1"));
		completedKitchen("m.krueger@krueger-hv.example.de", 55, true, "8.00", item("US-60-W", "6"),
				item("SP-60-W", "2"), item("AP-EI-38", "4.4"), item("EG-BO-60", "2"));
		completedKitchen("andreas.fischer@example.de", 40, true, "0.00", item("HO-60-W", "1"),
				item("EG-KS-178", "1"));
		// Invoiced 25 days ago and still unpaid, so it is 11 days overdue
		completedKitchen("praxis@dr-wagner.example.de", 25, false, "0.00", item("US-90-W", "2"), item("HS-60-W", "2"),
				item("AP-QZ-20", "2.4"), item("EG-DA-90", "1"));
	}

	private void completedKitchen(String customerEmail, int invoicedDaysAgo, boolean paid, String discountPercent,
			QuoteRequest.Item... items) {
		Long salesId = userRepository.findByEmail("sales@sedzkitchens.de").orElseThrow().getId();
		Long customerId = customerRepository.findAll()
			.stream()
			.filter(customer -> customerEmail.equals(customer.getEmail()))
			.findFirst()
			.orElseThrow()
			.getId();
		// The usual course of a kitchen: quote, order a week later, goods four weeks after that, then installation
		LocalDate invoiced = LocalDate.now().minusDays(invoicedDaysAgo);
		LocalDate quoted = invoiced.minusDays(42);
		LocalDate ordered = quoted.plusDays(7);
		LocalDate delivered = ordered.plusDays(28);

		Long quoteId = quoteService
			.create(new QuoteRequest(customerId, quoted.plusDays(30), new BigDecimal(discountPercent), null,
					List.of(items)), salesId)
			.id();
		quoteService.changeStatus(quoteId, QuoteStatus.SENT);
		quoteService.changeStatus(quoteId, QuoteStatus.ACCEPTED);
		Quote quote = quoteRepository.findById(quoteId).orElseThrow();
		quote.setQuoteDate(quoted);

		Long orderId = orderService.createFromQuote(quoteId, salesId).id();
		for (SupplierOrderResponse.Pending pending : supplierOrderService.findPending(orderId)) {
			Long supplierOrderId = supplierOrderService
				.create(new SupplierOrderRequests.Create(orderId, pending.supplierId(), delivered, null))
				.id();
			SupplierOrder supplierOrder = supplierOrderRepository.findById(supplierOrderId).orElseThrow();
			supplierOrder.setOrderedDate(ordered.plusDays(1));
			supplierOrder.setActualDeliveryDate(delivered);
			supplierOrder.setStatus(SupplierOrderStatus.DELIVERED);
		}
		var order = orderRepository.findById(orderId).orElseThrow();
		order.setOrderDate(ordered);
		order.setStatus(OrderStatus.COMPLETED);

		// The service only accepts due dates from today on, so the real one is set afterwards
		InvoiceResponse created = invoiceService
			.create(new InvoiceRequests.Create(orderId, invoiced.minusDays(2), LocalDate.now()));
		Invoice invoice = invoiceRepository.findById(created.id()).orElseThrow();
		invoice.setInvoiceDate(invoiced);
		invoice.setDueDate(invoiced.plusDays(14));
		if (paid) {
			invoiceService.addPayment(created.id(), new InvoiceRequests.AddPayment(created.grossTotal(),
					invoiced.plusDays(9), "Überweisung"));
		}
	}

	private QuoteRequest.Item item(String sku, String quantity) {
		Product product = products.get(sku);
		return new QuoteRequest.Item(product.getId(), new BigDecimal(quantity), product.getSellingPrice(),
				BigDecimal.ZERO);
	}

}
