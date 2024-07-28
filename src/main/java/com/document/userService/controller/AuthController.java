package com.document.userService.controller;

import com.document.userService.auth.JwtUtil;
import com.document.userService.dto.*;
import com.document.userService.entity.user.User;
import com.document.userService.enums.Role;
import com.document.userService.service.userService.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/customer/login")
    public ResponseEntity<Object> customerLogin(@RequestBody LoginRequest loginReq) {
        return login(loginReq, Role.CUSTOMER);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<Object> adminLogin(@RequestBody LoginRequest loginReq) {
        return login(loginReq, Role.ADMIN);
    }

    @PostMapping("/signup")
    public ResponseEntity<Object> signup(@RequestBody SignupRequest signupRequest) {
        log.info("AuthController::signup - Attempting to sign up user with email: {}", signupRequest.getEmail());
        User existingUser = userService.findByEmail(signupRequest.getEmail());
        if (existingUser != null) {
            log.warn("AuthController::signup - Email already in use: {}", signupRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");
        }
        User user = new User();
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(signupRequest.getPassword());

        User savedUser = userService.signup(user);
        UserResponse userResponse = new UserResponse(savedUser.getUserId(), savedUser.getFirstName(), savedUser.getLastName(), savedUser.getEmail());

        log.info("AuthController::signup - User signed up successfully with email: {}", savedUser.getEmail());
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
    }

    private ResponseEntity<Object> login(LoginRequest loginReq, Role role) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(loginReq.getEmail(), loginReq.getPassword()));
            String email = authentication.getName();

            User user = userService.findByEmail(email);

            if (user.getRole() != role) {
                ErrorResponse errorResponse =
                        new ErrorResponse(
                                HttpStatus.BAD_REQUEST, "Invalid credentials for " + role.toString() + " login");
                log.error("Invalid credentials for " + role + " login");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            UserLoginResponse userDTO =
                    new UserLoginResponse(
                            user.getUserId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getRole(),
                            user.isVerified(),
                            user.isActive(),
                            user.getCreatedAt(),
                            user.getUpdatedAt());

            String token = jwtUtil.createToken(user);
            LoginResponse loginRes = new LoginResponse(token, userDTO);

            log.info(role.toString() + " logged in successfully");
            return ResponseEntity.ok(loginRes);
        } catch (Exception badCredentialsException) {
            ErrorResponse errorResponse =
                    new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid username or password");
            log.error(
                    "Error occurred during " + role.toString() + " login process", badCredentialsException);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(HttpServletRequest request) {
        try {
            String token = jwtUtil.resolveToken(request);
            if (!Objects.isNull(token)) {
                jwtUtil.revokeToken(token);
                log.info("User logged out successfully.");
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception logoutException) {
            log.error("LogoutException occurred during logout process: {}", logoutException.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
