package de.sedzkitchens.audit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

import com.jayway.jsonpath.JsonPath;

import de.sedzkitchens.supplier.SupplierRequest;
import de.sedzkitchens.supplier.SupplierService;
import de.sedzkitchens.user.Role;
import de.sedzkitchens.user.User;
import de.sedzkitchens.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SupplierService supplierService;

	private RequestPostProcessor admin;

	private Long supplierId;

	@BeforeEach
	void createTestData() {
		User user = new User();
		user.setEmail("admin@example.com");
		user.setPasswordHash("irrelevant");
		user.setFirstName("Anna");
		user.setLastName("Schneider");
		user.setRole(Role.ADMIN);
		userRepository.save(user);
		admin = jwt().jwt(token -> token.subject(user.getId().toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
		// Created without a logged-in user, like demo data
		supplierId = supplierService.create(new SupplierRequest("Rheinland Küchenmöbel GmbH", null, null)).id();
	}

	@Test
	void changesAreLoggedWithUserActionAndTheNamesOfChangedFields() throws Exception {
		String created = mockMvc
			.perform(post("/api/products").with(admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content(productJson("Unterschrank 60 cm", "189.00")))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		Number productId = JsonPath.read(created, "$.id");
		mockMvc
			.perform(put("/api/products/" + productId).with(admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content(productJson("Unterschrank 60 cm, Eiche", "219.00")))
			.andExpect(status().isOk());
		writePendingChanges("/api/products");

		mockMvc.perform(get("/api/audit-log").with(admin).param("entityType", "Product"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			// Newest first
			.andExpect(jsonPath("$.content[0].action").value("UPDATE"))
			.andExpect(jsonPath("$.content[0].entityId").value(productId))
			.andExpect(jsonPath("$.content[0].userName").value("Anna Schneider"))
			.andExpect(jsonPath("$.content[0].changedFields", containsString("name")))
			.andExpect(jsonPath("$.content[0].changedFields", containsString("sellingPrice")))
			// Field names only: neither the new name nor the new price is stored
			.andExpect(jsonPath("$.content[0].changedFields", not(containsString("Eiche"))))
			.andExpect(jsonPath("$.content[0].changedFields", not(containsString("219"))))
			.andExpect(jsonPath("$.content[0].changedFields", not(containsString("purchasePrice"))))
			.andExpect(jsonPath("$.content[1].action").value("CREATE"))
			.andExpect(jsonPath("$.content[1].occurredAt").exists())
			.andExpect(jsonPath("$.content[1].changedFields").isEmpty());
	}

	@Test
	void savingWithoutChangesLogsNothing() throws Exception {
		mockMvc
			.perform(put("/api/suppliers/" + supplierId).with(admin).contentType(MediaType.APPLICATION_JSON).content("""
					{"name": "Rheinland Küchenmöbel GmbH"}
					"""))
			.andExpect(status().isOk());
		writePendingChanges("/api/suppliers");

		mockMvc.perform(get("/api/audit-log").with(admin).param("entityType", "Supplier").param("action", "UPDATE"))
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void changesByTheSystemHaveNoUserAndTheLogCanBeFiltered() throws Exception {
		mockMvc
			.perform(get("/api/audit-log").with(admin)
				.param("entityType", "Supplier")
				.param("entityId", supplierId.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].action").value("CREATE"))
			.andExpect(jsonPath("$.content[0].userId").isEmpty())
			.andExpect(jsonPath("$.content[0].userName").isEmpty());

		mockMvc.perform(get("/api/audit-log").with(admin).param("action", "DELETE"))
			.andExpect(jsonPath("$.content.length()").value(0));
		// The admin user and the supplier created in the setup
		mockMvc.perform(get("/api/audit-log").with(admin)).andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	void onlyAdminsCanReadTheAuditLog() throws Exception {
		for (String role : new String[] { "SALES", "OFFICE", "INSTALLER" }) {
			mockMvc
				.perform(get("/api/audit-log")
					.with(jwt().jwt(token -> token.subject("999")).authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
	}

	// Each test runs in one transaction, so an update is still pending after its request. In the application
	// every request commits on its own, which writes it. Here, reading the same kind of record as the same
	// user makes Hibernate write the pending update first.
	private void writePendingChanges(String listUrl) throws Exception {
		mockMvc.perform(get(listUrl).with(admin)).andExpect(status().isOk());
	}

	private String productJson(String name, String sellingPrice) {
		return """
				{"sku": "US-60", "name": "%s", "category": "CABINET", "unit": "PIECE",
				 "purchasePrice": 112.00, "sellingPrice": %s, "supplierId": %d, "active": true}
				""".formatted(name, sellingPrice, supplierId);
	}

}
