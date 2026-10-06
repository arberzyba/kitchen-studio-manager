package de.sedzkitchens.privacy;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.appointment.AppointmentRepository;
import de.sedzkitchens.appointment.AppointmentResponse;
import de.sedzkitchens.audit.AuditRecorder;
import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.NotFoundException;
import de.sedzkitchens.customer.Address;
import de.sedzkitchens.customer.Customer;
import de.sedzkitchens.customer.CustomerContact;
import de.sedzkitchens.customer.CustomerContactRepository;
import de.sedzkitchens.customer.CustomerMapper;
import de.sedzkitchens.customer.CustomerRepository;
import de.sedzkitchens.customer.Salutation;
import de.sedzkitchens.invoice.InvoiceRepository;
import de.sedzkitchens.invoice.InvoiceResponse;
import de.sedzkitchens.order.OrderRepository;
import de.sedzkitchens.order.OrderSummaryResponse;
import de.sedzkitchens.quote.Quote;
import de.sedzkitchens.quote.QuoteRepository;
import de.sedzkitchens.quote.QuoteResponse;
import lombok.RequiredArgsConstructor;

// Handles the two requests a customer can make under the GDPR: "what do you store about me?" and "delete it"
@Service
@RequiredArgsConstructor
public class CustomerDataService {

	public enum ErasureOutcome {

		// Nothing had to be kept, so the customer is gone completely
		DELETED,
		// Business documents had to be kept, so only the personal data was removed
		ANONYMIZED

	}

	private final CustomerRepository customerRepository;

	private final CustomerContactRepository contactRepository;

	private final QuoteRepository quoteRepository;

	private final OrderRepository orderRepository;

	private final AppointmentRepository appointmentRepository;

	private final InvoiceRepository invoiceRepository;

	private final CustomerMapper customerMapper;

	private final AuditRecorder auditRecorder;

	// Not read-only: handing out the data is itself recorded in the audit log
	@Transactional
	public CustomerDataExport export(Long customerId) {
		Customer customer = findCustomer(customerId);
		auditRecorder.record(AuditRecorder.EXPORT, Customer.class.getSimpleName(), customerId, null);
		return new CustomerDataExport(Instant.now(), customerMapper.toResponse(customer),
				contactRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId)
					.stream()
					.map(customerMapper::toResponse)
					.toList(),
				quoteRepository.findByCustomerId(customerId).stream().map(QuoteResponse::from).toList(),
				orderRepository.findByQuoteCustomerId(customerId).stream().map(OrderSummaryResponse::from).toList(),
				appointmentRepository.findByOrderQuoteCustomerId(customerId)
					.stream()
					.map(AppointmentResponse::from)
					.toList(),
				invoiceRepository.findBySalesOrderQuoteCustomerId(customerId)
					.stream()
					.map(InvoiceResponse::from)
					.toList());
	}

	// Erases a customer's personal data (GDPR Art. 17). Quotes, orders and invoices are business records that
	// German commercial and tax law requires the company to keep for years, which the GDPR allows
	// (Art. 17(3)(b)). So a customer with such documents is anonymized instead of deleted: the documents stay,
	// and issued invoices keep the recipient data they were issued with.
	@Transactional
	public ErasureOutcome erase(Long customerId) {
		Customer customer = findCustomer(customerId);
		if (customer.getAnonymizedAt() != null) {
			throw new ConflictException("This customer's data has already been erased");
		}
		List<CustomerContact> contacts = contactRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId);
		contactRepository.deleteAll(contacts);

		List<Quote> quotes = quoteRepository.findByCustomerId(customerId);
		if (quotes.isEmpty()) {
			customerRepository.delete(customer);
			return ErasureOutcome.DELETED;
		}

		customer.setSalutation(Salutation.NONE);
		customer.setFirstName("Anonymisiert");
		customer.setLastName("#" + customerId);
		customer.setCompanyName(null);
		customer.setEmail(null);
		customer.setPhone(null);
		Address placeholder = new Address();
		placeholder.setStreet("-");
		placeholder.setPostalCode("00000");
		placeholder.setCity("-");
		customer.setBillingAddress(placeholder);
		customer.setInstallationAddress(null);
		customer.setAnonymizedAt(Instant.now());
		// Free-text notes on quotes may mention the customer
		quotes.forEach(quote -> quote.setNotes(null));
		auditRecorder.record(AuditRecorder.ANONYMIZE, Customer.class.getSimpleName(), customerId, null);
		return ErasureOutcome.ANONYMIZED;
	}

	private Customer findCustomer(Long id) {
		return customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
	}

}
