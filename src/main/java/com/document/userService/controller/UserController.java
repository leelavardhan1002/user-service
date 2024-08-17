package com.document.userService.controller;

import com.document.userService.auth.JwtUtil;
import com.document.userService.config.BasePaths;
import com.document.userService.dto.request.LoginRequest;
import com.document.userService.dto.request.UpdateUser;
import com.document.userService.dto.request.VerifyOTP;
import com.document.userService.dto.response.ResetPasswordResponse;
import com.document.userService.entity.user.User;
import com.document.userService.enums.Role;
import com.document.userService.exception.EmailUpdateNotAllowedException;
import com.document.userService.exception.InvalidInputException;
import com.document.userService.exception.UserNotFoundException;
import com.document.userService.service.userService.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api" + BasePaths.BASE_PATH_USERS)
@RequiredArgsConstructor
@Slf4j
public class UserController {

    @Autowired
    UserService userService;
    @Autowired
    JwtUtil jwtUtil;

    private static final String ACTION_NOT_PERMITTED_MESSAGE = "Action not Permitted";

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        log.info("UserController::getUserById - Fetching user with ID: {}", id);
        User user = userService.getUserById(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @RequestBody LoginRequest loginRequest) {
        return userService.resetPassword(loginRequest);
    }

    @PostMapping("/verifyOtpEmail")
    public ResponseEntity<String> verifyOTPUnverifiedUser(@RequestBody VerifyOTP verifyOTP) {
        return userService.verifyOtpNoToken(verifyOTP.getEmail(), verifyOTP.getOtp());
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateUserDetails(
            @RequestBody UpdateUser updateUserDTO, HttpServletRequest request) {
        try {
            Claims user = jwtUtil.resolveClaims(request);
            User userDetails = userService.getUserById(UUID.fromString(user.getSubject()));

            if (userDetails.getRole().equals(Role.ADMIN)) {
                return new ResponseEntity<>(
                        Map.of("message", ACTION_NOT_PERMITTED_MESSAGE), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            userService.updateUserDetailsById(userDetails.getUserId(), updateUserDTO);
            log.info("Updated User Details");

            User updatedUserDetails = userService.getUserById(userDetails.getUserId());
            return ResponseEntity.ok()
                    .body(
                            Map.of("message", "User details updated successfully.", "user", updatedUserDetails));
        } catch (EmailUpdateNotAllowedException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (InvalidInputException | UserNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @SneakyThrows
    @PostMapping("/verifyOtp")
    public ResponseEntity<String> verifyOTP(@RequestParam String otp, HttpServletRequest request) {
        try {
            Claims user = jwtUtil.resolveClaims(request);
            User userDetails = userService.getUserById(UUID.fromString(user.getSubject()));

            if (userDetails.getRole().equals(Role.ADMIN)) {
                return new ResponseEntity<>(ACTION_NOT_PERMITTED_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            String status = userService.verifyOtpForUser(userDetails.getEmail(), otp);
            if (status.equals("verified")) {
                log.info("OTP verified successfully. User is now verified.");
                return new ResponseEntity<>(
                        "OTP verified successfully. User is now verified.", HttpStatus.OK);
            } else if (status.equals("User already verified")) {
                log.info(status);
                return new ResponseEntity<>(status, HttpStatus.OK);
            } else {
                log.info("not verified");
                return new ResponseEntity<>("user not verified", HttpStatus.NOT_FOUND);
            }

        } catch (RuntimeException runtimeException) {
            log.error(runtimeException.getMessage());
            return ResponseEntity.badRequest().body(runtimeException.getMessage());
        }
    }
}
