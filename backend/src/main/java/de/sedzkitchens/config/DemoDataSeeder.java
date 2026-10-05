package de.sedzkitchens.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.sedzkitchens.user.CreateUserRequest;
import de.sedzkitchens.user.Role;
import de.sedzkitchens.user.UserRepository;
import de.sedzkitchens.user.UserService;
import lombok.RequiredArgsConstructor;

// Creates one demo login per role when the dev database is empty.
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

	private static final String DEMO_PASSWORD = "demo1234";

	private final UserRepository userRepository;

	private final UserService userService;

	@Override
	public void run(ApplicationArguments args) {
		if (userRepository.count() > 0) {
			return;
		}
		createUser("admin@sedzkitchens.de", "Anna", "Schneider", Role.ADMIN);
		createUser("sales@sedzkitchens.de", "Lukas", "Weber", Role.SALES);
		createUser("office@sedzkitchens.de", "Petra", "Hoffmann", Role.OFFICE);
		createUser("installer@sedzkitchens.de", "Jonas", "Becker", Role.INSTALLER);
	}

	private void createUser(String email, String firstName, String lastName, Role role) {
		userService.create(new CreateUserRequest(email, DEMO_PASSWORD, firstName, lastName, role));
	}

}
