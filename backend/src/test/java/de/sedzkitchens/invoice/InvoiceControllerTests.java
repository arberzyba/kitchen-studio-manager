package de.sedzkitchens.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import de.sedzkitchens.customer.AddressDto;
import de.sedzkitchens.customer.CustomerRequest;
import de.sedzkitchens.customer.CustomerService;
import de.sedzkitchens.customer.Salutation;
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
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceControllerTests {

	private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
	private InvoiceRepository invoiceRepository;

	@MockitoBean
	private JavaMailSender mailSender;

	private final RequestPostProcessor office = role("OFFICE");

	private Long salesId;

	private Long productId;

	private Long customerId;

	private Long orderId;

	// The order is for 10 x 100.00 = 1000.00 net, 190.00 VAT, 1190.00 gross
	@BeforeEach
	void createTestData() {
		User sales = new User();
		sales.setEmail("sales@example.com");
		sales.setPasswordHash("irrelevant");
		sales.setFirstName("Lukas");
		sales.setLastName("Weber");
		sales.setRole(Role.SALES);
		salesId = userRepository.save(sales).getId();

		Long supplierId = supplierService.create(new SupplierRequest("Rheinland Küchenmöbel GmbH", null, null)).id();
		productId = productService
			.create(new ProductRequest("US-60", "Unterschrank 60 cm", null, ProductCategory.CABINET, ProductUnit.PIECE,
					new BigDecimal("60.00"), new BigDecimal("100.00"), supplierId, true))
			.id();
		customerId = createCustomer("Sabine", "Müller", "sabine@example.de");
		orderId = createOrder(customerId);
	}

	@Test
	void invoiceTakesNumberAmountsAndRecipientFromTheOrder() throws Exception {
		create(orderId, LocalDate.now(), LocalDate.now().plusDays(14)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.invoiceNumber").value("RE-%d-0001".formatted(Year.now().getValue())))
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.overdue").value(false))
			.andExpect(jsonPath("$.invoiceDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.recipientName").value("Frau Sabine Müller"))
			.andExpect(jsonPath("$.grossTotal").value(1190.00))
			.andExpect(jsonPath("$.paidTotal").value(0))
			.andExpect(jsonPath("$.openAmount").value(1190.00))
			.andExpect(jsonPath("$.details.customerEmail").value("sabine@example.de"))
			.andExpect(jsonPath("$.details.quote.netTotal").value(1000.00))
			.andExpect(jsonPath("$.details.quote.vatAmount").value(190.00))
			.andExpect(jsonPath("$.details.quote.items[0].sku").value("US-60"));
	}

	@Test
	void orderIsInvoicedOnceAndNumbersAreSequential() throws Exception {
		create(orderId, LocalDate.now(), LocalDate.now().plusDays(14)).andExpect(status().isCreated());
		create(orderId, LocalDate.now(), LocalDate.now().plusDays(14)).andExpect(status().isConflict());

		Long otherOrder = createOrder(createCustomer("Thomas", "Schmidt", null));
		create(otherOrder, LocalDate.now(), LocalDate.now().plusDays(14))
			.andExpect(jsonPath("$.invoiceNumber").value("RE-%d-0002".formatted(Year.now().getValue())));
	}

	@Test
	void dueDateCannotBeInThePast() throws Exception {
		create(orderId, LocalDate.now(), LocalDate.now().minusDays(1)).andExpect(status().isConflict());
	}

	@Test
	void paymentsReduceTheOpenAmountUntilTheInvoiceIsPaid() throws Exception {
		Long id = issueInvoice();

		pay(id, "400.00").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PARTIALLY_PAID"))
			.andExpect(jsonPath("$.paidTotal").value(400.00))
			.andExpect(jsonPath("$.openAmount").value(790.00))
			.andExpect(jsonPath("$.details.payments.length()").value(1))
			.andExpect(jsonPath("$.details.payments[0].id").isNumber());

		pay(id, "790.00").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PAID"))
			.andExpect(jsonPath("$.openAmount").value(0.00))
			.andExpect(jsonPath("$.details.payments.length()").value(2));
	}

	@Test
	void overpaymentAndInvalidPaymentsAreRejected() throws Exception {
		Long id = issueInvoice();

		pay(id, "1190.01").andExpect(status().isConflict());
		pay(id, "0").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.amount").exists());
		mockMvc
			.perform(post("/api/invoices/" + id + "/payments").with(office)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": 100.00, \"paidOn\": \"%s\"}".formatted(LocalDate.now().plusDays(1))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.paidOn").exists());
	}

	@Test
	void mistakenPaymentCanBeRemoved() throws Exception {
		Long id = issueInvoice();
		String response = pay(id, "1190.00").andExpect(jsonPath("$.status").value("PAID"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		Number paymentId = JsonPath.read(response, "$.details.payments[0].id");

		mockMvc.perform(delete("/api/invoices/" + id + "/payments/" + paymentId).with(office))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.openAmount").value(1190.00))
			.andExpect(jsonPath("$.details.payments.length()").value(0));
		mockMvc.perform(delete("/api/invoices/" + id + "/payments/" + paymentId).with(office))
			.andExpect(status().isNotFound());
	}

	@Test
	void unpaidInvoicePastItsDueDateIsOverdue() throws Exception {
		Long overdueId = issueInvoice();
		Long currentId = issueInvoice(createOrder(createCustomer("Thomas", "Schmidt", null)));
		Long paidLateId = issueInvoice(createOrder(createCustomer("Kim", "Braun", null)));
		// Due dates in the past cannot be entered, so the test moves them directly
		backdateDueDate(overdueId);
		backdateDueDate(paidLateId);
		pay(overdueId, "200.00");
		pay(paidLateId, "1190.00");

		mockMvc.perform(get("/api/invoices/" + overdueId).with(office)).andExpect(jsonPath("$.overdue").value(true));
		mockMvc.perform(get("/api/invoices/" + paidLateId).with(office)).andExpect(jsonPath("$.overdue").value(false));

		mockMvc.perform(get("/api/invoices").with(office).param("overdue", "true"))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(overdueId))
			.andExpect(jsonPath("$.content[0].openAmount").value(990.00));
		mockMvc.perform(get("/api/invoices").with(office)).andExpect(jsonPath("$.content.length()").value(3));
		mockMvc.perform(get("/api/invoices").with(office).param("status", "OPEN"))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(currentId));
		mockMvc.perform(get("/api/invoices").with(office).param("search", "braun"))
			.andExpect(jsonPath("$.content.length()").value(1));
		mockMvc.perform(get("/api/invoices").with(office).param("orderId", orderId.toString()))
			.andExpect(jsonPath("$.content.length()").value(1));
	}

	@Test
	void pdfIsAGermanInvoiceWithTheLegallyRequiredDetails() throws Exception {
		Long id = issueInvoice();

		String text = pdfText(id);
		assertThat(text).contains("Rechnung RE-%d-0001".formatted(Year.now().getValue()))
			.contains("Frau Sabine Müller")
			.contains("Aachener Straße 112")
			.contains("Rechnungsdatum " + GERMAN_DATE.format(LocalDate.now()))
			.contains("Leistungsdatum " + GERMAN_DATE.format(LocalDate.now()))
			.contains("Auftragsnummer AU-")
			.contains("Unterschrank 60 cm")
			.contains("Nettobetrag 1.000,00 €")
			.contains("zzgl. 19 % MwSt.")
			.contains("190,00 €")
			.contains("Gesamtbetrag 1.190,00 €")
			.contains("bis zum " + GERMAN_DATE.format(LocalDate.now().plusDays(14)))
			.contains("Verwendungszweck: RE-")
			.contains("Steuernummer: 214/5800/1234")
			.contains("USt-IdNr.: DE123456789")
			.contains("IBAN: DE02 3705 0198 0001 2345 67");
	}

	@Test
	void issuedInvoiceKeepsItsRecipientWhenTheCustomerMoves() throws Exception {
		Long id = issueInvoice();
		customerService.update(customerId, new CustomerRequest(Salutation.MS, "Sabine", "Müller-Lang", null,
				"sabine@example.de", null, new AddressDto("Neue Straße 1", "10115", "Berlin"), null));

		assertThat(pdfText(id)).contains("Frau Sabine Müller")
			.contains("Aachener Straße 112")
			.doesNotContain("Müller-Lang")
			.doesNotContain("Neue Straße 1");
	}

	@Test
	void invoiceIsEmailedToTheCustomer() throws Exception {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
		Long id = issueInvoice();

		mockMvc.perform(post("/api/invoices/" + id + "/send").with(office)).andExpect(status().isNoContent());

		ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender).send(message.capture());
		assertThat(message.getValue().getRecipients(Message.RecipientType.TO)[0]).hasToString("sabine@example.de");
		assertThat(message.getValue().getSubject())
			.isEqualTo("Ihre Rechnung RE-%d-0001".formatted(Year.now().getValue()));

		Long withoutEmail = issueInvoice(createOrder(createCustomer("Thomas", "Schmidt", null)));
		mockMvc.perform(post("/api/invoices/" + withoutEmail + "/send").with(office)).andExpect(status().isConflict());
	}

	@Test
	void salesCanReadButNotIssueOrRecordPaymentsAndInstallerHasNoAccess() throws Exception {
		Long id = issueInvoice();
		RequestPostProcessor sales = role("SALES");
		String body = "{\"orderId\": %d, \"serviceDate\": \"%s\", \"dueDate\": \"%s\"}"
			.formatted(createOrder(createCustomer("Kim", "Braun", null)), LocalDate.now(), LocalDate.now().plusDays(14));

		mockMvc.perform(get("/api/invoices").with(sales)).andExpect(status().isOk());
		mockMvc.perform(get("/api/invoices/" + id + "/pdf").with(sales)).andExpect(status().isOk());
		mockMvc.perform(post("/api/invoices").with(sales).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isForbidden());
		mockMvc
			.perform(post("/api/invoices/" + id + "/payments").with(sales)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": 100.00, \"paidOn\": \"%s\"}".formatted(LocalDate.now())))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/invoices/" + id + "/send").with(sales)).andExpect(status().isForbidden());

		mockMvc.perform(get("/api/invoices").with(role("INSTALLER"))).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/invoices/" + id + "/pdf").with(role("INSTALLER"))).andExpect(status().isForbidden());
	}

	private Long createCustomer(String firstName, String lastName, String email) {
		return customerService
			.create(new CustomerRequest(Salutation.MS, firstName, lastName, null, email, null,
					new AddressDto("Aachener Straße 112", "50674", "Köln"), null))
			.id();
	}

	private Long createOrder(Long forCustomerId) {
		Long quoteId = quoteService
			.create(new QuoteRequest(forCustomerId, LocalDate.now().plusDays(30), BigDecimal.ZERO, null,
					List.of(new QuoteRequest.Item(productId, BigDecimal.TEN, new BigDecimal("100.00"),
							BigDecimal.ZERO))),
					salesId)
			.id();
		quoteService.changeStatus(quoteId, QuoteStatus.SENT);
		quoteService.changeStatus(quoteId, QuoteStatus.ACCEPTED);
		return orderService.createFromQuote(quoteId, salesId).id();
	}

	private ResultActions create(Long forOrderId, LocalDate serviceDate, LocalDate dueDate) throws Exception {
		return mockMvc.perform(post("/api/invoices").with(office)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"orderId\": %d, \"serviceDate\": \"%s\", \"dueDate\": \"%s\"}".formatted(forOrderId,
					serviceDate, dueDate)));
	}

	private Long issueInvoice() throws Exception {
		return issueInvoice(orderId);
	}

	private Long issueInvoice(Long forOrderId) throws Exception {
		String response = create(forOrderId, LocalDate.now(), LocalDate.now().plusDays(14))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private ResultActions pay(Long invoiceId, String amount) throws Exception {
		return mockMvc.perform(post("/api/invoices/" + invoiceId + "/payments").with(office)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\": %s, \"paidOn\": \"%s\", \"note\": \"Überweisung\"}".formatted(amount,
					LocalDate.now())));
	}

	private void backdateDueDate(Long invoiceId) {
		invoiceRepository.findById(invoiceId).orElseThrow().setDueDate(LocalDate.now().minusDays(3));
	}

	private String pdfText(Long invoiceId) throws Exception {
		byte[] pdf = mockMvc.perform(get("/api/invoices/" + invoiceId + "/pdf").with(office))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		// Collapse line breaks and non-breaking spaces so the assertions do not depend on the PDF layout
		return new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1).replaceAll("[\\s\\h]+", " ");
	}

	private static RequestPostProcessor role(String role) {
		return jwt().jwt(token -> token.subject("999")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

}
