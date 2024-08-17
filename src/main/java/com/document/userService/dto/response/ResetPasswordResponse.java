package com.document.userService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class ResetPasswordResponse {
    private String message;
    private HttpStatus statusCode;
}
