package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.AuthResponse;
import com.florastore.web_ban_hoa.dto.LoginRequest;
import com.florastore.web_ban_hoa.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
