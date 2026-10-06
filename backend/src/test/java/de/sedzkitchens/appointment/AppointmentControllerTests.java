package de.sedzkitchens.appointment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
class AppointmentControllerTests {

	private static final String DAY = "2030-03-12T00:00:00Z";

	private static final String NEXT_DAY = "2030-03-13T00:00:00Z";

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
	private AppointmentService appointmentService;

	private User sales;

	private User installer;

	private User otherInstaller;

	private Long orderId;

	@BeforeEach
	void createTestData() {
		sales = createUser("sales@example.com", "Lukas", "Weber", Role.SALES);
		installer = createUser("installer@example.com", "Jonas", "Becker", Role.INSTALLER);
		otherInstaller = createUser("other@example.com", "Mia", "Vogel", Role.INSTALLER);

		Long customerId = customerService
			.create(new CustomerRequest(Salutation.MR, "Thomas", "Schmidt", null, null, "0211 9876543",
					new AddressDto("Kaiserswerther Straße 45", "40477", "Düsseldorf"),
					new AddressDto("Am Rheinufer 8", "40545", "Düsseldorf")))
			.id();
		Long supplierId = supplierService.create(new SupplierRequest("Rheinland Küchenmöbel GmbH", null, null)).id();
		Long productId = productService
			.create(new ProductRequest("US-60", "Unterschrank 60 cm", null, ProductCategory.CABINET, ProductUnit.PIECE,
					new BigDecimal("112.00"), new BigDecimal("100.00"), supplierId, true))
			.id();
		Long quoteId = quoteService
			.create(new QuoteRequest(customerId, LocalDate.now().plusDays(30), BigDecimal.ZERO, null,
					List.of(new QuoteRequest.Item(productId, BigDecimal.ONE, new BigDecimal("100.00"),
							BigDecimal.ZERO))),
					sales.getId())
			.id();
		quoteService.changeStatus(quoteId, QuoteStatus.SENT);
		quoteService.changeStatus(quoteId, QuoteStatus.ACCEPTED);
		orderId = orderService.createFromQuote(quoteId, sales.getId()).id();
	}

	@Test
	void plannedAppointmentCarriesWhatTheEmployeeNeedsOnSite() throws Exception {
		create(AppointmentType.INSTALLATION, "2030-03-12T08:00:00Z", "2030-03-12T16:00:00Z", installer)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.type").value("INSTALLATION"))
			.andExpect(jsonPath("$.assigneeName").value("Jonas Becker"))
			.andExpect(jsonPath("$.orderNumber").exists())
			.andExpect(jsonPath("$.customerName").value("Thomas Schmidt"))
			.andExpect(jsonPath("$.customerPhone").value("0211 9876543"))
			// The separate installation address, not the billing address
			.andExpect(jsonPath("$.address.street").value("Am Rheinufer 8"));
	}

	@Test
	void listReturnsOnlyAppointmentsOverlappingThePeriod() throws Exception {
		plan(AppointmentType.MEASUREMENT, "2030-03-11T09:00:00Z", "2030-03-11T10:00:00Z", sales);
		plan(AppointmentType.DELIVERY, "2030-03-12T14:00:00Z", "2030-03-12T15:00:00Z", installer);
		plan(AppointmentType.INSTALLATION, "2030-03-12T08:00:00Z", "2030-03-12T12:00:00Z", otherInstaller);
		// Started the evening before and runs into the requested day
		plan(AppointmentType.INSTALLATION, "2030-03-11T20:00:00Z", "2030-03-12T02:00:00Z", installer);

		mockMvc.perform(get("/api/appointments").with(as(sales)).param("from", DAY).param("to", NEXT_DAY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0].startTime").value("2030-03-11T20:00:00Z"))
			.andExpect(jsonPath("$[2].type").value("DELIVERY"));

		mockMvc
			.perform(get("/api/appointments").with(as(sales))
				.param("from", DAY)
				.param("to", NEXT_DAY)
				.param("assigneeId", otherInstaller.getId().toString()))
			.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void installerSeesOnlyTheirOwnAppointments() throws Exception {
		plan(AppointmentType.DELIVERY, "2030-03-12T14:00:00Z", "2030-03-12T15:00:00Z", installer);
		plan(AppointmentType.INSTALLATION, "2030-03-12T08:00:00Z", "2030-03-12T12:00:00Z", otherInstaller);
		plan(AppointmentType.MEASUREMENT, "2030-03-12T09:00:00Z", "2030-03-12T10:00:00Z", sales);

		mockMvc.perform(get("/api/appointments").with(as(installer)).param("from", DAY).param("to", NEXT_DAY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].assigneeName").value("Jonas Becker"));

		// Asking for a colleague's appointments makes no difference
		mockMvc
			.perform(get("/api/appointments").with(as(installer))
				.param("from", DAY)
				.param("to", NEXT_DAY)
				.param("assigneeId", otherInstaller.getId().toString()))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].assigneeName").value("Jonas Becker"));
	}

	@Test
	void installerCannotPlanChangeOrCancelAppointments() throws Exception {
		Long id = plan(AppointmentType.DELIVERY, "2030-03-12T14:00:00Z", "2030-03-12T15:00:00Z", installer);
		String body = json(AppointmentType.DELIVERY, "2030-03-12T16:00:00Z", "2030-03-12T17:00:00Z", installer);

		mockMvc.perform(post("/api/appointments").with(as(installer)).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isForbidden());
		mockMvc
			.perform(put("/api/appointments/" + id).with(as(installer)).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/appointments/" + id).with(as(installer))).andExpect(status().isForbidden());
	}

	@Test
	void appointmentCanBeRescheduledReassignedAndCancelled() throws Exception {
		Long id = plan(AppointmentType.DELIVERY, "2030-03-12T14:00:00Z", "2030-03-12T15:00:00Z", installer);

		mockMvc
			.perform(put("/api/appointments/" + id).with(as(sales))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(AppointmentType.DELIVERY, "2030-03-14T08:00:00Z", "2030-03-14T09:00:00Z", otherInstaller)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.startTime").value("2030-03-14T08:00:00Z"))
			.andExpect(jsonPath("$.assigneeName").value("Mia Vogel"));

		mockMvc.perform(delete("/api/appointments/" + id).with(as(sales))).andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/appointments/" + id).with(as(sales))).andExpect(status().isNotFound());
	}

	@Test
	void invalidAppointmentsAreRejected() throws Exception {
		create(AppointmentType.DELIVERY, "2030-03-12T15:00:00Z", "2030-03-12T14:00:00Z", installer)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.endAfterStart").exists());

		otherInstaller.setActive(false);
		create(AppointmentType.DELIVERY, "2030-03-12T14:00:00Z", "2030-03-12T15:00:00Z", otherInstaller)
			.andExpect(status().isConflict());
	}

	@Test
	void staffCanListActiveEmployeesToAssign() throws Exception {
		otherInstaller.setActive(false);

		mockMvc.perform(get("/api/users/assignable").with(as(sales)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].lastName").value("Becker"))
			.andExpect(jsonPath("$[0].role").value("INSTALLER"))
			.andExpect(jsonPath("$[0].email").doesNotExist());
		mockMvc.perform(get("/api/users/assignable").with(as(installer))).andExpect(status().isForbidden());
	}

	private User createUser(String email, String firstName, String lastName, Role role) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash("irrelevant");
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setRole(role);
		return userRepository.save(user);
	}

	private Long plan(AppointmentType type, String start, String end, User assignee) {
		return appointmentService
			.create(new AppointmentRequest(orderId, type, Instant.parse(start), Instant.parse(end), assignee.getId(),
					null))
			.id();
	}

	private ResultActions create(AppointmentType type, String start, String end, User assignee) throws Exception {
		return mockMvc.perform(post("/api/appointments").with(as(sales))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(type, start, end, assignee)));
	}

	private String json(AppointmentType type, String start, String end, User assignee) {
		return """
				{"orderId": %d, "type": "%s", "startTime": "%s", "endTime": "%s", "assigneeId": %d}
				""".formatted(orderId, type, start, end, assignee.getId());
	}

	// Tokens carry the role both as an authority and as the "role" claim, like real logins
	private RequestPostProcessor as(User user) {
		return jwt().jwt(token -> token.subject(user.getId().toString()).claim("role", user.getRole().name()))
			.authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

}
