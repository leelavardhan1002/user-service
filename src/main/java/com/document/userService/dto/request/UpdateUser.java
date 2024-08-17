package com.document.userService.dto.request;

import lombok.Data;

@Data
public class UpdateUser {
    private String firstName;
    private String lastName;
    private String email;
    private boolean isVerified;
}
