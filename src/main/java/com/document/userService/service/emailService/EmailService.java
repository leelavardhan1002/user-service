package com.document.userService.service.emailService;

public interface EmailService {
    public void sendSimpleEmail(String toEmail, String userName, String otpGenerated);
}
