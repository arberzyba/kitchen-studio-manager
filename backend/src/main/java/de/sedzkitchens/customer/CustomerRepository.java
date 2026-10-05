package de.sedzkitchens.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	// Expects a lower-case LIKE pattern such as "%schmidt%"
	@Query("""
			select c from Customer c
			where lower(concat(c.firstName, ' ', c.lastName)) like :pattern
			   or lower(c.companyName) like :pattern
			   or lower(c.email) like :pattern
			   or lower(c.billingAddress.city) like :pattern
			""")
	Page<Customer> search(@Param("pattern") String pattern, Pageable pageable);

}
