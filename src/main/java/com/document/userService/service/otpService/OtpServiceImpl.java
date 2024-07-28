package com.document.userService.service.otpService;

import com.document.userService.entity.user.OtpEntity;
import com.document.userService.entity.user.User;
import com.document.userService.exception.OtpExpiredException;
import com.document.userService.exception.OtpNotFoundException;
import com.document.userService.exception.UserNotFoundException;
import com.document.userService.repository.OtpRepository;
import com.document.userService.repository.UserRepository;
import com.document.userService.service.emailService.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {
    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Override
    public String generateOtp() {
        return String.valueOf(1000 + new SecureRandom().nextInt(9000));
    }

    @Override
    public void scheduleDeleteOtp(OtpEntity otp, long minutes, TimeUnit delayUnit) {
        executor.schedule(() -> {
            otpRepository.delete(otp);
            log.info("OtpServiceImpl::scheduleDeleteOtp::Deleted OTP for user: {}", otp.getUser().getEmail());
        }, minutes, delayUnit);
    }

    @Override
    public String otpGeneration(String email) {
        User user = userRepository.findByEmail(email);
        if (Objects.isNull(user)) {
            log.error("OtpServiceImpl::otpGeneration::Email not in system for any user: {}", email);
            throw new UserNotFoundException("Email not in system for any user");
        }

        OtpEntity oldOtp = otpRepository.findByUserId(user.getUserId());
        if (!Objects.isNull(oldOtp)) {
            otpRepository.delete(oldOtp);
            log.info("OtpServiceImpl::otpGeneration::Deleted old OTP for user: {}", email);
        }

        String otpGenerated = generateOtp();
        OtpEntity newOtp = new OtpEntity(otpGenerated, user);
        otpRepository.save(newOtp);
        log.info("OtpServiceImpl::otpGeneration::Saved new OTP for user: {}", email);

        emailService.sendSimpleEmail(email, user.getFirstName(), otpGenerated);
        log.info("OtpServiceImpl::otpGeneration::Sent OTP email to user: {}", email);

        scheduleDeleteOtp(newOtp, 2, TimeUnit.MINUTES);
        return "OTP sent successfully";
    }

    @Override
    public String verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email);
        if (Objects.isNull(user)) {
            log.error("OtpServiceImpl::verifyOtp::Email not in system for any user: {}", email);
            throw new UserNotFoundException("Email not in system for any user");
        }

        OtpEntity reqOtp = otpRepository.findByUserId(user.getUserId());
        if (Objects.isNull(reqOtp)) {
            log.error("OtpServiceImpl::verifyOtp::OTP for the user not found: {}", email);
            throw new OtpNotFoundException("OTP for the user not found");
        }

        Duration duration = Duration.between(reqOtp.getCreatedAt(), LocalDateTime.now());
        if (duration.getSeconds() > 120) {
            otpRepository.delete(reqOtp);
            log.error("OtpServiceImpl::verifyOtp::OTP expired for user: {}", email);
            throw new OtpExpiredException("OTP expired");
        }

        if (reqOtp.getOtp().equals(otp)) {
            otpRepository.delete(reqOtp);
            log.info("OtpServiceImpl::verifyOtp::OTP verified successfully for user: {}", email);
            return "correct";
        }

        log.error("OtpServiceImpl::verifyOtp::Invalid OTP for user: {}", email);
        return "not correct";
    }
}
