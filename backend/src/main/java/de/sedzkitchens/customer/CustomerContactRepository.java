package de.sedzkitchens.customer;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> {

	// Loads the author in the same query, since every entry shows who wrote it
	@EntityGraph(attributePaths = "createdBy")
	List<CustomerContact> findByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

}
