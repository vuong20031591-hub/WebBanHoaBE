package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long id);
    List<User> findByRoleOrderByFullNameAsc(Role role);
    Page<User> findByRole(Role role, Pageable pageable);
    long countByRole(Role role);
}
