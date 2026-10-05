package de.sedzkitchens.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

	// Expects a lower-case LIKE pattern such as "%spüle%"; a null category matches every category
	@EntityGraph(attributePaths = "supplier")
	@Query("""
			select p from Product p
			where (lower(p.name) like :pattern or lower(p.sku) like :pattern)
			  and (:category is null or p.category = :category)
			""")
	Page<Product> search(@Param("pattern") String pattern, @Param("category") ProductCategory category,
			Pageable pageable);

	boolean existsBySkuIgnoreCase(String sku);

	boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

}
