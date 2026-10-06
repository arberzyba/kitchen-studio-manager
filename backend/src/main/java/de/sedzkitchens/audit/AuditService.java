package de.sedzkitchens.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.user.User;
import de.sedzkitchens.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

	private final AuditEntryRepository auditEntryRepository;

	private final UserRepository userRepository;

	// userName is null for changes made by the system itself
	public record AuditEntryResponse(Long id, Instant occurredAt, Long userId, String userName, String action,
			String entityType, Long entityId, String changedFields) {
	}

	public Page<AuditEntryResponse> search(String action, String entityType, Long entityId, Pageable pageable) {
		Page<AuditEntry> entries = auditEntryRepository.search(action, entityType, entityId, pageable);
		// One query for the names of everyone on this page
		Map<Long, String> userNames = userRepository
			.findAllById(entries.stream().map(AuditEntry::getUserId).filter(Objects::nonNull).distinct().toList())
			.stream()
			.collect(Collectors.toMap(User::getId, user -> user.getFirstName() + " " + user.getLastName()));
		return entries.map(entry -> new AuditEntryResponse(entry.getId(), entry.getOccurredAt(), entry.getUserId(),
				userNames.get(entry.getUserId()), entry.getAction(), entry.getEntityType(), entry.getEntityId(),
				entry.getChangedFields()));
	}

}
