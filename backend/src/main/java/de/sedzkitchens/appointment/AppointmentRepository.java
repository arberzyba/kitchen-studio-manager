package de.sedzkitchens.appointment;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

	// Everything that overlaps the given period; a null assignee matches all employees.
	// Order, customer and assignee are loaded in the same query because every calendar entry shows them.
	@EntityGraph(attributePaths = { "order", "order.quote", "order.quote.customer", "assignee" })
	@Query("""
			select a from Appointment a
			where a.startTime < :to and a.endTime > :from
			  and (:assigneeId is null or a.assignee.id = :assigneeId)
			order by a.startTime
			""")
	List<Appointment> findInPeriod(@Param("from") Instant from, @Param("to") Instant to,
			@Param("assigneeId") Long assigneeId);

	// The next appointments of one type from a point in time on; the page limits how many
	@EntityGraph(attributePaths = { "order", "order.quote", "order.quote.customer", "assignee" })
	List<Appointment> findByTypeAndStartTimeGreaterThanEqualOrderByStartTime(AppointmentType type, Instant from,
			Pageable pageable);

}
