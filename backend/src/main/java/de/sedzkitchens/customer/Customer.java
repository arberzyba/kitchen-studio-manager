package de.sedzkitchens.customer;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private Salutation salutation;

	private String firstName;

	private String lastName;

	private String companyName;

	private String email;

	private String phone;

	@Embedded
	private Address billingAddress;

	// Null when the kitchen is installed at the billing address
	@Embedded
	@AttributeOverride(name = "street", column = @Column(name = "installation_street"))
	@AttributeOverride(name = "postalCode", column = @Column(name = "installation_postal_code"))
	@AttributeOverride(name = "city", column = @Column(name = "installation_city"))
	private Address installationAddress;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	public String getDisplayName() {
		return firstName + " " + lastName;
	}

}
