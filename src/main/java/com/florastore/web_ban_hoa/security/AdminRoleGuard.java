package com.florastore.web_ban_hoa.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminRoleGuard {

    private final String roleHeader;

    public AdminRoleGuard(@Value("${security.admin-role-header:X-Role}") String roleHeader) {
        this.roleHeader = roleHeader;
    }

    public void assertAdmin(String roleValue) {
        if (roleValue == null || !"ADMIN".equalsIgnoreCase(roleValue.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }
    }

    public String getRoleHeader() {
        return roleHeader;
    }
}
