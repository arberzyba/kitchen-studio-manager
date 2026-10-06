package de.sedzkitchens.invoice;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

	// Expects a lower-case LIKE pattern such as "%müller%". Null filters match everything;
	// a non-null overdueBefore keeps only invoices that are not fully paid and were due before that day.
	@EntityGraph(attributePaths = "salesOrder")
	@Query("""
			select i from Invoice i join i.salesOrder o
			where (lower(i.invoiceNumber) like :pattern
			    or lower(o.orderNumber) like :pattern
			    or lower(i.recipientName) like :pattern
			    or lower(i.recipientCompany) like :pattern)
			  and (:status is null or i.status = :status)
			  and (:salesOrderId is null or o.id = :salesOrderId)
			  and (cast(:overdueBefore as date) is null
			    or (i.status <> de.sedzkitchens.invoice.InvoiceStatus.PAID and i.dueDate < :overdueBefore))
			""")
	Page<Invoice> search(@Param("pattern") String pattern, @Param("status") InvoiceStatus status,
			@Param("salesOrderId") Long salesOrderId, @Param("overdueBefore") LocalDate overdueBefore,
			Pageable pageable);

	boolean existsBySalesOrderId(Long salesOrderId);

}
