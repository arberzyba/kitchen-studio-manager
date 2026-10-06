package de.sedzkitchens.audit;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Read-only view of an audit log row. Rows are written by AuditRecorder and never changed afterwards.
@Entity
@Immutable
@Table(name = "audit_log")
@Getter
@NoArgsConstructor
public class AuditEntry {

	@Id
	private Long id;

	private Instant occurredAt;

	// Null when the system itself made the change
	private Long userId;

	private String action;

	private String entityType;

	private Long entityId;

	private String changedFields;

}
