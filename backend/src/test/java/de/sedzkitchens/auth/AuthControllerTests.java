package de.sedzkitchens.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import de.sedzkitchens.user.Role;
import de.sedzkitchens.user.User;
import de.sedzkitchens.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private User user;

	@BeforeEach
	void createUser() {
		user = new User();
		user.setEmail("sales@example.com");
		user.setPasswordHash(passwordEncoder.encode("correct-password"));
		user.setFirstName("Lukas");
		user.setLastName("Weber");
		user.setRole(Role.SALES);
		userRepository.save(user);
	}

	@Test
	void loginReturnsTokenThatAuthenticatesFurtherRequests() throws Exception {
		String response = login("Sales@Example.com", "correct-password").andExpect(status().isOk())
			.andExpect(jsonPath("$.user.email").value("sales@example.com"))
			.andExpect(jsonPath("$.user.role").value("SALES"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String token = JsonPath.read(response, "$.token");

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("sales@example.com"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void loginWithWrongPasswordIsRejected() throws Exception {
		login("sales@example.com", "wrong-password").andExpect(status().isUnauthorized());
	}

	@Test
	void loginWithUnknownEmailIsRejected() throws Exception {
		login("nobody@example.com", "correct-password").andExpect(status().isUnauthorized());
	}

	@Test
	void loginOfDeactivatedUserIsRejected() throws Exception {
		user.setActive(false);

		login("sales@example.com", "correct-password").andExpect(status().isUnauthorized());
	}

	@Test
	void requestWithoutTokenIsRejected() throws Exception {
		mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "%s", "password": "%s"}
					""".formatted(email, password)));
	}

}
