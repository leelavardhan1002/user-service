package com.document.userService.service.userService;

import com.document.userService.entity.user.User;

import java.util.UUID;

public interface UserService {
    User getUserById(UUID id);
    User saveUser(User user);
}
