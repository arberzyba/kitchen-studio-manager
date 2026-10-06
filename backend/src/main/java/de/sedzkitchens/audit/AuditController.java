package de.sedzkitchens.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

// Only admins can see who changed what
@RestController
@RequestMapping("/api/audit-log")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

	private final AuditService auditService;

	@GetMapping
	public Page<AuditService.AuditEntryResponse> search(@RequestParam(required = false) String action,
			@RequestParam(required = false) String entityType, @RequestParam(required = false) Long entityId,
			@PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return auditService.search(action, entityType, entityId, pageable);
	}

}
