package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.AdminCreateUserRequest;
import com.florastore.web_ban_hoa.dto.AdminUpdateUserRequest;
import com.florastore.web_ban_hoa.dto.PagedResponse;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.UserRepository;
import com.florastore.web_ban_hoa.validation.AuthValidationRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getUsers(Role role, Pageable pageable) {
        Page<User> users = role == null
                ? userRepository.findAll(pageable)
                : userRepository.findByRole(role, pageable);

        return PagedResponse.from(users.map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUser(Long actorUserId, AdminCreateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedPhone = normalizePhone(request.phone());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already used by another account");
        }

        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already used by another account");
        }

        validatePassword(request.password());

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                normalizeFullName(request.fullName()),
                normalizedPhone,
                request.role()
        );
        user.setAvatarUrl(normalizeOptionalAvatarUrl(request.avatarUrl()));

        User savedUser = userRepository.save(user);
        log.info(
                "Admin created user: actorUserId={}, targetUserId={}, role={}",
                actorUserId,
                savedUser.getId(),
                savedUser.getRole()
        );
        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long actorUserId, Long userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String normalizedEmail = normalizeEmail(request.email());
        String normalizedPhone = normalizePhone(request.phone());

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already used by another account");
        }

        if (userRepository.existsByPhoneAndIdNot(normalizedPhone, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already used by another account");
        }

        if (user.getRole() == Role.ADMIN && request.role() != Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot demote the last admin");
        }

        user.setEmail(normalizedEmail);
        user.setFullName(normalizeFullName(request.fullName()));
        user.setPhone(normalizedPhone);
        user.setRole(request.role());
        user.setAvatarUrl(normalizeOptionalAvatarUrl(request.avatarUrl()));

        User savedUser = userRepository.save(user);
        log.info(
                "Admin updated user: actorUserId={}, targetUserId={}",
                actorUserId,
                savedUser.getId()
        );
        return UserResponse.from(savedUser);
    }

    @Transactional
    public void deleteUser(Long actorUserId, Long userId) {
        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admins cannot delete their own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete the last admin");
        }

        userRepository.delete(user);
        log.info(
                "Admin deleted user: actorUserId={}, targetUserId={}, targetRole={}",
                actorUserId,
                userId,
                user.getRole()
        );
    }

    @Transactional
    public UserResponse updateUserRole(Long actorUserId, Long userId, Role role) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admins cannot change their own role"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.ADMIN && role != Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot demote the last admin");
        }

        Role previousRole = user.getRole();
        if (previousRole == role) {
            log.info("Role update skipped: actorUserId={}, targetUserId={}, role={}", actorUserId, userId, role);
            return UserResponse.from(user);
        }

        user.setRole(role);
        User savedUser = userRepository.save(user);
        log.info(
                "User role updated: actorUserId={}, targetUserId={}, fromRole={}, toRole={}",
                actorUserId,
                userId,
                previousRole,
                role
        );
        return UserResponse.from(savedUser);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        try {
            return AuthValidationRules.normalizePhone(phone);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private void validatePassword(String password) {
        try {
            AuthValidationRules.validatePassword(password);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private String normalizeFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }

        return fullName.trim();
    }

    private String normalizeOptionalAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        return avatarUrl.trim();
    }
}
