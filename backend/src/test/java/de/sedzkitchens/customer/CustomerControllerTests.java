package de.sedzkitchens.customer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.user.Role;
import de.sedzkitchens.user.User;
import de.sedzkitchens.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerControllerTests {

	private static final String NEW_CUSTOMER = """
			{"salutation": "MS", "firstName": "Sabine", "lastName": "Müller", "email": "sabine@example.de",
			 "phone": "0221 4567890",
			 "billingAddress": {"street": "Aachener Straße 112", "postalCode": "50674", "city": "Köln"}}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CustomerService customerService;

	private RequestPostProcessor sales;

	@BeforeEach
	void createSalesUser() {
		User user = new User();
		user.setEmail("sales@example.com");
		user.setPasswordHash("irrelevant");
		user.setFirstName("Lukas");
		user.setLastName("Weber");
		user.setRole(Role.SALES);
		userRepository.save(user);
		sales = jwt().jwt(token -> token.subject(user.getId().toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_SALES"));
	}

	@Test
	void salesCanCreateAndReadCustomer() throws Exception {
		mockMvc.perform(post("/api/customers").with(sales).contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.lastName").value("Müller"))
			.andExpect(jsonPath("$.billingAddress.city").value("Köln"))
			.andExpect(jsonPath("$.installationAddress").isEmpty())
			.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	void customerCanHaveSeparateInstallationAddress() throws Exception {
		Long id = createCustomer("Thomas", "Schmidt", "Düsseldorf");

		mockMvc
			.perform(put("/api/customers/" + id).with(sales).contentType(MediaType.APPLICATION_JSON).content("""
					{"salutation": "MR", "firstName": "Thomas", "lastName": "Schmidt-Lang",
					 "billingAddress": {"street": "Kaiserswerther Straße 45", "postalCode": "40477", "city": "Düsseldorf"},
					 "installationAddress": {"street": "Am Rheinufer 8", "postalCode": "40545", "city": "Düsseldorf"}}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lastName").value("Schmidt-Lang"))
			.andExpect(jsonPath("$.installationAddress.street").value("Am Rheinufer 8"));

		mockMvc.perform(get("/api/customers/" + id).with(sales))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.installationAddress.postalCode").value("40545"));
	}

	@Test
	void searchMatchesNameAndCityAndIsPaged() throws Exception {
		createCustomer("Sabine", "Müller", "Köln");
		createCustomer("Thomas", "Schmidt", "Düsseldorf");
		createCustomer("Kim", "Braun", "Köln");

		mockMvc.perform(get("/api/customers").with(sales).param("search", "köln"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].lastName").value("Braun"));

		mockMvc.perform(get("/api/customers").with(sales).param("search", "thomas schm"))
			.andExpect(jsonPath("$.content.length()").value(1));

		mockMvc.perform(get("/api/customers").with(sales).param("size", "2"))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.page.totalElements").value(3));
	}

	@Test
	void invalidCustomerIsRejectedWithFieldErrors() throws Exception {
		String invalid = """
				{"salutation": "MS", "firstName": "", "lastName": "Müller", "email": "not-an-email",
				 "billingAddress": {"street": "Aachener Straße 112", "postalCode": "5067", "city": "Köln"}}
				""";

		mockMvc.perform(post("/api/customers").with(sales).contentType(MediaType.APPLICATION_JSON).content(invalid))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.firstName").exists())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors['billingAddress.postalCode']").exists());
	}

	@Test
	void officeCanReadButNotEditCustomers() throws Exception {
		Long id = createCustomer("Sabine", "Müller", "Köln");
		RequestPostProcessor office = jwt().jwt(token -> token.subject("999"))
			.authorities(new SimpleGrantedAuthority("ROLE_OFFICE"));

		mockMvc.perform(get("/api/customers/" + id).with(office)).andExpect(status().isOk());
		mockMvc.perform(post("/api/customers").with(office).contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
			.andExpect(status().isForbidden());
		mockMvc
			.perform(put("/api/customers/" + id).with(office).contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
			.andExpect(status().isForbidden());
	}

	@Test
	void installerHasNoAccessToCustomerData() throws Exception {
		Long id = createCustomer("Sabine", "Müller", "Köln");
		RequestPostProcessor installer = jwt().jwt(token -> token.subject("999"))
			.authorities(new SimpleGrantedAuthority("ROLE_INSTALLER"));

		mockMvc.perform(get("/api/customers").with(installer)).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/customers/" + id).with(installer)).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/customers/" + id + "/contacts").with(installer)).andExpect(status().isForbidden());
	}

	@Test
	void contactHistoryRecordsAuthorAndListsNewestFirst() throws Exception {
		Long id = createCustomer("Sabine", "Müller", "Köln");

		mockMvc
			.perform(post("/api/customers/" + id + "/contacts").with(sales).contentType(MediaType.APPLICATION_JSON).content("""
					{"contactType": "MEETING", "summary": "Erstberatung im Studio"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.createdByName").value("Lukas Weber"));
		mockMvc
			.perform(post("/api/customers/" + id + "/contacts").with(sales).contentType(MediaType.APPLICATION_JSON).content("""
					{"contactType": "CALL", "summary": "Aufmaßtermin abgestimmt"}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/customers/" + id + "/contacts").with(sales))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].contactType").value("CALL"))
			.andExpect(jsonPath("$[1].summary").value("Erstberatung im Studio"));
	}

	@Test
	void emptyContactEntryIsRejected() throws Exception {
		Long id = createCustomer("Sabine", "Müller", "Köln");

		mockMvc
			.perform(post("/api/customers/" + id + "/contacts").with(sales).contentType(MediaType.APPLICATION_JSON).content("""
					{"contactType": "NOTE", "summary": " "}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void unknownCustomerReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/customers/999999").with(sales)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/customers/999999/contacts").with(sales)).andExpect(status().isNotFound());
	}

	private Long createCustomer(String firstName, String lastName, String city) {
		return customerService
			.create(new CustomerRequest(Salutation.NONE, firstName, lastName, null, null, null,
					new AddressDto("Teststraße 1", "50667", city), null))
			.id();
	}

}
