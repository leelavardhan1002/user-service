package com.document.userService.service.userService;

import com.document.userService.dto.request.LoginRequest;
import com.document.userService.dto.request.UpdateUser;
import com.document.userService.dto.response.ResetPasswordResponse;
import com.document.userService.entity.user.User;
import com.document.userService.exception.EmailUpdateNotAllowedException;
import com.document.userService.exception.InvalidInputException;
import com.document.userService.exception.UserNotFoundException;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface UserService {
    User getUserById(UUID id);
    User findByEmail(String email);
    User signup(User user);
    String verifyOtpForUser(String email, String otp) throws Exception;
    ResponseEntity<String> verifyOtpNoToken(String email, String otp);
    ResponseEntity<ResetPasswordResponse> resetPassword(LoginRequest loginRequest);
    void updateUserDetailsById(UUID userId, UpdateUser updateUser)
            throws InvalidInputException, EmailUpdateNotAllowedException, UserNotFoundException;
}