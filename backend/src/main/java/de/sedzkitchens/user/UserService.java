package de.sedzkitchens.user;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.sedzkitchens.common.ConflictException;
import de.sedzkitchens.common.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	public List<UserResponse> findAll() {
		return userRepository.findAll(Sort.by("lastName", "firstName")).stream().map(UserResponse::from).toList();
	}

	public List<AssignableUserResponse> findAssignable() {
		return userRepository.findByActiveTrueOrderByLastNameAscFirstNameAsc()
			.stream()
			.map(user -> new AssignableUserResponse(user.getId(), user.getFirstName(), user.getLastName(),
					user.getRole()))
			.toList();
	}

	public UserResponse getById(Long id) {
		return UserResponse.from(findUser(id));
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		String email = request.email().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("A user with this email already exists");
		}
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFirstName(request.firstName());
		user.setLastName(request.lastName());
		user.setRole(request.role());
		return UserResponse.from(userRepository.save(user));
	}

	@Transactional
	public UserResponse update(Long id, UpdateUserRequest request, Long currentUserId) {
		User user = findUser(id);
		// Prevents an admin from locking themselves out of user management
		if (id.equals(currentUserId) && (request.role() != user.getRole() || !request.active())) {
			throw new ConflictException("You cannot change your own role or deactivate your own account");
		}
		user.setFirstName(request.firstName());
		user.setLastName(request.lastName());
		user.setRole(request.role());
		user.setActive(request.active());
		return UserResponse.from(user);
	}

	private User findUser(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
	}

}
