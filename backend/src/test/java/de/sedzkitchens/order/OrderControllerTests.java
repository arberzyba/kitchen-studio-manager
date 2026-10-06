package de.sedzkitchens.order;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.customer.AddressDto;
import de.sedzkitchens.customer.CustomerRequest;
import de.sedzkitchens.customer.CustomerService;
import de.sedzkitchens.customer.Salutation;
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
class OrderControllerTests {

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

	private Long userId;

	private RequestPostProcessor sales;

	private Long customerId;

	private Long productId;

	@BeforeEach
	void createTestData() {
		User user = new User();
		user.setEmail("sales@example.com");
		user.setPasswordHash("irrelevant");
		user.setFirstName("Lukas");
		user.setLastName("Weber");
		user.setRole(Role.SALES);
		userId = userRepository.save(user).getId();
		sales = jwt().jwt(token -> token.subject(userId.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_SALES"));

		customerId = customerService
			.create(new CustomerRequest(Salutation.MS, "Sabine", "Müller", null, null, null,
					new AddressDto("Aachener Straße 112", "50674", "Köln"), null))
			.id();
		Long supplierId = supplierService.create(new SupplierRequest("Rheinland Küchenmöbel GmbH", null, null)).id();
		productId = productService
			.create(new ProductRequest("US-60", "Unterschrank 60 cm", null, ProductCategory.CABINET, ProductUnit.PIECE,
					new BigDecimal("112.00"), new BigDecimal("100.00"), supplierId, true))
			.id();
	}

	@Test
	void acceptedQuoteBecomesOrderWithItsItemsAndTotals() throws Exception {
		Long quoteId = createQuote(QuoteStatus.ACCEPTED);

		createOrder(quoteId).andExpect(status().isCreated())
			.andExpect(jsonPath("$.orderNumber").value("AU-%d-0001".formatted(Year.now().getValue())))
			.andExpect(jsonPath("$.status").value("NEW"))
			.andExpect(jsonPath("$.orderDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.createdByName").value("Lukas Weber"))
			.andExpect(jsonPath("$.quote.id").value(quoteId))
			.andExpect(jsonPath("$.quote.customerName").value("Sabine Müller"))
			.andExpect(jsonPath("$.quote.items[0].sku").value("US-60"))
			.andExpect(jsonPath("$.quote.grossTotal").value(238.00));
	}

	@Test
	void onlyAcceptedQuotesCanBecomeOrders() throws Exception {
		createOrder(createQuote(QuoteStatus.DRAFT)).andExpect(status().isConflict());
		createOrder(createQuote(QuoteStatus.SENT)).andExpect(status().isConflict());
		createOrder(999999L).andExpect(status().isNotFound());
	}

	@Test
	void quoteCanOnlyBecomeOneOrder() throws Exception {
		Long quoteId = createQuote(QuoteStatus.ACCEPTED);

		createOrder(quoteId).andExpect(status().isCreated());
		createOrder(quoteId).andExpect(status().isConflict());
	}

	@Test
	void orderMovesThroughItsStepsInSequenceAndStopsWhenCompleted() throws Exception {
		Long orderId = orderService.createFromQuote(createQuote(QuoteStatus.ACCEPTED), userId).id();

		for (String expected : List.of("MEASURED", "ORDERED_FROM_SUPPLIER", "DELIVERED", "INSTALLED", "COMPLETED")) {
			mockMvc.perform(post("/api/orders/" + orderId + "/advance").with(sales))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(expected));
		}
		mockMvc.perform(post("/api/orders/" + orderId + "/advance").with(sales)).andExpect(status().isConflict());
	}

	@Test
	void listCanBeSearchedAndFilteredByStatusAndQuote() throws Exception {
		Long firstQuote = createQuote(QuoteStatus.ACCEPTED);
		Long firstOrder = orderService.createFromQuote(firstQuote, userId).id();
		orderService.createFromQuote(createQuote(QuoteStatus.ACCEPTED), userId);
		orderService.advance(firstOrder);

		mockMvc.perform(get("/api/orders").with(sales))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].orderNumber").value("AU-%d-0002".formatted(Year.now().getValue())))
			.andExpect(jsonPath("$.content[0].customerName").value("Sabine Müller"))
			.andExpect(jsonPath("$.content[0].grossTotal").value(238.00));
		mockMvc.perform(get("/api/orders").with(sales).param("status", "MEASURED"))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(firstOrder));
		mockMvc.perform(get("/api/orders").with(sales).param("quoteId", firstQuote.toString()))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(firstOrder));
		mockMvc.perform(get("/api/orders").with(sales).param("search", "nobody"))
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void officeCanAdvanceButNotCreateAndInstallerHasNoAccess() throws Exception {
		Long quoteId = createQuote(QuoteStatus.ACCEPTED);
		RequestPostProcessor office = role("OFFICE");

		mockMvc
			.perform(post("/api/orders").with(office)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"quoteId\": %d}".formatted(quoteId)))
			.andExpect(status().isForbidden());

		Long orderId = orderService.createFromQuote(quoteId, userId).id();
		mockMvc.perform(get("/api/orders/" + orderId).with(office)).andExpect(status().isOk());
		mockMvc.perform(post("/api/orders/" + orderId + "/advance").with(office)).andExpect(status().isOk());

		mockMvc.perform(get("/api/orders").with(role("INSTALLER"))).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/orders/" + orderId + "/advance").with(role("INSTALLER")))
			.andExpect(status().isForbidden());
	}

	// Creates a quote for 2 x 100.00 (238.00 gross) and moves it to the given status
	private Long createQuote(QuoteStatus status) {
		Long quoteId = quoteService
			.create(new QuoteRequest(customerId, LocalDate.now().plusDays(30), BigDecimal.ZERO, null,
					List.of(new QuoteRequest.Item(productId, new BigDecimal("2"), new BigDecimal("100.00"),
							BigDecimal.ZERO))),
					userId)
			.id();
		if (status != QuoteStatus.DRAFT) {
			quoteService.changeStatus(quoteId, QuoteStatus.SENT);
		}
		if (status == QuoteStatus.ACCEPTED) {
			quoteService.changeStatus(quoteId, QuoteStatus.ACCEPTED);
		}
		return quoteId;
	}

	private ResultActions createOrder(Long quoteId) throws Exception {
		return mockMvc.perform(post("/api/orders").with(sales)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"quoteId\": %d}".formatted(quoteId)));
	}

	private RequestPostProcessor role(String role) {
		return jwt().jwt(token -> token.subject("999")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

}
