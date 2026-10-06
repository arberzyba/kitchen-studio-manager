package de.sedzkitchens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import de.sedzkitchens.user.UserRepository;

// Starts the application the way the hosting platform does: "prod" profile with every setting supplied
// from outside. Here the settings point at an in-memory database.
@SpringBootTest(properties = { "DATABASE_URL=jdbc:h2:mem:production-profile-test", "DATABASE_USERNAME=sa",
		"DATABASE_PASSWORD=", "JWT_SECRET=production-profile-test-secret-0123456789",
		"FRONTEND_URL=https://kitchens.example.com" })
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProductionProfileTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Test
	void startsWithMigrationsButWithoutDemoData() throws Exception {
		mockMvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
		assertThat(userRepository.count()).isZero();
	}

	@Test
	void browserCallsFromTheHostedFrontendAreAllowed() throws Exception {
		mockMvc
			.perform(options("/api/customers").header("Origin", "https://kitchens.example.com")
				.header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "Authorization"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "https://kitchens.example.com"));
	}

	@Test
	void browserCallsFromOtherSitesAreRefused() throws Exception {
		mockMvc
			.perform(options("/api/customers").header("Origin", "https://evil.example.com")
				.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isForbidden());
	}

	@Test
	void apiDocumentationIsPublicAndDescribesTheEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("SedzKitchens API"))
			.andExpect(jsonPath("$.paths['/api/quotes']").exists())
			.andExpect(jsonPath("$.paths['/api/invoices/{id}/payments']").exists())
			.andExpect(jsonPath("$.components.securitySchemes['bearer-token'].scheme").value("bearer"));
	}

}
