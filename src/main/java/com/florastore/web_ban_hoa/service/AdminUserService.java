package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(Role role) {
        List<User> users = role == null
                ? userRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName"))
                : userRepository.findByRoleOrderByFullNameAsc(role);

        return users.stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setRole(role);
        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }
}
