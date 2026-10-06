package de.sedzkitchens.quote;

import java.math.BigDecimal;
import java.util.List;

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

	// Number of quotes and their combined gross value per status, for the dashboard
	@Query("""
			select q.status as status, count(q) as count, coalesce(sum(q.grossTotal), 0) as grossTotal
			from Quote q group by q.status
			""")
	List<StatusSummary> summarizeByStatus();

	interface StatusSummary {

		QuoteStatus getStatus();

		long getCount();

		BigDecimal getGrossTotal();

	}

}
