package de.sedzkitchens.appointment;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import de.sedzkitchens.order.SalesOrder;
import de.sedzkitchens.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private SalesOrder order;

	@Enumerated(EnumType.STRING)
	@Column(name = "appointment_type")
	private AppointmentType type;

	private Instant startTime;

	private Instant endTime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private User assignee;

	private String notes;

	@CreationTimestamp
	private Instant createdAt;

}
