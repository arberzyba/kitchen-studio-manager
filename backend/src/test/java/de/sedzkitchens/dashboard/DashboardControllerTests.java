package de.sedzkitchens.dashboard;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.appointment.AppointmentRequest;
import de.sedzkitchens.appointment.AppointmentService;
import de.sedzkitchens.appointment.AppointmentType;
import de.sedzkitchens.customer.AddressDto;
import de.sedzkitchens.customer.CustomerRequest;
import de.sedzkitchens.customer.CustomerService;
import de.sedzkitchens.customer.Salutation;
import de.sedzkitchens.invoice.Invoice;
import de.sedzkitchens.invoice.InvoiceRepository;
import de.sedzkitchens.invoice.InvoiceRequests;
import de.sedzkitchens.invoice.InvoiceService;
import de.sedzkitchens.order.OrderService;
import de.sedzkitchens.product.ProductCategory;
import de.sedzkitchens.product.ProductRequest;
import de.sedzkitchens.product.ProductService;
import de.sedzkitchens.product.ProductUnit;
import de.sedzkitchens.quote.QuoteRequest;
import de.sedzkitchens.quote.QuoteService;
import de.sedzkitchens.quote.QuoteStatus;
import de.sedzkitchens.supplier.SupplierRequest;
import de.sedzkitchens.supplier.SupplierService;
import de.sedzkitchens.user.Role;
import de.sedzkitchens.user.User;
import de.sedzkitchens.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private SupplierService supplierService;

	@Autowired
	private ProductService productService;

	@Autowired
	private QuoteService quoteService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private AppointmentService appointmentService;

	private Long userId;

	private Long customerId;

	private Long productId;

	// One product at 100.00 net, so a quote for n pieces is n x 100.00 net and n x 119.00 gross
	@BeforeEach
	void createTestData() {
		User sales = new User();
		sales.setEmail("sales@example.com");
		sales.setPasswordHash("irrelevant");
		sales.setFirstName("Lukas");
		sales.setLastName("Weber");
		sales.setRole(Role.SALES);
		userId = userRepository.save(sales).getId();
		customerId = customerService
			.create(new CustomerRequest(Salutation.MS, "Sabine", "Müller", null, null, null,
					new AddressDto("Aachener Straße 112", "50674", "Köln"), null))
			.id();
		Long supplierId = supplierService.create(new SupplierRequest("Rheinland Küchenmöbel GmbH", null, null)).id();
		productId = productService
			.create(new ProductRequest("US-60", "Unterschrank 60 cm", null, ProductCategory.CABINET, ProductUnit.PIECE,
					new BigDecimal("60.00"), new BigDecimal("100.00"), supplierId, true))
			.id();
	}

	@Test
	void openQuotesCountDraftsAndSentQuotesButNotDecidedOnes() throws Exception {
		createQuote(1, QuoteStatus.DRAFT);
		createQuote(1, QuoteStatus.SENT);
		createQuote(2, QuoteStatus.SENT);
		createQuote(5, QuoteStatus.ACCEPTED);
		createQuote(5, QuoteStatus.REJECTED);

		mockMvc.perform(get("/api/dashboard").with(role("SALES")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openQuotes.draftCount").value(1))
			.andExpect(jsonPath("$.openQuotes.sentCount").value(2))
			// 119.00 + 238.00
			.andExpect(jsonPath("$.openQuotes.sentGrossTotal").value(357.00));
	}

	@Test
	void monthlyRevenueIsNetInvoicedPerMonthForTheLastSixMonths() throws Exception {
		invoice(1);
		invoice(3);
		invoice(2).setInvoiceDate(LocalDate.now().minusMonths(2).withDayOfMonth(15));
		// Older than the six months shown
		invoice(7).setInvoiceDate(LocalDate.now().minusMonths(8));

		mockMvc.perform(get("/api/dashboard").with(role("OFFICE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.monthlyRevenue.length()").value(6))
			.andExpect(jsonPath("$.monthlyRevenue[0].month").value(YearMonth.now().minusMonths(5).toString()))
			.andExpect(jsonPath("$.monthlyRevenue[0].netTotal").value(0))
			.andExpect(jsonPath("$.monthlyRevenue[3].month").value(YearMonth.now().minusMonths(2).toString()))
			.andExpect(jsonPath("$.monthlyRevenue[3].netTotal").value(200.00))
			.andExpect(jsonPath("$.monthlyRevenue[4].netTotal").value(0))
			.andExpect(jsonPath("$.monthlyRevenue[5].month").value(YearMonth.now().toString()))
			.andExpect(jsonPath("$.monthlyRevenue[5].netTotal").value(400.00));
	}

	@Test
	void overdueInvoicesAreCountedWithWhatIsStillOwed() throws Exception {
		invoice(1);
		Invoice partlyPaid = invoice(2);
		invoiceService.addPayment(partlyPaid.getId(),
				new InvoiceRequests.AddPayment(new BigDecimal("38.00"), LocalDate.now(), null));
		partlyPaid.setDueDate(LocalDate.now().minusDays(3));
		Invoice longOverdue = invoice(3);
		longOverdue.setDueDate(LocalDate.now().minusDays(20));
		Invoice paidLate = invoice(4);
		invoiceService.addPayment(paidLate.getId(),
				new InvoiceRequests.AddPayment(new BigDecimal("476.00"), LocalDate.now(), null));
		paidLate.setDueDate(LocalDate.now().minusDays(5));

		mockMvc.perform(get("/api/dashboard").with(role("OFFICE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.overdueInvoices.count").value(2))
			// 238.00 - 38.00 + 357.00
			.andExpect(jsonPath("$.overdueInvoices.openAmount").value(557.00))
			.andExpect(jsonPath("$.overdueInvoices.invoices.length()").value(2))
			.andExpect(jsonPath("$.overdueInvoices.invoices[0].id").value(longOverdue.getId()))
			.andExpect(jsonPath("$.overdueInvoices.invoices[1].openAmount").value(200.00));
	}

	@Test
	void upcomingInstallationsListFutureInstallationsSoonestFirst() throws Exception {
		Long orderId = createOrder(1);
		plan(orderId, AppointmentType.INSTALLATION, 10);
		plan(orderId, AppointmentType.INSTALLATION, 3);
		plan(orderId, AppointmentType.INSTALLATION, -2);
		plan(orderId, AppointmentType.MEASUREMENT, 1);

		mockMvc.perform(get("/api/dashboard").with(role("ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.upcomingInstallations.length()").value(2))
			.andExpect(jsonPath("$.upcomingInstallations[0].customerName").value("Sabine Müller"))
			.andExpect(jsonPath("$.upcomingInstallations[0].city").value("Köln"))
			.andExpect(jsonPath("$.upcomingInstallations[0].assigneeName").value("Lukas Weber"))
			.andExpect(jsonPath("$.upcomingInstallations[0].orderNumber").exists());
	}

	@Test
	void emptySystemGivesZeroesAndInstallersHaveNoAccess() throws Exception {
		mockMvc.perform(get("/api/dashboard").with(role("ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openQuotes.sentCount").value(0))
			.andExpect(jsonPath("$.openQuotes.sentGrossTotal").value(0))
			.andExpect(jsonPath("$.monthlyRevenue.length()").value(6))
			.andExpect(jsonPath("$.upcomingInstallations.length()").value(0))
			.andExpect(jsonPath("$.overdueInvoices.count").value(0))
			.andExpect(jsonPath("$.overdueInvoices.openAmount").value(0));

		mockMvc.perform(get("/api/dashboard").with(role("INSTALLER"))).andExpect(status().isForbidden());
	}

	private Long createQuote(int pieces, QuoteStatus status) {
		Long quoteId = quoteService
			.create(new QuoteRequest(customerId, LocalDate.now().plusDays(30), BigDecimal.ZERO, null,
					List.of(new QuoteRequest.Item(productId, new BigDecimal(pieces), new BigDecimal("100.00"),
							BigDecimal.ZERO))),
					userId)
			.id();
		if (status != QuoteStatus.DRAFT) {
			quoteService.changeStatus(quoteId, QuoteStatus.SENT);
		}
		if (status == QuoteStatus.ACCEPTED || status == QuoteStatus.REJECTED) {
			quoteService.changeStatus(quoteId, status);
		}
		return quoteId;
	}

	private Long createOrder(int pieces) {
		return orderService.createFromQuote(createQuote(pieces, QuoteStatus.ACCEPTED), userId).id();
	}

	// Issues an invoice dated today and due in two weeks; tests move the dates on the returned entity
	private Invoice invoice(int pieces) {
		Long id = invoiceService
			.create(new InvoiceRequests.Create(createOrder(pieces), LocalDate.now(), LocalDate.now().plusDays(14)))
			.id();
		return invoiceRepository.findById(id).orElseThrow();
	}

	private void plan(Long orderId, AppointmentType type, int daysFromNow) {
		Instant start = Instant.now().plus(daysFromNow, ChronoUnit.DAYS);
		appointmentService
			.create(new AppointmentRequest(orderId, type, start, start.plus(4, ChronoUnit.HOURS), userId, null));
	}

	private static RequestPostProcessor role(String role) {
		return jwt().jwt(token -> token.subject("999")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

}
