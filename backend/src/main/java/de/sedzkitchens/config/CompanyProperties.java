package de.sedzkitchens.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// The company's own details as printed on quotes and invoices (app.company.* in application.properties)
@ConfigurationProperties("app.company")
public record CompanyProperties(String name, String street, String postalCode, String city, String phone,
		String email, String managingDirector, String registerCourt, String registerNumber, String taxNumber,
		String vatId, String bankName, String iban, String bic) {
}
