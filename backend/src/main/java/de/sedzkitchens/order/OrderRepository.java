package de.sedzkitchens.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<SalesOrder, Long> {

	// Expects a lower-case LIKE pattern such as "%müller%"; a null status or quote id matches everything
	@EntityGraph(attributePaths = { "quote", "quote.customer" })
	@Query("""
			select o from SalesOrder o join o.quote q join q.customer c
			where (lower(o.orderNumber) like :pattern
			    or lower(concat(c.firstName, ' ', c.lastName)) like :pattern
			    or lower(c.companyName) like :pattern)
			  and (:status is null or o.status = :status)
			  and (:quoteId is null or q.id = :quoteId)
			""")
	Page<SalesOrder> search(@Param("pattern") String pattern, @Param("status") OrderStatus status,
			@Param("quoteId") Long quoteId, Pageable pageable);

	boolean existsByQuoteId(Long quoteId);

}
