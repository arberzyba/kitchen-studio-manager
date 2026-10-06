package de.sedzkitchens.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.appointment.AppointmentRequest;
import de.sedzkitchens.appointment.AppointmentService;
import de.sedzkitchens.appointment.AppointmentType;
import de.sedzkitchens.customer.AddressDto;
import de.sedzkitchens.customer.ContactRequest;
import de.sedzkitchens.customer.ContactType;
import de.sedzkitchens.customer.CustomerContactRepository;
import de.sedzkitchens.customer.CustomerRequest;
import de.sedzkitchens.customer.CustomerService;
import de.sedzkitchens.customer.Salutation;
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
class CustomerDataTests {

	private static final CustomerRequest SABINE = new CustomerRequest(Salutation.MS, "Sabine", "Müller", null,
			"sabine@example.de", "0221 4567890", new AddressDto("Aachener Straße 112", "50674", "Köln"), null);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private CustomerContactRepository contactRepository;

	@Autowired
	private SupplierService supplierService;

	@Autowired
	private ProductService productService;

	@Autowired
	private QuoteService quoteService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private AppointmentService appointmentService;

	@Autowired
	private InvoiceService invoiceService;

	private Long adminId;

	private RequestPostProcessor admin;

	private Long productId;

	private Long customerId;

	@BeforeEach
	void createTestData() {
		User user = new User();
		user.setEmail("admin@example.com");
		user.setPasswordHash("irrelevant");
		user.setFirstName("Anna");
		user.setLastName("Schneider");
		user.setRole(Role.ADMIN);
		adminId = userRepository.save(user).getId();
		admin = jwt().jwt(token -> token.subject(adminId.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

		Long supplierId = supplierService.create(new SupplierRequest("Rheinland Küchenmöbel GmbH", null, null)).id();
		productId = productService
			.create(new ProductRequest("US-60", "Unterschrank 60 cm", null, ProductCategory.CABINET, ProductUnit.PIECE,
					new BigDecimal("60.00"), new BigDecimal("100.00"), supplierId, true))
			.id();
		customerId = customerService.create(SABINE).id();
		customerService.addContact(customerId, new ContactRequest(ContactType.CALL, "Rückruf wegen Aufmaß"), adminId);
	}

	@Test
	void exportContainsEverythingStoredAboutTheCustomerAndIsLogged() throws Exception {
		Long invoiceId = createInvoicedOrder();
		invoiceService.addPayment(invoiceId,
				new InvoiceRequests.AddPayment(new BigDecimal("50.00"), LocalDate.now(), "Anzahlung"));

		mockMvc.perform(get("/api/customers/" + customerId + "/export").with(admin))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("attachment")))
			.andExpect(header().string("Content-Disposition", containsString("customer-" + customerId + "-data.json")))
			.andExpect(jsonPath("$.exportedAt").exists())
			.andExpect(jsonPath("$.customer.lastName").value("Müller"))
			.andExpect(jsonPath("$.customer.email").value("sabine@example.de"))
			.andExpect(jsonPath("$.customer.billingAddress.street").value("Aachener Straße 112"))
			.andExpect(jsonPath("$.contactHistory[0].summary").value("Rückruf wegen Aufmaß"))
			.andExpect(jsonPath("$.quotes.length()").value(1))
			.andExpect(jsonPath("$.quotes[0].items[0].sku").value("US-60"))
			.andExpect(jsonPath("$.orders.length()").value(1))
			.andExpect(jsonPath("$.appointments[0].type").value("MEASUREMENT"))
			.andExpect(jsonPath("$.invoices[0].grossTotal").value(119.00))
			.andExpect(jsonPath("$.invoices[0].details.payments[0].note").value("Anzahlung"));

		mockMvc
			.perform(get("/api/audit-log").with(admin)
				.param("action", "EXPORT")
				.param("entityType", "Customer")
				.param("entityId", customerId.toString()))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].userName").value("Anna Schneider"));
	}

	@Test
	void customerWithoutBusinessDocumentsIsDeletedCompletely() throws Exception {
		mockMvc.perform(delete("/api/customers/" + customerId).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.outcome").value("DELETED"));

		mockMvc.perform(get("/api/customers/" + customerId).with(admin)).andExpect(status().isNotFound());
		assertThat(contactRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId)).isEmpty();
		mockMvc.perform(delete("/api/customers/" + customerId).with(admin)).andExpect(status().isNotFound());
	}

	@Test
	void customerWithBusinessDocumentsIsAnonymizedAndTheDocumentsAreKept() throws Exception {
		Long invoiceId = createInvoicedOrder();

		mockMvc.perform(delete("/api/customers/" + customerId).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.outcome").value("ANONYMIZED"));

		// The personal data is gone from the customer record
		mockMvc.perform(get("/api/customers/" + customerId).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.anonymized").value(true))
			.andExpect(jsonPath("$.firstName").value("Anonymisiert"))
			.andExpect(jsonPath("$.lastName").value("#" + customerId))
			.andExpect(jsonPath("$.email").isEmpty())
			.andExpect(jsonPath("$.phone").isEmpty())
			.andExpect(jsonPath("$.billingAddress.street").value("-"));
		assertThat(contactRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId)).isEmpty();
		mockMvc.perform(get("/api/customers").with(admin).param("search", "müller"))
			.andExpect(jsonPath("$.content.length()").value(0));
		mockMvc.perform(get("/api/customers").with(admin).param("search", "anonymisiert"))
			.andExpect(jsonPath("$.content.length()").value(0));

		// Quotes and orders stay, now without a name or notes
		mockMvc.perform(get("/api/quotes").with(admin))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].customerName").value("Anonymisiert #" + customerId));
		mockMvc.perform(get("/api/quotes/" + firstQuoteId()).with(admin)).andExpect(jsonPath("$.notes").isEmpty());

		// The issued invoice must be retained as it was issued, including its recipient
		byte[] pdf = mockMvc.perform(get("/api/invoices/" + invoiceId + "/pdf").with(admin))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		assertThat(new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1)).contains("Frau Sabine Müller")
			.contains("Aachener Straße 112");

		mockMvc
			.perform(get("/api/audit-log").with(admin)
				.param("action", "ANONYMIZE")
				.param("entityId", customerId.toString()))
			.andExpect(jsonPath("$.content.length()").value(1));
	}

	@Test
	void anonymizedCustomerCannotBeChangedQuotedOrErasedAgain() throws Exception {
		createInvoicedOrder();
		mockMvc.perform(delete("/api/customers/" + customerId).with(admin)).andExpect(status().isOk());

		mockMvc.perform(put("/api/customers/" + customerId).with(admin).contentType(MediaType.APPLICATION_JSON).content("""
				{"salutation": "MS", "firstName": "Sabine", "lastName": "Müller",
				 "billingAddress": {"street": "Aachener Straße 112", "postalCode": "50674", "city": "Köln"}}
				""")).andExpect(status().isConflict());
		mockMvc
			.perform(post("/api/customers/" + customerId + "/contacts").with(admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"contactType\": \"NOTE\", \"summary\": \"Neue Notiz\"}"))
			.andExpect(status().isConflict());
		mockMvc.perform(post("/api/quotes").with(admin).contentType(MediaType.APPLICATION_JSON).content("""
				{"customerId": %d, "validUntil": "2030-01-31", "discountPercent": 0,
				 "items": [{"productId": %d, "quantity": 1, "unitPrice": 100.00, "discountPercent": 0}]}
				""".formatted(customerId, productId))).andExpect(status().isConflict());
		mockMvc.perform(delete("/api/customers/" + customerId).with(admin)).andExpect(status().isConflict());
	}

	@Test
	void onlyAdminsCanExportOrEraseCustomerData() throws Exception {
		for (String role : new String[] { "SALES", "OFFICE", "INSTALLER" }) {
			RequestPostProcessor other = jwt().jwt(token -> token.subject("999"))
				.authorities(new SimpleGrantedAuthority("ROLE_" + role));
			mockMvc.perform(get("/api/customers/" + customerId + "/export").with(other))
				.andExpect(status().isForbidden());
			mockMvc.perform(delete("/api/customers/" + customerId).with(other)).andExpect(status().isForbidden());
		}
		mockMvc.perform(get("/api/customers/" + customerId).with(admin))
			.andExpect(jsonPath("$.lastName").value("Müller"));
	}

	private Long firstQuoteId() {
		return quoteService.search("", null, org.springframework.data.domain.Pageable.unpaged())
			.getContent()
			.getFirst()
			.id();
	}

	// A quote with a note, turned into an order with a measurement appointment and an invoice for 119.00
	private Long createInvoicedOrder() {
		Long quoteId = quoteService
			.create(new QuoteRequest(customerId, LocalDate.now().plusDays(30), BigDecimal.ZERO,
					"Frau Müller wünscht Lieferung am Vormittag.",
					List.of(new QuoteRequest.Item(productId, BigDecimal.ONE, new BigDecimal("100.00"),
							BigDecimal.ZERO))),
					adminId)
			.id();
		quoteService.changeStatus(quoteId, QuoteStatus.SENT);
		quoteService.changeStatus(quoteId, QuoteStatus.ACCEPTED);
		Long orderId = orderService.createFromQuote(quoteId, adminId).id();
		Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
		appointmentService.create(new AppointmentRequest(orderId, AppointmentType.MEASUREMENT, start,
				start.plus(1, ChronoUnit.HOURS), adminId, null));
		return invoiceService
			.create(new InvoiceRequests.Create(orderId, LocalDate.now(), LocalDate.now().plusDays(14)))
			.id();
	}

}
