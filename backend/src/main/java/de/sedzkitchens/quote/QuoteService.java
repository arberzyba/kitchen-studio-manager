package de.sedzkitchens.quote;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.DocumentNumberService;
import de.sedzkitchens.common.NotFoundException;
import de.sedzkitchens.customer.Customer;
import de.sedzkitchens.customer.CustomerRepository;
import de.sedzkitchens.product.Product;
import de.sedzkitchens.product.ProductRepository;
import de.sedzkitchens.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

	// "AN" for Angebot, giving numbers like AN-2026-0001
	private static final String NUMBER_PREFIX = "AN";

	private final QuoteRepository quoteRepository;

	private final CustomerRepository customerRepository;

	private final ProductRepository productRepository;

	private final UserRepository userRepository;

	private final DocumentNumberService documentNumberService;

	private final QuotePdfService pdfService;

	private final QuoteMailService mailService;

	public Page<QuoteSummaryResponse> search(String search, QuoteStatus status, Pageable pageable) {
		String pattern = "%" + search.trim().toLowerCase() + "%";
		return quoteRepository.search(pattern, status, pageable).map(QuoteSummaryResponse::from);
	}

	public QuoteResponse getById(Long id) {
		return QuoteResponse.from(findQuote(id));
	}

	@Transactional
	public QuoteResponse create(QuoteRequest request, Long userId) {
		Quote quote = new Quote();
		quote.setQuoteNumber(documentNumberService.next(NUMBER_PREFIX));
		quote.setQuoteDate(LocalDate.now());
		quote.setVatRate(QuoteCalculator.VAT_RATE);
		quote.setCreatedBy(userRepository.getReferenceById(userId));
		apply(quote, request);
		return QuoteResponse.from(quoteRepository.save(quote));
	}

	@Transactional
	public QuoteResponse update(Long id, QuoteRequest request) {
		Quote quote = findQuote(id);
		if (quote.getStatus() != QuoteStatus.DRAFT) {
			throw new ConflictException("Only draft quotes can be edited");
		}
		apply(quote, request);
		return QuoteResponse.from(quote);
	}

	@Transactional
	public QuoteResponse changeStatus(Long id, QuoteStatus target) {
		Quote quote = findQuote(id);
		if (!quote.getStatus().canChangeTo(target)) {
			throw new ConflictException(
					"A quote cannot change from %s to %s".formatted(quote.getStatus(), target));
		}
		quote.setStatus(target);
		return QuoteResponse.from(quote);
	}

	public byte[] createPdf(Long id) {
		return pdfService.createPdf(findQuote(id));
	}

	// Emails the PDF to the customer. A draft becomes SENT; a quote that was already sent can be sent again.
	@Transactional
	public QuoteResponse sendToCustomer(Long id) {
		Quote quote = findQuote(id);
		if (quote.getStatus() != QuoteStatus.DRAFT && quote.getStatus() != QuoteStatus.SENT) {
			throw new ConflictException("This quote has already been accepted or rejected");
		}
		if (quote.getCustomer().getEmail() == null) {
			throw new ConflictException("The customer has no email address");
		}
		mailService.send(quote, pdfService.createPdf(quote));
		quote.setStatus(QuoteStatus.SENT);
		return QuoteResponse.from(quote);
	}

	private void apply(Quote quote, QuoteRequest request) {
		Customer customer = customerRepository.findById(request.customerId())
			.orElseThrow(() -> new NotFoundException("Customer not found"));
		if (customer.getAnonymizedAt() != null) {
			throw new ConflictException("This customer's data has been erased");
		}
		quote.setCustomer(customer);
		quote.setValidUntil(request.validUntil());
		quote.setDiscountPercent(request.discountPercent());
		quote.setNotes(request.notes());

		quote.getItems().clear();
		for (QuoteRequest.Item itemRequest : request.items()) {
			Product product = productRepository.findById(itemRequest.productId())
				.orElseThrow(() -> new NotFoundException("Product not found"));
			QuoteItem item = new QuoteItem();
			item.setQuote(quote);
			item.setProduct(product);
			item.setPosition(quote.getItems().size() + 1);
			item.setSku(product.getSku());
			item.setDescription(product.getName());
			item.setUnit(product.getUnit());
			item.setQuantity(itemRequest.quantity());
			item.setUnitPrice(itemRequest.unitPrice());
			item.setDiscountPercent(itemRequest.discountPercent());
			quote.getItems().add(item);
		}

		QuoteCalculator.Totals totals = quote.calculateTotals();
		quote.setNetTotal(totals.netTotal());
		quote.setVatAmount(totals.vatAmount());
		quote.setGrossTotal(totals.grossTotal());
	}

	private Quote findQuote(Long id) {
		return quoteRepository.findById(id).orElseThrow(() -> new NotFoundException("Quote not found"));
	}

}
