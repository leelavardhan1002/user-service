package com.document.userService.service.emailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;

    @Override
    @Async("asyncTaskExecutor")
    public void sendSimpleEmail(String toEmail, String userName, String otpGenerated) {

        String subject = "Your OTP for File Matrix";
        MimeMessage message = mailSender.createMimeMessage();
        try {
            String htmlTemplate = readHtmlTemplate("templates/otp_email_template.html");
            if ("Error while reading the template".equals(htmlTemplate)) {
                log.error("EmailServiceImpl::sendSimpleEmail::Error while reading the template");
                throw new MailSendException(htmlTemplate);
            }
            htmlTemplate = htmlTemplate.replace("${user_name}", userName);
            htmlTemplate = htmlTemplate.replace("${otp_generated}", otpGenerated);
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(System.getenv().getOrDefault("EMAIL_USERNAME", "filematrix.dms@gmail.com"));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlTemplate, true);
            mailSender.send(message);
            log.info("EmailServiceImpl::sendSimpleEmail::Email sent to: {}", toEmail);
        } catch (MailException | MessagingException exception) {
            log.error("EmailServiceImpl::sendSimpleEmail:: Exception: {}", exception.getMessage());
            throw new MailSendException("Failed to send email to: " + toEmail, exception);
        }
    }

    public String readHtmlTemplate(String filePath) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + filePath);
            InputStream inputStream = resource.getInputStream();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log.error("EmailServiceImpl::readHtmlTemplate:: Exception: {}", exception.getMessage());
            return "Error while reading the template";
        }
    }
}
