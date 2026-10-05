package de.sedzkitchens.common;

import java.time.Year;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

// Issues gapless, sequential document numbers per year, e.g. AN-2026-0001, AN-2026-0002, ...
@Service
@RequiredArgsConstructor
public class DocumentNumberService {

	private final DocumentSequenceRepository sequenceRepository;

	// Runs inside the caller's transaction: if saving the document fails, the number is released again
	@Transactional
	public String next(String prefix) {
		int year = Year.now().getValue();
		String name = prefix + "-" + year;
		DocumentSequence sequence = sequenceRepository.findForUpdate(name).orElseGet(() -> {
			DocumentSequence created = new DocumentSequence();
			created.setName(name);
			created.setNextValue(1);
			return created;
		});
		long number = sequence.getNextValue();
		sequence.setNextValue(number + 1);
		sequenceRepository.save(sequence);
		return "%s-%d-%04d".formatted(prefix, year, number);
	}

}
