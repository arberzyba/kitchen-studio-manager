package de.sedzkitchens.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.appointment.AppointmentRepository;
import de.sedzkitchens.appointment.AppointmentResponse;
import de.sedzkitchens.appointment.AppointmentType;
import de.sedzkitchens.invoice.Invoice;
import de.sedzkitchens.invoice.InvoiceRepository;
import de.sedzkitchens.invoice.InvoiceResponse;
import de.sedzkitchens.quote.QuoteRepository;
import de.sedzkitchens.quote.QuoteStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

	private static final int REVENUE_MONTHS = 6;

	private static final int LIST_SIZE = 5;

	private final QuoteRepository quoteRepository;

	private final InvoiceRepository invoiceRepository;

	private final AppointmentRepository appointmentRepository;

	public DashboardResponse getDashboard() {
		return new DashboardResponse(openQuotes(), monthlyRevenue(), upcomingInstallations(), overdueInvoices());
	}

	private DashboardResponse.OpenQuotes openQuotes() {
		Map<QuoteStatus, QuoteRepository.StatusSummary> byStatus = quoteRepository.summarizeByStatus()
			.stream()
			.collect(Collectors.toMap(QuoteRepository.StatusSummary::getStatus, summary -> summary));
		QuoteRepository.StatusSummary drafts = byStatus.get(QuoteStatus.DRAFT);
		QuoteRepository.StatusSummary sent = byStatus.get(QuoteStatus.SENT);
		return new DashboardResponse.OpenQuotes(drafts == null ? 0 : drafts.getCount(),
				sent == null ? 0 : sent.getCount(), sent == null ? BigDecimal.ZERO : sent.getGrossTotal());
	}

	// The current month and the five before it, oldest first; months without invoices are included with zero
	private List<DashboardResponse.MonthRevenue> monthlyRevenue() {
		YearMonth first = YearMonth.now().minusMonths(REVENUE_MONTHS - 1);
		Map<YearMonth, BigDecimal> netByMonth = invoiceRepository.findByInvoiceDateGreaterThanEqual(first.atDay(1))
			.stream()
			.collect(Collectors.groupingBy(invoice -> YearMonth.from(invoice.getInvoiceDate()),
					Collectors.reducing(BigDecimal.ZERO, Invoice::getNetTotal, BigDecimal::add)));
		List<DashboardResponse.MonthRevenue> months = new ArrayList<>();
		for (int i = 0; i < REVENUE_MONTHS; i++) {
			YearMonth month = first.plusMonths(i);
			months.add(new DashboardResponse.MonthRevenue(month.toString(),
					netByMonth.getOrDefault(month, BigDecimal.ZERO)));
		}
		return months;
	}

	private List<DashboardResponse.Installation> upcomingInstallations() {
		return appointmentRepository
			.findByTypeAndStartTimeGreaterThanEqualOrderByStartTime(AppointmentType.INSTALLATION, Instant.now(),
					PageRequest.of(0, LIST_SIZE))
			.stream()
			.map(AppointmentResponse::from)
			.map(appointment -> new DashboardResponse.Installation(appointment.id(), appointment.startTime(),
					appointment.customerName(), appointment.address().city(), appointment.assigneeName(),
					appointment.orderId(), appointment.orderNumber()))
			.toList();
	}

	private DashboardResponse.OverdueInvoices overdueInvoices() {
		LocalDate today = LocalDate.now();
		// Longest overdue first
		Page<Invoice> overdue = invoiceRepository.search("%%", null, null, today,
				PageRequest.of(0, LIST_SIZE, Sort.by("dueDate")));
		return new DashboardResponse.OverdueInvoices(overdue.getTotalElements(),
				invoiceRepository.sumOverdueOpenAmount(today),
				overdue.getContent().stream().map(InvoiceResponse::summary).toList());
	}

}
