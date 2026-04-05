package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.AdminCollaboratorResponse;
import com.florastore.web_ban_hoa.dto.CollaboratorBadgeResponse;
import com.florastore.web_ban_hoa.dto.AdminInviteCollaboratorRequest;
import com.florastore.web_ban_hoa.dto.AdminUpdateCollaboratorRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.entity.CollaboratorBadge;
import com.florastore.web_ban_hoa.entity.CollaboratorProfile;
import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.CollaboratorProfileRepository;
import com.florastore.web_ban_hoa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminCollaboratorService {

    private static final Logger log = LoggerFactory.getLogger(AdminCollaboratorService.class);

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
        Map<Long, AdminCollaboratorResponse> collaboratorsByUserId = buildAdminResponsesByUserId();

        collaboratorProfileRepository.findCollaboratorUserViews().forEach(view ->
                collaboratorsByUserId.put(view.getUserId(), toCollaboratorResponse(view))
        );

        return sortCollaborators(collaboratorsByUserId.values());
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
    public AdminCollaboratorResponse inviteCollaborator(long actorUserId, AdminInviteCollaboratorRequest request) {
        User user = loadUserOrThrow(request.userId());
        AdminCollaboratorResponse response = upsertCollaboratorProfile(
                user,
                request.badge(),
                request.positionTitle(),
                request.positionDescription()
        );
        log.info(
                "Collaborator invited/updated: actorUserId={}, targetUserId={}, badge={}, positionTitle={}",
                actorUserId,
                user.getId(),
                request.badge(),
                response.positionTitle()
        );
        return response;
    }

    @Transactional
    public AdminCollaboratorResponse updateCollaborator(
            long actorUserId,
            Long userId,
            AdminUpdateCollaboratorRequest request
    ) {
        User user = loadUserOrThrow(userId);
        ensureCollaboratorExists(user);

        AdminCollaboratorResponse response = upsertCollaboratorProfile(
                user,
                request.badge(),
                request.positionTitle(),
                request.positionDescription()
        );
        log.info(
                "Collaborator updated: actorUserId={}, targetUserId={}, badge={}, positionTitle={}",
                actorUserId,
                user.getId(),
                request.badge(),
                response.positionTitle()
        );
        return response;
    }

    @Transactional
    public void removeCollaborator(long actorUserId, Long userId) {
        User user = loadUserOrThrow(userId);
        ensureCollaboratorExists(user);

        ensureCanRemoveCollaborator(user);

        user.setRole(Role.USER);
        userRepository.save(user);
        collaboratorProfileRepository.deleteByUserId(userId);
        log.info("Collaborator removed: actorUserId={}, targetUserId={}", actorUserId, userId);
    }

    private Map<Long, AdminCollaboratorResponse> buildAdminResponsesByUserId() {
        Map<Long, AdminCollaboratorResponse> adminsByUserId = new LinkedHashMap<>();

        userRepository.findByRoleOrderByFullNameAsc(Role.ADMIN).forEach(adminUser ->
                adminsByUserId.put(
                        adminUser.getId(),
                        AdminCollaboratorResponse.from(
                                adminUser,
                                CollaboratorBadge.ADMIN.name(),
                                "Shop Administrator",
                                null
                        )
                )
        );

        return adminsByUserId;
    }

    private AdminCollaboratorResponse toCollaboratorResponse(CollaboratorProfileRepository.CollaboratorUserView view) {
        Role userRole = parseRole(view.getUserRole());
        CollaboratorBadge badge = userRole == Role.ADMIN ? CollaboratorBadge.ADMIN : parseBadge(view.getBadge());
        return AdminCollaboratorResponse.fromValues(
                view.getUserId(),
                view.getEmail(),
                view.getFullName(),
                view.getPhone(),
                userRole.name(),
                badge.name(),
                view.getPositionTitle(),
                view.getPositionDescription()
        );
    }

    private List<AdminCollaboratorResponse> sortCollaborators(Collection<AdminCollaboratorResponse> collaborators) {
        return collaborators.stream()
                .sorted(
                        Comparator
                                .comparing((AdminCollaboratorResponse item) -> item.badge() != CollaboratorBadgeResponse.ADMIN)
                                .thenComparing(AdminCollaboratorResponse::fullName, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();
    }

    private AdminCollaboratorResponse upsertCollaboratorProfile(
            User user,
            CollaboratorBadge badge,
            String positionTitleRaw,
            String positionDescriptionRaw
    ) {
        ensureCanDemoteAdmin(user, badge);

        String positionTitle = normalizePositionTitle(positionTitleRaw, badge);
        String positionDescription = normalizeNullable(positionDescriptionRaw);

        user.setRole(resolveRole(badge));
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
                savedProfile.getBadge() != null ? savedProfile.getBadge().name() : null,
                savedProfile.getPositionTitle(),
                savedProfile.getPositionDescription()
        );
    }

    private User loadUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Role resolveRole(CollaboratorBadge badge) {
        return badge == CollaboratorBadge.ADMIN ? Role.ADMIN : Role.USER;
    }

    private void ensureCollaboratorExists(User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (collaboratorProfileRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collaborator not found");
    }

    private void ensureCanDemoteAdmin(User user) {
        if (isLastAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot demote the last admin account");
        }
    }

    private void ensureCanRemoveCollaborator(User user) {
        if (isLastAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the last admin account");
        }
    }

    private void ensureCanDemoteAdmin(User user, CollaboratorBadge targetBadge) {
        if (targetBadge == CollaboratorBadge.STAFF) {
            ensureCanDemoteAdmin(user);
        }
    }

    private boolean isLastAdmin(User user) {
        return user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1;
    }

    private Role parseRole(String roleRaw) {
        if (roleRaw == null || roleRaw.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid collaborator role data in database"
            );
        }

        try {
            return Role.valueOf(roleRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unsupported collaborator role value: " + roleRaw
            );
        }
    }

    private CollaboratorBadge parseBadge(String badgeRaw) {
        if (badgeRaw == null || badgeRaw.isBlank()) {
            return CollaboratorBadge.STAFF;
        }

        try {
            return CollaboratorBadge.valueOf(badgeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unsupported collaborator badge value: " + badgeRaw
            );
        }
    }

    private String normalizePositionTitle(String value, CollaboratorBadge badge) {
        String normalized = normalizeNullable(value);
        if (normalized != null) {
            return normalized;
        }
        return badge == CollaboratorBadge.ADMIN ? "Shop Administrator" : "Shop Staff";
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
