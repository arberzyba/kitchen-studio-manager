package de.sedzkitchens.quote;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

	// Expects a lower-case LIKE pattern such as "%müller%"; a null status matches every status
	@EntityGraph(attributePaths = "customer")
	@Query("""
			select q from Quote q join q.customer c
			where (lower(q.quoteNumber) like :pattern
			    or lower(concat(c.firstName, ' ', c.lastName)) like :pattern
			    or lower(c.companyName) like :pattern)
			  and (:status is null or q.status = :status)
			""")
	Page<Quote> search(@Param("pattern") String pattern, @Param("status") QuoteStatus status, Pageable pageable);

}
