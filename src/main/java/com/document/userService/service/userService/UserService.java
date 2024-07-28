package com.document.userService.service.userService;

import com.document.userService.entity.user.User;

import java.util.UUID;

public interface UserService {
    User getUserById(UUID id);
    User findByEmail(String email);
    User signup(User user);
    String verifyOtpForUser(String email, String otp);
}