package de.sedzkitchens.user;

// What colleagues need to assign someone to an appointment; deliberately without the email address
public record AssignableUserResponse(Long id, String firstName, String lastName, Role role) {
}
