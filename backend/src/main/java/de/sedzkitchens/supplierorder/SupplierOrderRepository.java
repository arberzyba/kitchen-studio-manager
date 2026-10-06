package de.sedzkitchens.supplierorder;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long> {

	// Expects a lower-case LIKE pattern such as "%rheinland%"; a null status or order id matches everything
	@EntityGraph(attributePaths = { "supplier", "salesOrder", "salesOrder.quote", "salesOrder.quote.customer" })
	@Query("""
			select s from SupplierOrder s join s.supplier sup join s.salesOrder o join o.quote q join q.customer c
			where (lower(s.orderNumber) like :pattern
			    or lower(sup.name) like :pattern
			    or lower(o.orderNumber) like :pattern
			    or lower(concat(c.firstName, ' ', c.lastName)) like :pattern)
			  and (:status is null or s.status = :status)
			  and (:salesOrderId is null or o.id = :salesOrderId)
			""")
	Page<SupplierOrder> search(@Param("pattern") String pattern, @Param("status") SupplierOrderStatus status,
			@Param("salesOrderId") Long salesOrderId, Pageable pageable);

	List<SupplierOrder> findBySalesOrderId(Long salesOrderId);

}
