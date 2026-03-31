package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.ChangePasswordRequest;
import com.florastore.web_ban_hoa.dto.UpdateProfileRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(String email);
    UserResponse updateProfile(String email, UpdateProfileRequest request);
    void changePassword(String email, ChangePasswordRequest request);
}
