package de.sedzkitchens.audit;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

import de.sedzkitchens.appointment.Appointment;
import de.sedzkitchens.customer.Customer;
import de.sedzkitchens.customer.CustomerContact;
import de.sedzkitchens.invoice.Invoice;
import de.sedzkitchens.invoice.Payment;
import de.sedzkitchens.order.SalesOrder;
import de.sedzkitchens.product.Product;
import de.sedzkitchens.quote.Quote;
import de.sedzkitchens.supplier.Supplier;
import de.sedzkitchens.supplierorder.SupplierOrder;
import de.sedzkitchens.user.User;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

// Records every insert, update and delete of the business records below, without the services having to
// remember to do so. Hibernate calls this listener whenever it writes one of them to the database.
@Component
@RequiredArgsConstructor
public class AuditEventListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

	// Line items are left out: they only change together with their quote or supplier order
	private static final Set<Class<?>> AUDITED = Set.of(User.class, Customer.class, CustomerContact.class,
			Supplier.class, Product.class, Quote.class, SalesOrder.class, Appointment.class, SupplierOrder.class,
			Invoice.class, Payment.class);

	// Bookkeeping fields that change with every update and say nothing about what the user did
	private static final Set<String> IGNORED_FIELDS = Set.of("updatedAt");

	private final EntityManagerFactory entityManagerFactory;

	private final AuditRecorder recorder;

	@PostConstruct
	void register() {
		EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
			.getServiceRegistry()
			.getService(EventListenerRegistry.class);
		registry.appendListeners(EventType.POST_INSERT, this);
		registry.appendListeners(EventType.POST_UPDATE, this);
		registry.appendListeners(EventType.POST_DELETE, this);
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		record(AuditRecorder.CREATE, event.getEntity(), event.getId(), null);
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		// Only the names of the changed fields are kept, never their old or new values
		String[] names = event.getPersister().getPropertyNames();
		int[] dirty = event.getDirtyProperties();
		String changedFields = dirty == null ? null
				: Arrays.stream(dirty)
					.mapToObj(index -> names[index])
					.filter(name -> !IGNORED_FIELDS.contains(name))
					.collect(Collectors.joining(", "));
		if (changedFields == null || !changedFields.isEmpty()) {
			record(AuditRecorder.UPDATE, event.getEntity(), event.getId(), changedFields);
		}
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		record(AuditRecorder.DELETE, event.getEntity(), event.getId(), null);
	}

	// Entries are written inside the transaction, not after it
	@Override
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		return false;
	}

	private void record(String action, Object entity, Object id, String changedFields) {
		if (AUDITED.contains(entity.getClass())) {
			recorder.record(action, entity.getClass().getSimpleName(), (Long) id, changedFields);
		}
	}

}
