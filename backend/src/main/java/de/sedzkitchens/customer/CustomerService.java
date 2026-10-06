package de.sedzkitchens.customer;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.NotFoundException;
import de.sedzkitchens.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

	private final CustomerRepository customerRepository;

	private final CustomerContactRepository contactRepository;

	private final UserRepository userRepository;

	private final CustomerMapper mapper;

	public Page<CustomerResponse> search(String search, Pageable pageable) {
		String pattern = "%" + search.trim().toLowerCase() + "%";
		return customerRepository.search(pattern, pageable).map(mapper::toResponse);
	}

	public CustomerResponse getById(Long id) {
		return mapper.toResponse(findCustomer(id));
	}

	@Transactional
	public CustomerResponse create(CustomerRequest request) {
		Customer customer = new Customer();
		mapper.update(customer, request);
		return mapper.toResponse(customerRepository.save(customer));
	}

	@Transactional
	public CustomerResponse update(Long id, CustomerRequest request) {
		Customer customer = findActiveCustomer(id);
		mapper.update(customer, request);
		// Flush so the response carries the new updatedAt timestamp
		return mapper.toResponse(customerRepository.saveAndFlush(customer));
	}

	public List<ContactResponse> findContacts(Long customerId) {
		findCustomer(customerId);
		return contactRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId)
			.stream()
			.map(mapper::toResponse)
			.toList();
	}

	@Transactional
	public ContactResponse addContact(Long customerId, ContactRequest request, Long userId) {
		CustomerContact contact = new CustomerContact();
		contact.setCustomer(findActiveCustomer(customerId));
		contact.setContactType(request.contactType());
		contact.setSummary(request.summary());
		contact.setCreatedBy(userRepository.getReferenceById(userId));
		return mapper.toResponse(contactRepository.save(contact));
	}

	// For changes: a customer whose data was erased must not collect new personal data
	private Customer findActiveCustomer(Long id) {
		Customer customer = findCustomer(id);
		if (customer.getAnonymizedAt() != null) {
			throw new ConflictException("This customer's data has been erased");
		}
		return customer;
	}

	private Customer findCustomer(Long id) {
		return customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
	}

}
