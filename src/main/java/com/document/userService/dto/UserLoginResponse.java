package com.document.userService.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.document.userService.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLoginResponse {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private boolean isVerified;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
