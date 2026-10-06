package de.sedzkitchens.invoice;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Office staff and admins issue invoices and record payments; sales staff can look invoices up
@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICE')")
@RequiredArgsConstructor
public class InvoiceController {

	private final InvoiceService invoiceService;

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'OFFICE', 'SALES')")
	public Page<InvoiceResponse> search(@RequestParam(defaultValue = "") String search,
			@RequestParam(required = false) InvoiceStatus status, @RequestParam(required = false) Long orderId,
			@RequestParam(defaultValue = "false") boolean overdue,
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return invoiceService.search(search, status, orderId, overdue, pageable);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'OFFICE', 'SALES')")
	public InvoiceResponse getById(@PathVariable Long id) {
		return invoiceService.getById(id);
	}

	@GetMapping("/{id}/pdf")
	@PreAuthorize("hasAnyRole('ADMIN', 'OFFICE', 'SALES')")
	public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
		String fileName = "Rechnung-" + invoiceService.getById(id).invoiceNumber() + ".pdf";
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(fileName).build().toString())
			.body(invoiceService.createPdf(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InvoiceResponse create(@Valid @RequestBody InvoiceRequests.Create request) {
		return invoiceService.create(request);
	}

	@PostMapping("/{id}/send")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void send(@PathVariable Long id) {
		invoiceService.sendToCustomer(id);
	}

	@PostMapping("/{id}/payments")
	public InvoiceResponse addPayment(@PathVariable Long id, @Valid @RequestBody InvoiceRequests.AddPayment request) {
		return invoiceService.addPayment(id, request);
	}

	@DeleteMapping("/{id}/payments/{paymentId}")
	public InvoiceResponse removePayment(@PathVariable Long id, @PathVariable Long paymentId) {
		return invoiceService.removePayment(id, paymentId);
	}

}
