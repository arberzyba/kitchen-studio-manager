package de.sedzkitchens.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.sedzkitchens.customer.AddressDto;
import de.sedzkitchens.customer.ContactRequest;
import de.sedzkitchens.customer.ContactType;
import de.sedzkitchens.customer.CustomerRepository;
import de.sedzkitchens.customer.CustomerRequest;
import de.sedzkitchens.customer.CustomerService;
import de.sedzkitchens.customer.Salutation;
import de.sedzkitchens.user.CreateUserRequest;
import de.sedzkitchens.user.Role;
import de.sedzkitchens.user.UserRepository;
import de.sedzkitchens.user.UserService;
import lombok.RequiredArgsConstructor;

// Fills an empty dev database with demo data: one login per role and a few customers.
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

	private static final String DEMO_PASSWORD = "demo1234";

	private final UserRepository userRepository;

	private final UserService userService;

	private final CustomerRepository customerRepository;

	private final CustomerService customerService;

	@Override
	public void run(ApplicationArguments args) {
		if (userRepository.count() == 0) {
			createUser("admin@sedzkitchens.de", "Anna", "Schneider", Role.ADMIN);
			createUser("sales@sedzkitchens.de", "Lukas", "Weber", Role.SALES);
			createUser("office@sedzkitchens.de", "Petra", "Hoffmann", Role.OFFICE);
			createUser("installer@sedzkitchens.de", "Jonas", "Becker", Role.INSTALLER);
		}
		if (customerRepository.count() == 0) {
			seedCustomers();
		}
	}

	private void seedCustomers() {
		Long salesId = userRepository.findByEmail("sales@sedzkitchens.de").orElseThrow().getId();

		Long mueller = createCustomer(Salutation.MS, "Sabine", "Müller", null, "sabine.mueller@example.de",
				"0221 4567890", new AddressDto("Aachener Straße 112", "50674", "Köln"), null);
		addContact(mueller, ContactType.MEETING,
				"Erstberatung im Studio. Wünscht eine L-Küche mit Kochinsel, Budget ca. 18.000 €.", salesId);
		addContact(mueller, ContactType.CALL, "Aufmaßtermin telefonisch für nächste Woche abgestimmt.", salesId);

		Long schmidt = createCustomer(Salutation.MR, "Thomas", "Schmidt", null, "t.schmidt@example.de",
				"0211 9876543", new AddressDto("Kaiserswerther Straße 45", "40477", "Düsseldorf"),
				new AddressDto("Am Rheinufer 8", "40545", "Düsseldorf"));
		addContact(schmidt, ContactType.NOTE, "Küche wird in der neuen Eigentumswohnung eingebaut, Einzug im März.",
				salesId);

		Long hausverwaltung = createCustomer(Salutation.MR, "Michael", "Krüger", "Krüger Hausverwaltung GmbH",
				"m.krueger@krueger-hv.example.de", "0228 3344556", new AddressDto("Poppelsdorfer Allee 27", "53115", "Bonn"),
				new AddressDto("Kölnstraße 310", "53117", "Bonn"));
		addContact(hausverwaltung, ContactType.EMAIL,
				"Anfrage für drei baugleiche Küchenzeilen in Mietwohnungen erhalten.", salesId);

		createCustomer(Salutation.MS, "Elif", "Yılmaz", null, "elif.yilmaz@example.de", "0201 7788990",
				new AddressDto("Rüttenscheider Straße 201", "45131", "Essen"), null);
		createCustomer(Salutation.MR, "Andreas", "Fischer", null, "andreas.fischer@example.de", "02202 123456",
				new AddressDto("Hauptstraße 76", "51465", "Bergisch Gladbach"), null);
		createCustomer(Salutation.MS, "Katharina", "Wagner", "Praxis Dr. Wagner", "praxis@dr-wagner.example.de",
				"0214 5566778", new AddressDto("Wiesdorfer Platz 3", "51373", "Leverkusen"), null);
		createCustomer(Salutation.NONE, "Kim", "Braun", null, null, "0172 3456789",
				new AddressDto("Venloer Straße 389", "50825", "Köln"), null);
		createCustomer(Salutation.MR, "Stefan", "Zimmermann", null, "stefan.zimmermann@example.de", null,
				new AddressDto("Bahnhofstraße 14", "50354", "Hürth"), null);
	}

	private void createUser(String email, String firstName, String lastName, Role role) {
		userService.create(new CreateUserRequest(email, DEMO_PASSWORD, firstName, lastName, role));
	}

	private Long createCustomer(Salutation salutation, String firstName, String lastName, String companyName,
			String email, String phone, AddressDto billingAddress, AddressDto installationAddress) {
		return customerService
			.create(new CustomerRequest(salutation, firstName, lastName, companyName, email, phone, billingAddress,
					installationAddress))
			.id();
	}

	private void addContact(Long customerId, ContactType type, String summary, Long userId) {
		customerService.addContact(customerId, new ContactRequest(type, summary), userId);
	}

}
