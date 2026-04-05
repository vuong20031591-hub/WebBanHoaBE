package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.PagedResponse;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;

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
}
