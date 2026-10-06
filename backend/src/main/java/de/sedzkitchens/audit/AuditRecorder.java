package de.sedzkitchens.audit;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// Writes audit entries. Plain JDBC is used because entries are written while Hibernate is in the middle of
// flushing other changes, when its own session must not be used. The insert joins the surrounding
// transaction, so an entry only exists if the change it describes was actually committed.
@Component
@RequiredArgsConstructor
public class AuditRecorder {

	public static final String CREATE = "CREATE";

	public static final String UPDATE = "UPDATE";

	public static final String DELETE = "DELETE";

	// A customer's data was handed out or erased on request (GDPR)
	public static final String EXPORT = "EXPORT";

	public static final String ANONYMIZE = "ANONYMIZE";

	private final JdbcTemplate jdbcTemplate;

	public void record(String action, String entityType, Long entityId, String changedFields) {
		jdbcTemplate.update("""
				insert into audit_log (occurred_at, user_id, action, entity_type, entity_id, changed_fields)
				values (?, ?, ?, ?, ?, ?)
				""", Timestamp.from(Instant.now()), currentUserId(), action, entityType, entityId, changedFields);
	}

	// The logged-in employee, or null when the system acts on its own (e.g. when seeding demo data)
	private static Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			return Long.valueOf(jwt.getSubject());
		}
		return null;
	}

}
