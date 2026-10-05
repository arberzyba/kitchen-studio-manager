package de.sedzkitchens.user;

import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTests {

	private static final String NEW_USER = """
			{"email": "New.Installer@example.com", "password": "secret-password",
			 "firstName": "Jonas", "lastName": "Becker", "role": "INSTALLER"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	private User admin;

	@BeforeEach
	void createAdmin() {
		admin = new User();
		admin.setEmail("admin@example.com");
		admin.setPasswordHash("irrelevant");
		admin.setFirstName("Anna");
		admin.setLastName("Schneider");
		admin.setRole(Role.ADMIN);
		userRepository.save(admin);
	}

	@Test
	void adminCanCreateUser() throws Exception {
		mockMvc.perform(post("/api/users").with(as(admin)).contentType(MediaType.APPLICATION_JSON).content(NEW_USER))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("new.installer@example.com"))
			.andExpect(jsonPath("$.role").value("INSTALLER"))
			.andExpect(jsonPath("$.active").value(true))
			.andExpect(jsonPath("$.password").doesNotExist());

		User created = userRepository.findByEmail("new.installer@example.com").orElseThrow();
		assertThat(created.getPasswordHash()).startsWith("$2").doesNotContain("secret-password");
	}

	@Test
	void adminCanListUsers() throws Exception {
		mockMvc.perform(get("/api/users").with(as(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].email").value("admin@example.com"));
	}

	@Test
	void nonAdminCannotManageUsers() throws Exception {
		RequestPostProcessor sales = jwt().jwt(token -> token.subject("999"))
			.authorities(new SimpleGrantedAuthority("ROLE_SALES"));

		mockMvc.perform(get("/api/users").with(sales)).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/users").with(sales).contentType(MediaType.APPLICATION_JSON).content(NEW_USER))
			.andExpect(status().isForbidden());
	}

	@Test
	void duplicateEmailIsRejected() throws Exception {
		String duplicate = NEW_USER.replace("New.Installer@example.com", "ADMIN@example.com");

		mockMvc.perform(post("/api/users").with(as(admin)).contentType(MediaType.APPLICATION_JSON).content(duplicate))
			.andExpect(status().isConflict());
	}

	@Test
	void invalidInputIsRejectedWithFieldErrors() throws Exception {
		String invalid = """
				{"email": "not-an-email", "password": "short", "firstName": "", "lastName": "Becker", "role": "INSTALLER"}
				""";

		mockMvc.perform(post("/api/users").with(as(admin)).contentType(MediaType.APPLICATION_JSON).content(invalid))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.password").exists())
			.andExpect(jsonPath("$.errors.firstName").exists());
	}

	@Test
	void adminCanUpdateAndDeactivateAnotherUser() throws Exception {
		User installer = new User();
		installer.setEmail("installer@example.com");
		installer.setPasswordHash("irrelevant");
		installer.setFirstName("Jonas");
		installer.setLastName("Becker");
		installer.setRole(Role.INSTALLER);
		userRepository.save(installer);

		mockMvc
			.perform(put("/api/users/" + installer.getId()).with(as(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName": "Jonas", "lastName": "Becker-Klein", "role": "OFFICE", "active": false}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lastName").value("Becker-Klein"))
			.andExpect(jsonPath("$.role").value("OFFICE"))
			.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void adminCannotDeactivateOwnAccount() throws Exception {
		mockMvc
			.perform(put("/api/users/" + admin.getId()).with(as(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName": "Anna", "lastName": "Schneider", "role": "ADMIN", "active": false}
						"""))
			.andExpect(status().isConflict());
	}

	@Test
	void updatingUnknownUserReturnsNotFound() throws Exception {
		mockMvc
			.perform(put("/api/users/999999").with(as(admin)).contentType(MediaType.APPLICATION_JSON).content("""
					{"firstName": "A", "lastName": "B", "role": "SALES", "active": true}
					"""))
			.andExpect(status().isNotFound());
	}

	private RequestPostProcessor as(User user) {
		return jwt().jwt(token -> token.subject(user.getId().toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

}
