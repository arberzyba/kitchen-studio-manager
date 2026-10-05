package de.sedzkitchens.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.user.User;
import de.sedzkitchens.user.UserRepository;
import de.sedzkitchens.user.UserResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final Duration TOKEN_LIFETIME = Duration.ofHours(8);

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtEncoder jwtEncoder;

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		// Same error for unknown email, wrong password and deactivated account, so none can be told apart
		User user = userRepository.findByEmail(request.email().toLowerCase())
			.filter(User::isActive)
			.filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
			.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
		return new LoginResponse(createToken(user), UserResponse.from(user));
	}

	private String createToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(user.getId().toString())
			.claim("role", user.getRole().name())
			.issuedAt(now)
			.expiresAt(now.plus(TOKEN_LIFETIME))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
