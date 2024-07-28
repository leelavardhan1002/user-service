package com.document.userService.service.otpService;

import com.document.userService.entity.user.OtpEntity;

import java.util.concurrent.TimeUnit;

public interface OtpService {
    public String generateOtp();

    public void scheduleDeleteOtp(OtpEntity otp, long minutes, TimeUnit delayUnit);

    String otpGeneration(String email) throws Exception;

    String verifyOtp(String email, String otp) throws Exception;
}
