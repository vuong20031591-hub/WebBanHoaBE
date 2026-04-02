package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestJwtHelper {

    @Autowired
    private JwtService jwtService;

    public String generateToken(String userId) {
        User mockUser = new User();
        mockUser.setId(Long.parseLong(userId));
        mockUser.setEmail("test@example.com");
        mockUser.setRole(Role.USER);
        return jwtService.generateToken(mockUser);
    }

    public String bearer(String userId) {
        return "Bearer " + generateToken(userId);
    }
}
