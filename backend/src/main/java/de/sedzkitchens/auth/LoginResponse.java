package de.sedzkitchens.auth;

import de.sedzkitchens.user.UserResponse;

public record LoginResponse(String token, UserResponse user) {
}
