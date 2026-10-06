package de.sedzkitchens.supplierorder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class SupplierOrderControllerTests {

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
	private SupplierOrderService supplierOrderService;

	private final RequestPostProcessor office = role("OFFICE");

	private Long cabinetSupplierId;

	private Long applianceSupplierId;

	private Long unusedSupplierId;

	private Long orderId;

	// The customer order contains 4 cabinets and 2.5 m of plinth from one supplier and 1 oven from another
	@BeforeEach
	void createTestData() {
		User sales = new User();
		sales.setEmail("sales@example.com");
		sales.setPasswordHash("irrelevant");
		sales.setFirstName("Lukas");
		sales.setLastName("Weber");
		sales.setRole(Role.SALES);
		Long salesId = userRepository.save(sales).getId();

		Long customerId = customerService
			.create(new CustomerRequest(Salutation.MS, "Sabine", "Müller", null, null, null,
					new AddressDto("Aachener Straße 112", "50674", "Köln"), null))
			.id();
		cabinetSupplierId = supplierService.create(new SupplierRequest("Rheinland Küchenmöbel GmbH", null, null)).id();
		applianceSupplierId = supplierService.create(new SupplierRequest("Hausgeräte Nord GmbH", null, null)).id();
		unusedSupplierId = supplierService.create(new SupplierRequest("Westfalen Arbeitsplatten KG", null, null)).id();
		Long cabinet = createProduct("US-60", "Unterschrank 60 cm", ProductUnit.PIECE, "112.00", cabinetSupplierId);
		Long plinth = createProduct("SO-150", "Sockelblende", ProductUnit.METER, "9.90", cabinetSupplierId);
		Long oven = createProduct("EG-BO", "Einbaubackofen", ProductUnit.PIECE, "389.00", applianceSupplierId);

		Long quoteId = quoteService
			.create(new QuoteRequest(customerId, LocalDate.now().plusDays(30), BigDecimal.ZERO, null,
					List.of(quoteItem(cabinet, "4"), quoteItem(plinth, "2.5"), quoteItem(oven, "1"))), salesId)
			.id();
		quoteService.changeStatus(quoteId, QuoteStatus.SENT);
		quoteService.changeStatus(quoteId, QuoteStatus.ACCEPTED);
		orderId = orderService.createFromQuote(quoteId, salesId).id();
	}

	@Test
	void supplierOrderContainsThatSuppliersItemsAtPurchasePrices() throws Exception {
		// 4 x 112.00 + 2.5 x 9.90 = 448.00 + 24.75 = 472.75
		create(cabinetSupplierId, LocalDate.now().plusDays(14)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.orderNumber").value("BE-%d-0001".formatted(Year.now().getValue())))
			.andExpect(jsonPath("$.status").value("ORDERED"))
			.andExpect(jsonPath("$.overdue").value(false))
			.andExpect(jsonPath("$.supplierName").value("Rheinland Küchenmöbel GmbH"))
			.andExpect(jsonPath("$.customerName").value("Sabine Müller"))
			.andExpect(jsonPath("$.orderedDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.actualDeliveryDate").isEmpty())
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.items[0].sku").value("US-60"))
			.andExpect(jsonPath("$.items[0].quantity").value(4))
			.andExpect(jsonPath("$.items[0].purchasePrice").value(112.00))
			.andExpect(jsonPath("$.items[1].lineTotal").value(24.75))
			.andExpect(jsonPath("$.total").value(472.75));
	}

	@Test
	void pendingListsSuppliersNotYetOrderedFrom() throws Exception {
		mockMvc.perform(get("/api/supplier-orders/pending").with(office).param("orderId", orderId.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].supplierName").value("Rheinland Küchenmöbel GmbH"))
			.andExpect(jsonPath("$[0].itemCount").value(2));

		order(cabinetSupplierId, LocalDate.now().plusDays(14));

		mockMvc.perform(get("/api/supplier-orders/pending").with(office).param("orderId", orderId.toString()))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].supplierName").value("Hausgeräte Nord GmbH"));
	}

	@Test
	void supplierCanOnlyBeOrderedFromOncePerOrderAndOnlyIfItHasItems() throws Exception {
		order(cabinetSupplierId, LocalDate.now().plusDays(14));

		create(cabinetSupplierId, LocalDate.now().plusDays(20)).andExpect(status().isConflict());
		create(unusedSupplierId, LocalDate.now().plusDays(20)).andExpect(status().isConflict());
	}

	@Test
	void deliveryIsRecordedOnce() throws Exception {
		Long id = order(cabinetSupplierId, LocalDate.now().plusDays(14));

		markDelivered(id, LocalDate.now()).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("DELIVERED"))
			.andExpect(jsonPath("$.actualDeliveryDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.overdue").value(false));
		markDelivered(id, LocalDate.now()).andExpect(status().isConflict());
	}

	@Test
	void deliveryDateMustBePlausible() throws Exception {
		Long id = order(cabinetSupplierId, LocalDate.now().plusDays(14));

		markDelivered(id, LocalDate.now().plusDays(1)).andExpect(status().isBadRequest());
		markDelivered(id, LocalDate.now().minusDays(1)).andExpect(status().isConflict());
	}

	@Test
	void expectedDateCanBeMovedUntilDeliveryAndPastDatesAreOverdue() throws Exception {
		Long id = order(cabinetSupplierId, LocalDate.now().plusDays(14));

		mockMvc.perform(put("/api/supplier-orders/" + id).with(office).contentType(MediaType.APPLICATION_JSON).content("""
				{"expectedDeliveryDate": "%s", "notes": "Lieferverzug laut Hersteller"}
				""".formatted(LocalDate.now().minusDays(2))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.overdue").value(true))
			.andExpect(jsonPath("$.notes").value("Lieferverzug laut Hersteller"));

		supplierOrderService.markDelivered(id, new SupplierOrderRequests.Delivered(LocalDate.now()));
		mockMvc.perform(put("/api/supplier-orders/" + id).with(office).contentType(MediaType.APPLICATION_JSON).content("""
				{"expectedDeliveryDate": "%s"}
				""".formatted(LocalDate.now().plusDays(3)))).andExpect(status().isConflict());
	}

	@Test
	void listCanBeSearchedAndFiltered() throws Exception {
		Long first = order(cabinetSupplierId, LocalDate.now().plusDays(14));
		order(applianceSupplierId, LocalDate.now().plusDays(7));
		supplierOrderService.markDelivered(first, new SupplierOrderRequests.Delivered(LocalDate.now()));

		mockMvc.perform(get("/api/supplier-orders").with(office))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].supplierName").value("Hausgeräte Nord GmbH"))
			.andExpect(jsonPath("$.content[0].total").value(389.00));
		mockMvc.perform(get("/api/supplier-orders").with(office).param("status", "DELIVERED"))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(first));
		mockMvc.perform(get("/api/supplier-orders").with(office).param("search", "rheinland"))
			.andExpect(jsonPath("$.content.length()").value(1));
		mockMvc.perform(get("/api/supplier-orders").with(office).param("orderId", orderId.toString()))
			.andExpect(jsonPath("$.content.length()").value(2));
		mockMvc.perform(get("/api/supplier-orders").with(office).param("orderId", "999999"))
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void salesCanReadButNotOrderAndInstallerHasNoAccess() throws Exception {
		Long id = order(cabinetSupplierId, LocalDate.now().plusDays(14));
		RequestPostProcessor sales = role("SALES");
		String body = """
				{"orderId": %d, "supplierId": %d, "expectedDeliveryDate": "%s"}
				""".formatted(orderId, applianceSupplierId, LocalDate.now().plusDays(7));

		mockMvc.perform(get("/api/supplier-orders/" + id).with(sales)).andExpect(status().isOk());
		mockMvc.perform(get("/api/supplier-orders/pending").with(sales).param("orderId", orderId.toString()))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/supplier-orders").with(sales).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isForbidden());
		mockMvc
			.perform(post("/api/supplier-orders/" + id + "/delivered").with(sales)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"actualDeliveryDate\": \"%s\"}".formatted(LocalDate.now())))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/supplier-orders").with(role("INSTALLER"))).andExpect(status().isForbidden());
	}

	private Long createProduct(String sku, String name, ProductUnit unit, String purchasePrice, Long supplierId) {
		return productService
			.create(new ProductRequest(sku, name, null, ProductCategory.CABINET, unit, new BigDecimal(purchasePrice),
					new BigDecimal("999.00"), supplierId, true))
			.id();
	}

	private QuoteRequest.Item quoteItem(Long productId, String quantity) {
		return new QuoteRequest.Item(productId, new BigDecimal(quantity), new BigDecimal("999.00"), BigDecimal.ZERO);
	}

	private Long order(Long supplierId, LocalDate expected) {
		return supplierOrderService.create(new SupplierOrderRequests.Create(orderId, supplierId, expected, null)).id();
	}

	private ResultActions create(Long supplierId, LocalDate expected) throws Exception {
		return mockMvc.perform(post("/api/supplier-orders").with(office).contentType(MediaType.APPLICATION_JSON).content("""
				{"orderId": %d, "supplierId": %d, "expectedDeliveryDate": "%s"}
				""".formatted(orderId, supplierId, expected)));
	}

	private ResultActions markDelivered(Long id, LocalDate date) throws Exception {
		return mockMvc.perform(post("/api/supplier-orders/" + id + "/delivered").with(office)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"actualDeliveryDate\": \"%s\"}".formatted(date)));
	}

	private static RequestPostProcessor role(String role) {
		return jwt().jwt(token -> token.subject("999")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

}
