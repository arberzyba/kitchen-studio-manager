package de.sedzkitchens.quote;

public enum QuoteStatus {

	DRAFT, SENT, ACCEPTED, REJECTED;

	// A draft is sent to the customer, who then accepts or rejects it; nothing else is allowed
	public boolean canChangeTo(QuoteStatus target) {
		return switch (this) {
			case DRAFT -> target == SENT;
			case SENT -> target == ACCEPTED || target == REJECTED;
			case ACCEPTED, REJECTED -> false;
		};
	}

}
