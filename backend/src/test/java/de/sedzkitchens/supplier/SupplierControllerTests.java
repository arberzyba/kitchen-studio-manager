package de.sedzkitchens.supplier;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SupplierControllerTests {

	private static final String SUPPLIER = """
			{"name": "Westfalen Arbeitsplatten KG", "email": "bestellung@westfalen-ap.example.de", "phone": "0251 112233"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SupplierService supplierService;

	@Test
	void adminCanCreateAndRenameSupplier() throws Exception {
		mockMvc.perform(post("/api/suppliers").with(role("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(SUPPLIER))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Westfalen Arbeitsplatten KG"));

		Long id = supplierService.findAll().getFirst().id();
		mockMvc
			.perform(put("/api/suppliers/" + id).with(role("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("""
					{"name": "Westfalen Arbeitsplatten GmbH"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Westfalen Arbeitsplatten GmbH"))
			.andExpect(jsonPath("$.email").isEmpty());
	}

	@Test
	void duplicateNameIsRejected() throws Exception {
		supplierService.create(new SupplierRequest("Westfalen Arbeitsplatten KG", null, null));

		mockMvc.perform(post("/api/suppliers").with(role("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(SUPPLIER))
			.andExpect(status().isConflict());
	}

	@Test
	void salesCanListButNotChangeSuppliers() throws Exception {
		supplierService.create(new SupplierRequest("Westfalen Arbeitsplatten KG", null, null));

		mockMvc.perform(get("/api/suppliers").with(role("SALES")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(post("/api/suppliers").with(role("SALES")).contentType(MediaType.APPLICATION_JSON).content(SUPPLIER))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/suppliers").with(role("INSTALLER"))).andExpect(status().isForbidden());
	}

	private RequestPostProcessor role(String role) {
		return jwt().jwt(token -> token.subject("999")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

}
