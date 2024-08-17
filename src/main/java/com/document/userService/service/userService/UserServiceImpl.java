package com.document.userService.service.userService;

import com.document.userService.dto.request.LoginRequest;
import com.document.userService.dto.request.UpdateUser;
import com.document.userService.dto.response.ResetPasswordResponse;
import com.document.userService.entity.user.User;
import com.document.userService.exception.*;
import com.document.userService.repository.UserRepository;
import com.document.userService.service.otpService.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final OtpService otpService;

    @Override
    public User getUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.error("UserServiceImpl::getUserById::User not found with ID: {}", id);
            return new UserNotFoundException("User not found with ID: " + id);
        });
    }

    @Override
    public User signup(User user) {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            log.error("UserServiceImpl::signup::Email already in use: {}", user.getEmail());
            throw new UserAlreadyExistsException("Email already in use: " + user.getEmail());
        }
        User savedUser = userRepository.save(user);
        log.info("UserServiceImpl::signup::User signed up with email: {}", user.getEmail());
        return savedUser;
    }

    @Override
    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            log.info("UserServiceImpl::findByEmail::User found with email: {}", email);
        } else {
            log.info("UserServiceImpl::findByEmail::User not found with email: {}", email);
        }
        return user;
    }

    @Override
    @Transactional
    public String verifyOtpForUser(String email, String otp) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null) throw new UserNotFoundException("User not found");

        if (user.isVerified()) return "User already verified";

        String status = otpService.verifyOtp(email, otp);

        if (status.equals("correct")) {
            user.setVerified(true);
            userRepository.save(user);
            log.info("UserServiceImpl::verifyOtpForUser::User verified with email: {}", email);
            return "verified";
        }
        log.warn("UserServiceImpl::verifyOtpForUser::OTP verification failed for email: {}", email);
        return "not verified";
    }

    @Override
    @Transactional
    public ResponseEntity<ResetPasswordResponse> resetPassword(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());

        if (user == null) {
            log.error("UserServiceImpl::resetPassword::User not found with email: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResetPasswordResponse("User not found", HttpStatus.NOT_FOUND));
        }

        if (!user.isVerified()) {
            log.error("UserServiceImpl::resetPassword::Email not verified for email: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResetPasswordResponse("Email not verified", HttpStatus.BAD_REQUEST));
        }

        // Here, you should hash the password before saving it
        user.setPassword(loginRequest.getPassword());
        userRepository.save(user);

        log.info("UserServiceImpl::resetPassword::Password updated successfully for email: {}", loginRequest.getEmail());
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResetPasswordResponse("Password updated successfully", HttpStatus.OK));
    }

    @Override
    @Transactional
    public ResponseEntity<String> verifyOtpNoToken(String email, String otp) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            log.error("UserServiceImpl::verifyOtpNoToken::User not found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        try {
            String status = otpService.verifyOtp(email, otp);

            if (status.equals("correct")) {
                user.setVerified(true);
                userRepository.save(user);
                log.info("UserServiceImpl::verifyOtpNoToken::User verified with email: {}", email);
                return ResponseEntity.status(HttpStatus.OK).body("Verified");
            }

            log.warn("UserServiceImpl::verifyOtpNoToken::OTP verification failed for email: {}", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not Verified");

        } catch (UserNotFoundException | OtpNotFoundException ex) {
            log.error("UserServiceImpl::verifyOtpNoToken::Exception: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception e) {
            log.error("UserServiceImpl::verifyOtpNoToken::RuntimeException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateUserDetailsById(UUID userId, UpdateUser updateUser) throws InvalidInputException, EmailUpdateNotAllowedException {
        User user = userRepository.findByUserId(userId);

        if (user == null) {
            log.error("UserServiceImpl::updateUserDetailsById::User not found with the provided ID");
            throw new UserNotFoundException("User not found with the provided ID.");
        }

        if (!StringUtils.hasText(updateUser.getFirstName())) {
            log.error("UserServiceImpl::updateUserDetailsById::Please provide a valid first name");
            throw new InvalidInputException("Please provide a valid first name.");
        }

        user.setFirstName(updateUser.getFirstName());
        user.setLastName(updateUser.getLastName());

        if (!user.isVerified() && StringUtils.hasText(updateUser.getEmail())) {
            user.setEmail(updateUser.getEmail());
        } else if (user.isVerified() && StringUtils.hasText(updateUser.getEmail()) &&
                !updateUser.getEmail().equals(user.getEmail())) {
            log.error("UserServiceImpl::updateUserDetailsById::Email is already verified and cannot be changed");
            throw new EmailUpdateNotAllowedException("Email is already verified and cannot be changed.");
        }

        if (updateUser.isVerified()) {
            user.setVerified(true);
        }

        userRepository.save(user);
        log.info("UserServiceImpl::updateUserDetailsById::User details updated for userID: {}", userId);
    }
}
