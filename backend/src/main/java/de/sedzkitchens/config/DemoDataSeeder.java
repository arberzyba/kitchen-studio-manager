package de.sedzkitchens.config;

import java.math.BigDecimal;

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
import de.sedzkitchens.product.ProductCategory;
import de.sedzkitchens.product.ProductRepository;
import de.sedzkitchens.product.ProductRequest;
import de.sedzkitchens.product.ProductService;
import de.sedzkitchens.product.ProductUnit;
import de.sedzkitchens.supplier.SupplierRequest;
import de.sedzkitchens.supplier.SupplierService;
import de.sedzkitchens.user.CreateUserRequest;
import de.sedzkitchens.user.Role;
import de.sedzkitchens.user.UserRepository;
import de.sedzkitchens.user.UserService;
import lombok.RequiredArgsConstructor;

// Fills an empty dev database with demo data: one login per role, customers, suppliers and products.
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

	private static final String DEMO_PASSWORD = "demo1234";

	private final UserRepository userRepository;

	private final UserService userService;

	private final CustomerRepository customerRepository;

	private final CustomerService customerService;

	private final ProductRepository productRepository;

	private final ProductService productService;

	private final SupplierService supplierService;

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
		if (productRepository.count() == 0) {
			seedCatalog();
		}
	}

	private void seedCatalog() {
		Long cabinets = createSupplier("Rheinland Küchenmöbel GmbH", "bestellung@rheinland-kuechen.example.de",
				"02234 556677");
		Long worktops = createSupplier("Westfalen Arbeitsplatten KG", "auftrag@westfalen-ap.example.de", "0251 112233");
		Long appliances = createSupplier("Hausgeräte Nord Vertriebs GmbH", "haendler@hausgeraete-nord.example.de",
				"040 998877");

		createProduct("US-60-W", "Unterschrank 60 cm, weiß matt", ProductCategory.CABINET, ProductUnit.PIECE, "112.00",
				"189.00", cabinets);
		createProduct("US-90-W", "Auszugsunterschrank 90 cm, weiß matt", ProductCategory.CABINET, ProductUnit.PIECE,
				"198.00", "329.00", cabinets);
		createProduct("HS-60-W", "Hängeschrank 60 cm, weiß matt", ProductCategory.CABINET, ProductUnit.PIECE, "84.00",
				"145.00", cabinets);
		createProduct("HO-60-W", "Hochschrank für Einbaugeräte 60 cm, weiß matt", ProductCategory.CABINET,
				ProductUnit.PIECE, "265.00", "449.00", cabinets);
		createProduct("SP-60-W", "Spülenunterschrank 60 cm, weiß matt", ProductCategory.CABINET, ProductUnit.PIECE,
				"105.00", "179.00", cabinets);
		createProduct("AP-EI-38", "Arbeitsplatte Eiche Dekor, 38 mm", ProductCategory.WORKTOP, ProductUnit.METER,
				"48.00", "89.00", worktops);
		createProduct("AP-QZ-20", "Arbeitsplatte Quarzstein grau, 20 mm", ProductCategory.WORKTOP, ProductUnit.METER,
				"210.00", "369.00", worktops);
		createProduct("AP-MA-12", "Arbeitsplatte Keramik Marmoroptik, 12 mm", ProductCategory.WORKTOP,
				ProductUnit.METER, "265.00", "459.00", worktops);
		createProduct("EG-BO-60", "Einbaubackofen 60 cm, Edelstahl", ProductCategory.APPLIANCE, ProductUnit.PIECE,
				"389.00", "599.00", appliances);
		createProduct("EG-IK-80", "Induktionskochfeld 80 cm", ProductCategory.APPLIANCE, ProductUnit.PIECE, "465.00",
				"749.00", appliances);
		createProduct("EG-GS-60", "Geschirrspüler vollintegriert 60 cm", ProductCategory.APPLIANCE, ProductUnit.PIECE,
				"412.00", "649.00", appliances);
		createProduct("EG-KS-178", "Einbau-Kühl-Gefrierkombination 178 cm", ProductCategory.APPLIANCE,
				ProductUnit.PIECE, "598.00", "929.00", appliances);
		createProduct("EG-DA-90", "Wandhaube 90 cm, Edelstahl", ProductCategory.APPLIANCE, ProductUnit.PIECE, "245.00",
				"399.00", appliances);
	}

	private Long createSupplier(String name, String email, String phone) {
		return supplierService.create(new SupplierRequest(name, email, phone)).id();
	}

	private void createProduct(String sku, String name, ProductCategory category, ProductUnit unit,
			String purchasePrice, String sellingPrice, Long supplierId) {
		productService.create(new ProductRequest(sku, name, null, category, unit, new BigDecimal(purchasePrice),
				new BigDecimal(sellingPrice), supplierId, true));
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
