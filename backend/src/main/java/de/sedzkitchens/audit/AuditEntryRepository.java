package de.sedzkitchens.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

	// Null filters match everything
	@Query("""
			select a from AuditEntry a
			where (cast(:action as string) is null or a.action = :action)
			  and (cast(:entityType as string) is null or a.entityType = :entityType)
			  and (:entityId is null or a.entityId = :entityId)
			""")
	Page<AuditEntry> search(@Param("action") String action, @Param("entityType") String entityType,
			@Param("entityId") Long entityId, Pageable pageable);

}
