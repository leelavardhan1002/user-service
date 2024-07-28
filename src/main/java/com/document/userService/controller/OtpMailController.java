package com.document.userService.controller;

import com.document.userService.auth.JwtUtil;
import com.document.userService.config.BasePaths;
import com.document.userService.dto.GenerateOtpRequest;
import com.document.userService.entity.user.User;
import com.document.userService.enums.Role;
import com.document.userService.exception.UserNotFoundException;
import com.document.userService.service.otpService.OtpService;
import com.document.userService.service.userService.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api" + BasePaths.BASE_PATH)
@RequiredArgsConstructor
@Slf4j
public class OtpMailController {

    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    UserService userService;
    @Autowired
    OtpService otpService;

    private static final String ACTION_NOT_PERMITTED_MESSAGE = "Action not Permitted";

    @SneakyThrows
    @PostMapping("/sendOtp")
    public ResponseEntity<String> generateOtp(HttpServletRequest request) {

        Claims user = jwtUtil.resolveClaims(request);
        User userDetails = userService.getUserById(UUID.fromString(user.getSubject()));
        if (userDetails == null)
            return new ResponseEntity<>("User details not found", HttpStatus.NOT_FOUND);
        if (userDetails.getRole().equals(Role.ADMIN)) {
            return new ResponseEntity<>(ACTION_NOT_PERMITTED_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            String status = otpService.otpGeneration(userDetails.getEmail());
            if (status.equals("OTP sent successfully")) {
                log.info("OTP generated and saved successfully.");
                return new ResponseEntity<>("OTP generated and saved successfully.", HttpStatus.OK);
            } else {
                log.info("otp not sent");
                return new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (UserNotFoundException userNotFoundException) {
            log.info(userNotFoundException.getMessage());
            return new ResponseEntity<>(userNotFoundException.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @SneakyThrows
    @PostMapping("/sendOtpEmail")
    public ResponseEntity<String> generateOtpWithoutToken(
            @RequestBody GenerateOtpRequest generateOtpRequest) {
        try {
            otpService.otpGeneration(generateOtpRequest.getEmail());
            return ResponseEntity.ok("OTP generated and saved");
        } catch (MailException mailException) {
            return new ResponseEntity<>("internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @SneakyThrows
    @PostMapping("/verify-otp")
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
