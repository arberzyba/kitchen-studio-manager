package de.sedzkitchens.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Year;

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import de.sedzkitchens.customer.AddressDto;
import de.sedzkitchens.customer.CustomerRequest;
import de.sedzkitchens.customer.CustomerService;
import de.sedzkitchens.customer.Salutation;
import de.sedzkitchens.product.ProductCategory;
import de.sedzkitchens.product.ProductRequest;
import de.sedzkitchens.product.ProductService;
import de.sedzkitchens.product.ProductUnit;
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
class QuoteControllerTests {

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

	@MockitoBean
	private JavaMailSender mailSender;

	private RequestPostProcessor sales;

	private Long customerId;

	private Long cabinetId;

	private Long worktopId;

	@BeforeEach
	void createTestData() {
		User user = new User();
		user.setEmail("sales@example.com");
		user.setPasswordHash("irrelevant");
		user.setFirstName("Lukas");
		user.setLastName("Weber");
		user.setRole(Role.SALES);
		userRepository.save(user);
		sales = jwt().jwt(token -> token.subject(user.getId().toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_SALES"));

		customerId = createCustomer("sabine@example.de");
		Long supplierId = supplierService.create(new SupplierRequest("Rheinland Küchenmöbel GmbH", null, null)).id();
		cabinetId = productService
			.create(new ProductRequest("US-60", "Unterschrank 60 cm", null, ProductCategory.CABINET, ProductUnit.PIECE,
					new BigDecimal("112.00"), new BigDecimal("189.00"), supplierId, true))
			.id();
		worktopId = productService
			.create(new ProductRequest("AP-QZ", "Arbeitsplatte Quarzstein", null, ProductCategory.WORKTOP,
					ProductUnit.METER, new BigDecimal("210.00"), new BigDecimal("369.00"), supplierId, true))
			.id();
	}

	@Test
	void createdQuoteGetsNumberAndCalculatedTotals() throws Exception {
		// 4 x 189.00 = 756.00; 4.2 m x 369.00 less 10 % = 1394.82; subtotal 2150.82
		// 5 % discount = 107.54; net 2043.28; 19 % VAT = 388.22; gross 2431.50
		mockMvc.perform(post("/api/quotes").with(sales).contentType(MediaType.APPLICATION_JSON).content(quoteJson("5.00")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.quoteNumber").value("AN-%d-0001".formatted(Year.now().getValue())))
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.customerName").value("Sabine Müller"))
			.andExpect(jsonPath("$.createdByName").value("Lukas Weber"))
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.items[0].sku").value("US-60"))
			.andExpect(jsonPath("$.items[0].description").value("Unterschrank 60 cm"))
			.andExpect(jsonPath("$.items[0].lineTotal").value(756.00))
			.andExpect(jsonPath("$.items[1].unit").value("METER"))
			.andExpect(jsonPath("$.items[1].lineTotal").value(1394.82))
			.andExpect(jsonPath("$.subtotal").value(2150.82))
			.andExpect(jsonPath("$.discountAmount").value(107.54))
			.andExpect(jsonPath("$.netTotal").value(2043.28))
			.andExpect(jsonPath("$.vatRate").value(19.00))
			.andExpect(jsonPath("$.vatAmount").value(388.22))
			.andExpect(jsonPath("$.grossTotal").value(2431.50));
	}

	@Test
	void quoteNumbersAreSequential() throws Exception {
		createQuote();

		mockMvc.perform(post("/api/quotes").with(sales).contentType(MediaType.APPLICATION_JSON).content(quoteJson("0")))
			.andExpect(jsonPath("$.quoteNumber").value("AN-%d-0002".formatted(Year.now().getValue())));
	}

	@Test
	void quoteKeepsItsPricesWhenTheCatalogChanges() throws Exception {
		Long id = createQuote();
		productService.update(cabinetId,
				new ProductRequest("US-60", "Unterschrank 60 cm NEU", null, ProductCategory.CABINET, ProductUnit.PIECE,
						new BigDecimal("150.00"), new BigDecimal("299.00"), supplierService.findAll().getFirst().id(),
						true));

		mockMvc.perform(get("/api/quotes/" + id).with(sales))
			.andExpect(jsonPath("$.items[0].description").value("Unterschrank 60 cm"))
			.andExpect(jsonPath("$.items[0].unitPrice").value(189.00));
	}

	@Test
	void draftCanBeEditedButSentQuoteCannot() throws Exception {
		Long id = createQuote();

		mockMvc.perform(put("/api/quotes/" + id).with(sales).contentType(MediaType.APPLICATION_JSON).content("""
				{"customerId": %d, "validUntil": "2030-01-31", "discountPercent": 0,
				 "items": [{"productId": %d, "quantity": 1, "unitPrice": 100.00, "discountPercent": 0}]}
				""".formatted(customerId, cabinetId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.grossTotal").value(119.00));

		changeStatus(id, "SENT").andExpect(status().isOk());
		mockMvc.perform(put("/api/quotes/" + id).with(sales).contentType(MediaType.APPLICATION_JSON).content(quoteJson("0")))
			.andExpect(status().isConflict());
	}

	@Test
	void statusFollowsDraftSentAccepted() throws Exception {
		Long id = createQuote();

		changeStatus(id, "ACCEPTED").andExpect(status().isConflict());
		changeStatus(id, "SENT").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));
		changeStatus(id, "ACCEPTED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
		changeStatus(id, "REJECTED").andExpect(status().isConflict());
	}

	@Test
	void invalidQuotesAreRejected() throws Exception {
		mockMvc.perform(post("/api/quotes").with(sales).contentType(MediaType.APPLICATION_JSON).content("""
				{"customerId": %d, "validUntil": "2030-01-31", "discountPercent": 120, "items": []}
				""".formatted(customerId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.discountPercent").exists())
			.andExpect(jsonPath("$.errors.items").exists());

		mockMvc.perform(post("/api/quotes").with(sales).contentType(MediaType.APPLICATION_JSON).content("""
				{"customerId": %d, "validUntil": "2030-01-31", "discountPercent": 0,
				 "items": [{"productId": %d, "quantity": 0, "unitPrice": 100.00, "discountPercent": 0}]}
				""".formatted(customerId, cabinetId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors['items[0].quantity']").exists());
	}

	@Test
	void listCanBeSearchedAndFilteredByStatus() throws Exception {
		Long first = createQuote();
		createQuote();
		changeStatus(first, "SENT");

		mockMvc.perform(get("/api/quotes").with(sales))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].quoteNumber").value("AN-%d-0002".formatted(Year.now().getValue())))
			.andExpect(jsonPath("$.content[0].grossTotal").value(2431.50));
		mockMvc.perform(get("/api/quotes").with(sales).param("status", "SENT"))
			.andExpect(jsonPath("$.content.length()").value(1));
		mockMvc.perform(get("/api/quotes").with(sales).param("search", "müller"))
			.andExpect(jsonPath("$.content.length()").value(2));
		mockMvc.perform(get("/api/quotes").with(sales).param("search", "nobody"))
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void pdfContainsNumberCustomerAndGermanTotals() throws Exception {
		Long id = createQuote();

		byte[] pdf = mockMvc.perform(get("/api/quotes/" + id + "/pdf").with(sales))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		// Collapse line breaks and non-breaking spaces so the assertions do not depend on the PDF layout
		String text = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1).replaceAll("[\\s\\h]+", " ");
		assertThat(text).contains("Angebot AN-%d-0001".formatted(Year.now().getValue()))
			.contains("Frau Sabine Müller")
			.contains("50674 Köln")
			.contains("Unterschrank 60 cm")
			.contains("4,2 m")
			.contains("2.150,82 €")
			.contains("Rabatt 5 %")
			.contains("zzgl. 19 % MwSt.")
			.contains("388,22 €")
			.contains("2.431,50 €")
			.contains("SedzKitchens GmbH")
			.contains("USt-IdNr.: DE123456789");
	}

	@Test
	void sendingEmailsThePdfAndMarksTheQuoteSent() throws Exception {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
		Long id = createQuote();

		mockMvc.perform(post("/api/quotes/" + id + "/send").with(sales))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SENT"));

		ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender).send(message.capture());
		assertThat(message.getValue().getRecipients(Message.RecipientType.TO)[0]).hasToString("sabine@example.de");
		assertThat(message.getValue().getSubject()).isEqualTo("Ihr Angebot AN-%d-0001".formatted(Year.now().getValue()));
	}

	@Test
	void quoteCannotBeSentWithoutCustomerEmail() throws Exception {
		customerId = createCustomer(null);
		Long id = createQuote();

		mockMvc.perform(post("/api/quotes/" + id + "/send").with(sales)).andExpect(status().isConflict());
		verify(mailSender, never()).send(any(MimeMessage.class));
	}

	@Test
	void officeCanReadButNotWriteAndInstallerHasNoAccess() throws Exception {
		Long id = createQuote();
		RequestPostProcessor office = role("OFFICE");

		mockMvc.perform(get("/api/quotes").with(office)).andExpect(status().isOk());
		mockMvc.perform(get("/api/quotes/" + id + "/pdf").with(office)).andExpect(status().isOk());
		mockMvc.perform(post("/api/quotes").with(office).contentType(MediaType.APPLICATION_JSON).content(quoteJson("0")))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/quotes/" + id + "/send").with(office)).andExpect(status().isForbidden());

		mockMvc.perform(get("/api/quotes").with(role("INSTALLER"))).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/quotes/" + id).with(role("INSTALLER"))).andExpect(status().isForbidden());
	}

	private Long createCustomer(String email) {
		return customerService
			.create(new CustomerRequest(Salutation.MS, "Sabine", "Müller", null, email, null,
					new AddressDto("Aachener Straße 112", "50674", "Köln"), null))
			.id();
	}

	private Long createQuote() throws Exception {
		String response = mockMvc
			.perform(post("/api/quotes").with(sales).contentType(MediaType.APPLICATION_JSON).content(quoteJson("5.00")))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private org.springframework.test.web.servlet.ResultActions changeStatus(Long id, String status) throws Exception {
		return mockMvc.perform(post("/api/quotes/" + id + "/status").with(sales)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"%s\"}".formatted(status)));
	}

	private String quoteJson(String discountPercent) {
		return """
				{"customerId": %d, "validUntil": "2030-01-31", "discountPercent": %s, "notes": "Lieferung in 6 Wochen",
				 "items": [
				   {"productId": %d, "quantity": 4, "unitPrice": 189.00, "discountPercent": 0},
				   {"productId": %d, "quantity": 4.2, "unitPrice": 369.00, "discountPercent": 10}
				 ]}
				""".formatted(customerId, discountPercent, cabinetId, worktopId);
	}

	private RequestPostProcessor role(String role) {
		return jwt().jwt(token -> token.subject("999")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

}
