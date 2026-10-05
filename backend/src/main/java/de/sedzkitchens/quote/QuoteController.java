package de.sedzkitchens.quote;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

// Sales and admins write quotes; office staff can read them and download the PDF
@RestController
@RequestMapping("/api/quotes")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
@RequiredArgsConstructor
public class QuoteController {

	private final QuoteService quoteService;

	public record StatusRequest(@NotNull QuoteStatus status) {
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OFFICE')")
	public Page<QuoteSummaryResponse> search(@RequestParam(defaultValue = "") String search,
			@RequestParam(required = false) QuoteStatus status,
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return quoteService.search(search, status, pageable);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OFFICE')")
	public QuoteResponse getById(@PathVariable Long id) {
		return quoteService.getById(id);
	}

	@GetMapping("/{id}/pdf")
	@PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OFFICE')")
	public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
		String fileName = "Angebot-" + quoteService.getById(id).quoteNumber() + ".pdf";
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(fileName).build().toString())
			.body(quoteService.createPdf(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public QuoteResponse create(@Valid @RequestBody QuoteRequest request, @AuthenticationPrincipal Jwt jwt) {
		return quoteService.create(request, Long.valueOf(jwt.getSubject()));
	}

	@PutMapping("/{id}")
	public QuoteResponse update(@PathVariable Long id, @Valid @RequestBody QuoteRequest request) {
		return quoteService.update(id, request);
	}

	@PostMapping("/{id}/status")
	public QuoteResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
		return quoteService.changeStatus(id, request.status());
	}

	@PostMapping("/{id}/send")
	public QuoteResponse send(@PathVariable Long id) {
		return quoteService.sendToCustomer(id);
	}

}
