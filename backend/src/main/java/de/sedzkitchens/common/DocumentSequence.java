package de.sedzkitchens.common;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_sequences")
@Getter
@Setter
@NoArgsConstructor
public class DocumentSequence {

	@Id
	private String name;

	private long nextValue;

}
