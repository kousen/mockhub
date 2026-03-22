package com.mockhub.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Profile("email-smtp")
@Primary
public class SmtpEmailDeliveryService implements EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailDeliveryService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailDeliveryService(JavaMailSender mailSender,
                                     @Value("${mockhub.email.from-address}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public String sendEmail(String toAddress, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            String messageId = message.getMessageID();
            log.info("Email sent to {} — Message-ID: {}", toAddress, messageId);
            return messageId;
        } catch (MessagingException | MailException exception) {
            log.error("Failed to send email to {}: {}", toAddress, exception.getMessage(), exception);
            return null;
        }
    }
}
