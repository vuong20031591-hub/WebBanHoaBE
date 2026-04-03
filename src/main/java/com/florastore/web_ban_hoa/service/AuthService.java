package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.LoginRequest;
import com.florastore.web_ban_hoa.dto.LoginResponse;
import com.florastore.web_ban_hoa.dto.RegisterRequest;
import com.florastore.web_ban_hoa.dto.SupabaseUserProfile;
import com.florastore.web_ban_hoa.dto.ChangePasswordRequest;
import com.florastore.web_ban_hoa.dto.UpdateProfileRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(
                request.email(),
                hashedPassword,
                request.fullName(),
                request.phone(),
                Role.USER
        );

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        UserResponse userResponse = UserResponse.from(user);

        return LoginResponse.of(token, jwtService.getExpirationMs(), userResponse);
    }

    @Transactional
    public LoginResponse loginWithSupabaseProfile(SupabaseUserProfile profile) {
        User user = userRepository.findByEmail(profile.email())
                .orElseGet(() -> userRepository.save(new User(
                        profile.email(),
                        passwordEncoder.encode("oauth-google-user"),
                        normalizeFullName(profile.fullName(), profile.email()),
                        normalizePhone(profile.phone()),
                        Role.USER
                )));

        boolean changed = false;
        String nextName = normalizeFullName(profile.fullName(), profile.email());
        if (nextName != null && !nextName.isBlank() && !nextName.equals(user.getFullName())) {
            user.setFullName(nextName);
            changed = true;
        }

        String nextPhone = normalizePhone(profile.phone());
        if (nextPhone != null && !nextPhone.isBlank() && !nextPhone.equals(user.getPhone())) {
            user.setPhone(nextPhone);
            changed = true;
        }

        if (changed) {
            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(user);
        return LoginResponse.of(token, jwtService.getExpirationMs(), UserResponse.from(user));
    }

    public UserResponse getUserById(String userId) {
        User user = parseAndLoadUser(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = parseAndLoadUser(userId);

        user.setFullName(request.fullName().trim());
        user.setPhone(normalizePhone(request.phone()));

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = parseAndLoadUser(userId);

        if (passwordEncoder.matches("oauth-google-user", user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This account uses Google sign-in. Password change is not available in this screen."
            );
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User parseAndLoadUser(String userId) {
        try {
            Long id = Long.parseLong(userId);
            return userRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user ID format");
        }
    }

    private String normalizeFullName(String fullName, String fallbackEmail) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        if (fallbackEmail != null && fallbackEmail.contains("@")) {
            return fallbackEmail.substring(0, fallbackEmail.indexOf("@"));
        }
        return "Google User";
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "0000000000";
        }
        String normalized = phone.trim();
        if (normalized.length() > 20) {
            return normalized.substring(0, 20);
        }
        return normalized;
    }
}
