package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.AdminCollaboratorResponse;
import com.florastore.web_ban_hoa.dto.AdminInviteCollaboratorRequest;
import com.florastore.web_ban_hoa.dto.AdminUpdateCollaboratorRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.entity.CollaboratorBadge;
import com.florastore.web_ban_hoa.entity.CollaboratorProfile;
import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.CollaboratorProfileRepository;
import com.florastore.web_ban_hoa.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminCollaboratorService {

    private final UserRepository userRepository;
    private final CollaboratorProfileRepository collaboratorProfileRepository;

    public AdminCollaboratorService(
            UserRepository userRepository,
            CollaboratorProfileRepository collaboratorProfileRepository
    ) {
        this.userRepository = userRepository;
        this.collaboratorProfileRepository = collaboratorProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminCollaboratorResponse> getCollaborators() {
        List<User> adminUsers = userRepository.findByRoleOrderByFullNameAsc(Role.ADMIN);
        List<CollaboratorProfile> profiles = collaboratorProfileRepository.findAll();
        Map<Long, CollaboratorProfile> profilesByUserId = profiles.stream()
                .collect(Collectors.toMap(CollaboratorProfile::getUserId, profile -> profile, (left, right) -> right));

        List<Long> collaboratorUserIds = new ArrayList<>(profilesByUserId.keySet());
        Map<Long, User> collaboratorsAsUsers = collaboratorUserIds.isEmpty()
                ? new HashMap<>()
                : userRepository.findAllById(collaboratorUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<Long, AdminCollaboratorResponse> merged = new LinkedHashMap<>();

        for (User adminUser : adminUsers) {
            CollaboratorProfile profile = profilesByUserId.get(adminUser.getId());
            String title = profile != null && profile.getPositionTitle() != null && !profile.getPositionTitle().isBlank()
                    ? profile.getPositionTitle().trim()
                    : "Shop Administrator";
            String description = profile != null ? normalizeNullable(profile.getPositionDescription()) : null;

            merged.put(
                    adminUser.getId(),
                    AdminCollaboratorResponse.from(adminUser, CollaboratorBadge.ADMIN, title, description)
            );
        }

        for (Map.Entry<Long, User> entry : collaboratorsAsUsers.entrySet()) {
            Long userId = entry.getKey();
            User user = entry.getValue();
            CollaboratorProfile profile = profilesByUserId.get(userId);

            if (profile == null) {
                continue;
            }

            CollaboratorBadge badge = user.getRole() == Role.ADMIN ? CollaboratorBadge.ADMIN : profile.getBadge();
            String title = profile.getPositionTitle() != null && !profile.getPositionTitle().isBlank()
                    ? profile.getPositionTitle().trim()
                    : (badge == CollaboratorBadge.ADMIN ? "Shop Administrator" : "Shop Staff");
            String description = normalizeNullable(profile.getPositionDescription());

            merged.put(userId, AdminCollaboratorResponse.from(user, badge, title, description));
        }

        return merged.values().stream()
                .sorted(
                        Comparator
                                .comparing((AdminCollaboratorResponse item) -> item.badge() != CollaboratorBadge.ADMIN)
                                .thenComparing(AdminCollaboratorResponse::fullName, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getInviteCandidates() {
        Set<Long> collaboratorUserIds = collaboratorProfileRepository.findAllUserIds().stream()
                .collect(Collectors.toSet());

        return userRepository.findByRoleOrderByFullNameAsc(Role.USER).stream()
                .filter(user -> !collaboratorUserIds.contains(user.getId()))
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public AdminCollaboratorResponse inviteCollaborator(AdminInviteCollaboratorRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CollaboratorBadge badge = request.badge();
        String positionTitle = request.positionTitle().trim();
        String positionDescription = normalizeNullable(request.positionDescription());

        user.setRole(badge == CollaboratorBadge.ADMIN ? Role.ADMIN : Role.USER);
        User savedUser = userRepository.save(user);

        CollaboratorProfile profile = collaboratorProfileRepository.findByUserId(savedUser.getId())
                .orElseGet(CollaboratorProfile::new);
        profile.setUserId(savedUser.getId());
        profile.setBadge(badge);
        profile.setPositionTitle(positionTitle);
        profile.setPositionDescription(positionDescription);

        CollaboratorProfile savedProfile = collaboratorProfileRepository.save(profile);
        return AdminCollaboratorResponse.from(
                savedUser,
                savedProfile.getBadge(),
                savedProfile.getPositionTitle(),
                savedProfile.getPositionDescription()
        );
    }

    @Transactional
    public AdminCollaboratorResponse updateCollaborator(Long userId, AdminUpdateCollaboratorRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CollaboratorBadge badge = request.badge();
        if (badge == CollaboratorBadge.STAFF && user.getRole() == Role.ADMIN && isLastAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot demote the last admin account");
        }

        String positionTitle = request.positionTitle().trim();
        String positionDescription = normalizeNullable(request.positionDescription());

        user.setRole(badge == CollaboratorBadge.ADMIN ? Role.ADMIN : Role.USER);
        User savedUser = userRepository.save(user);

        CollaboratorProfile profile = collaboratorProfileRepository.findByUserId(savedUser.getId())
                .orElseGet(CollaboratorProfile::new);
        profile.setUserId(savedUser.getId());
        profile.setBadge(badge);
        profile.setPositionTitle(positionTitle);
        profile.setPositionDescription(positionDescription);

        CollaboratorProfile savedProfile = collaboratorProfileRepository.save(profile);
        return AdminCollaboratorResponse.from(
                savedUser,
                savedProfile.getBadge(),
                savedProfile.getPositionTitle(),
                savedProfile.getPositionDescription()
        );
    }

    @Transactional
    public void removeCollaborator(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.ADMIN && isLastAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the last admin account");
        }

        user.setRole(Role.USER);
        userRepository.save(user);
        collaboratorProfileRepository.deleteByUserId(userId);
    }

    private boolean isLastAdmin(User user) {
        return user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
