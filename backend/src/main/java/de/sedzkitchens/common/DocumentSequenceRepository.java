package de.sedzkitchens.common;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, String> {

	// Locks the row until the transaction ends, so two documents can never get the same number
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from DocumentSequence s where s.name = :name")
	Optional<DocumentSequence> findForUpdate(@Param("name") String name);

}
