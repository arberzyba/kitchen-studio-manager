package de.sedzkitchens.privacy;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

// GDPR requests are handled by admins only
@RestController
@RequestMapping("/api/customers/{id}")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CustomerDataController {

	private final CustomerDataService customerDataService;

	public record ErasureResponse(CustomerDataService.ErasureOutcome outcome) {
	}

	// Sent as a file so it can be passed on to the customer
	@GetMapping("/export")
	public ResponseEntity<CustomerDataExport> export(@PathVariable Long id) {
		String fileName = "customer-" + id + "-data.json";
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename(fileName).build().toString())
			.body(customerDataService.export(id));
	}

	@DeleteMapping
	public ErasureResponse erase(@PathVariable Long id) {
		return new ErasureResponse(customerDataService.erase(id));
	}

}
