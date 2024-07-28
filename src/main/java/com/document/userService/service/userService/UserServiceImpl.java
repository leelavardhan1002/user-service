package com.document.userService.service.userService;

import com.document.userService.entity.user.User;
import com.document.userService.exception.UserAlreadyExistsException;
import com.document.userService.exception.UserNotFoundException;
import com.document.userService.repository.UserRepository;
import com.document.userService.service.otpService.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    OtpService otpService;

    @Override
    public User getUserById(UUID id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            log.info("UserServiceImpl::getUserById::User found with ID: {}", id);
            return user.get();
        } else {
            log.error("UserServiceImpl::getUserById::User not found with ID: {}", id);
            throw new UserNotFoundException("User not found with ID: " + id);
        }
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
    @SneakyThrows
    public String verifyOtpForUser(String email, String otp) {

        User user = userRepository.findByEmail(email);
        if (user == null) throw new UserNotFoundException("user not found");
        if (user.isVerified()) return "User already verified";

        try {
            String status = otpService.verifyOtp(email, otp);

            if (status.equals("correct")) {
                User newUser = userRepository.findByEmail(email);
                newUser.setVerified(true);
                userRepository.save(newUser);
                return "verified";
            }
            return "not verified";
        } catch (Exception exception) {
            log.error("UserServiceImpl::verifyOtpForUser::Exception: {}", exception.getMessage());
            return exception.getMessage();
        }
    }
}
