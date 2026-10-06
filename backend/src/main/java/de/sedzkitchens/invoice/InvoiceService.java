package de.sedzkitchens.invoice;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.DocumentNumberService;
import de.sedzkitchens.common.NotFoundException;
import de.sedzkitchens.customer.Customer;
import de.sedzkitchens.document.DocumentMailer;
import de.sedzkitchens.document.LetterContent;
import de.sedzkitchens.document.LetterPdfRenderer;
import de.sedzkitchens.order.OrderRepository;
import de.sedzkitchens.order.SalesOrder;
import de.sedzkitchens.quote.Quote;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

	// "RE" for Rechnung, giving numbers like RE-2026-0001. German law requires invoice numbers without gaps.
	private static final String NUMBER_PREFIX = "RE";

	private final InvoiceRepository invoiceRepository;

	private final OrderRepository orderRepository;

	private final DocumentNumberService documentNumberService;

	private final InvoicePdfService pdfService;

	private final DocumentMailer mailer;

	public Page<InvoiceResponse> search(String search, InvoiceStatus status, Long orderId, boolean overdueOnly,
			Pageable pageable) {
		String pattern = "%" + search.trim().toLowerCase() + "%";
		return invoiceRepository.search(pattern, status, orderId, overdueOnly ? LocalDate.now() : null, pageable)
			.map(InvoiceResponse::summary);
	}

	public InvoiceResponse getById(Long id) {
		return InvoiceResponse.from(findInvoice(id));
	}

	@Transactional
	public InvoiceResponse create(InvoiceRequests.Create request) {
		SalesOrder order = orderRepository.findById(request.orderId())
			.orElseThrow(() -> new NotFoundException("Order not found"));
		if (invoiceRepository.existsBySalesOrderId(order.getId())) {
			throw new ConflictException("This order has already been invoiced");
		}
		LocalDate today = LocalDate.now();
		if (request.dueDate().isBefore(today)) {
			throw new ConflictException("The due date cannot be in the past");
		}
		Quote quote = order.getQuote();
		LetterContent.Recipient recipient = LetterContent.Recipient.of(quote.getCustomer());

		Invoice invoice = new Invoice();
		invoice.setInvoiceNumber(documentNumberService.next(NUMBER_PREFIX));
		invoice.setSalesOrder(order);
		invoice.setInvoiceDate(today);
		invoice.setServiceDate(request.serviceDate());
		invoice.setDueDate(request.dueDate());
		invoice.setRecipientCompany(recipient.company());
		invoice.setRecipientName(recipient.name());
		invoice.setRecipientStreet(recipient.street());
		invoice.setRecipientPostalCode(recipient.postalCode());
		invoice.setRecipientCity(recipient.city());
		invoice.setVatRate(quote.getVatRate());
		invoice.setNetTotal(quote.getNetTotal());
		invoice.setVatAmount(quote.getVatAmount());
		invoice.setGrossTotal(quote.getGrossTotal());
		return InvoiceResponse.from(invoiceRepository.save(invoice));
	}

	@Transactional
	public InvoiceResponse addPayment(Long id, InvoiceRequests.AddPayment request) {
		Invoice invoice = findInvoice(id);
		if (request.amount().compareTo(invoice.getOpenAmount()) > 0) {
			throw new ConflictException("The payment is higher than the open amount");
		}
		Payment payment = new Payment();
		payment.setInvoice(invoice);
		payment.setAmount(request.amount());
		payment.setPaidOn(request.paidOn());
		payment.setNote(request.note());
		invoice.getPayments().add(payment);
		invoice.refreshPaymentStatus();
		// Flush so the response carries the new payment's id
		return InvoiceResponse.from(invoiceRepository.saveAndFlush(invoice));
	}

	// For correcting a payment that was entered by mistake
	@Transactional
	public InvoiceResponse removePayment(Long id, Long paymentId) {
		Invoice invoice = findInvoice(id);
		if (!invoice.getPayments().removeIf(payment -> payment.getId().equals(paymentId))) {
			throw new NotFoundException("Payment not found");
		}
		invoice.refreshPaymentStatus();
		return InvoiceResponse.from(invoice);
	}

	public byte[] createPdf(Long id) {
		return pdfService.createPdf(findInvoice(id));
	}

	public void sendToCustomer(Long id) {
		Invoice invoice = findInvoice(id);
		Customer customer = invoice.getSalesOrder().getQuote().getCustomer();
		if (customer.getEmail() == null) {
			throw new ConflictException("The customer has no email address");
		}
		String body = """
				%s,

				im Anhang erhalten Sie unsere Rechnung %s über %s.
				Bitte begleichen Sie den Betrag bis zum %s.

				Vielen Dank für Ihren Auftrag.

				%s
				""".formatted(DocumentMailer.greeting(customer), invoice.getInvoiceNumber(),
				LetterPdfRenderer.euro(invoice.getGrossTotal()),
				LetterPdfRenderer.DATE_FORMAT.format(invoice.getDueDate()), mailer.signature());
		mailer.send(customer.getEmail(), "Ihre Rechnung " + invoice.getInvoiceNumber(), body,
				"Rechnung-" + invoice.getInvoiceNumber() + ".pdf", pdfService.createPdf(invoice));
	}

	private Invoice findInvoice(Long id) {
		return invoiceRepository.findById(id).orElseThrow(() -> new NotFoundException("Invoice not found"));
	}

}
