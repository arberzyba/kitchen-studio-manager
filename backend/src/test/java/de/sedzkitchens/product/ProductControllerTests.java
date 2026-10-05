package de.sedzkitchens.product;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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

import de.sedzkitchens.supplier.SupplierRequest;
import de.sedzkitchens.supplier.SupplierService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SupplierService supplierService;

	@Autowired
	private ProductService productService;

	private Long supplierId;

	@BeforeEach
	void createSupplier() {
		supplierId = supplierService.create(new SupplierRequest("Rheinland Küchenmöbel GmbH", null, null)).id();
	}

	@Test
	void adminCanCreateProduct() throws Exception {
		mockMvc
			.perform(post("/api/products").with(role("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productJson("US-60", "Unterschrank 60 cm", "189.00")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.sku").value("US-60"))
			.andExpect(jsonPath("$.sellingPrice").value(189.00))
			.andExpect(jsonPath("$.supplierName").value("Rheinland Küchenmöbel GmbH"));
	}

	@Test
	void duplicateArticleNumberIsRejected() throws Exception {
		createProduct("US-60", "Unterschrank 60 cm", ProductCategory.CABINET);

		mockMvc
			.perform(post("/api/products").with(role("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productJson("us-60", "Anderer Schrank", "99.00")))
			.andExpect(status().isConflict());
	}

	@Test
	void negativePriceAndUnknownSupplierAreRejected() throws Exception {
		mockMvc
			.perform(post("/api/products").with(role("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productJson("US-60", "Unterschrank 60 cm", "-1.00")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.sellingPrice").exists());

		mockMvc
			.perform(post("/api/products").with(role("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productJson("US-60", "Unterschrank 60 cm", "189.00").replace("\"supplierId\": " + supplierId,
						"\"supplierId\": 999999")))
			.andExpect(status().isNotFound());
	}

	@Test
	void adminCanUpdateAndDeactivateProduct() throws Exception {
		Long id = createProduct("US-60", "Unterschrank 60 cm", ProductCategory.CABINET);

		mockMvc
			.perform(put("/api/products/" + id).with(role("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productJson("US-60", "Unterschrank 60 cm, weiß", "199.50").replace("\"active\": true",
						"\"active\": false")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Unterschrank 60 cm, weiß"))
			.andExpect(jsonPath("$.sellingPrice").value(199.50))
			.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void searchFiltersByTextAndCategory() throws Exception {
		createProduct("US-60", "Unterschrank 60 cm", ProductCategory.CABINET);
		createProduct("HS-60", "Hängeschrank 60 cm", ProductCategory.CABINET);
		createProduct("AP-EICHE", "Arbeitsplatte Eiche", ProductCategory.WORKTOP);

		mockMvc.perform(get("/api/products").with(role("SALES")).param("search", "schrank"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].name").value("Hängeschrank 60 cm"));

		mockMvc.perform(get("/api/products").with(role("SALES")).param("category", "WORKTOP"))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].sku").value("AP-EICHE"));

		mockMvc.perform(get("/api/products").with(role("SALES")).param("search", "ap-"))
			.andExpect(jsonPath("$.content.length()").value(1));

		mockMvc.perform(get("/api/products").with(role("SALES")))
			.andExpect(jsonPath("$.page.totalElements").value(3));
	}

	@Test
	void onlyAdminsCanChangeTheCatalog() throws Exception {
		Long id = createProduct("US-60", "Unterschrank 60 cm", ProductCategory.CABINET);
		String body = productJson("HS-60", "Hängeschrank 60 cm", "149.00");

		for (String role : new String[] { "SALES", "OFFICE" }) {
			mockMvc.perform(get("/api/products").with(role(role))).andExpect(status().isOk());
			mockMvc.perform(post("/api/products").with(role(role)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
			mockMvc
				.perform(put("/api/products/" + id).with(role(role)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		}
		mockMvc.perform(get("/api/products").with(role("INSTALLER"))).andExpect(status().isForbidden());
	}

	private Long createProduct(String sku, String name, ProductCategory category) {
		return productService
			.create(new ProductRequest(sku, name, null, category, ProductUnit.PIECE, new BigDecimal("100.00"),
					new BigDecimal("150.00"), supplierId, true))
			.id();
	}

	private String productJson(String sku, String name, String sellingPrice) {
		return """
				{"sku": "%s", "name": "%s", "category": "CABINET", "unit": "PIECE",
				 "purchasePrice": 120.00, "sellingPrice": %s, "supplierId": %d, "active": true}
				""".formatted(sku, name, sellingPrice, supplierId);
	}

	private RequestPostProcessor role(String role) {
		return jwt().jwt(token -> token.subject("999")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

}
