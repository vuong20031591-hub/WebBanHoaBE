package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.ChangePasswordRequest;
import com.florastore.web_ban_hoa.dto.UpdateProfileRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.UserRepository;
import com.florastore.web_ban_hoa.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = getUserOrThrow(email);
        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserOrThrow(email);
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user = userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserOrThrow(email);
        
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
