package com.mockhub.notification.service;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailDeliveryServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpEmailDeliveryService service;

    SmtpEmailDeliveryServiceTest() {
        // fromAddress is injected via @Value, so we construct manually in tests
    }

    @Test
    void sendEmail_givenValidInput_sendsMessageAndReturnsId() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mimeMessage.getMessageID()).thenReturn("<msg-123@resend.com>");

        SmtpEmailDeliveryService testService =
                new SmtpEmailDeliveryService(mailSender, "noreply@mockhub.dev");

        String id = testService.sendEmail("buyer@example.com", "Order Confirmed", "<h1>Hi</h1>");

        assertThat(id).isEqualTo("<msg-123@resend.com>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue()).isSameAs(mimeMessage);
    }

    @Test
    void sendEmail_givenMailException_returnsNull() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        SmtpEmailDeliveryService testService =
                new SmtpEmailDeliveryService(mailSender, "noreply@mockhub.dev");

        String id = testService.sendEmail("buyer@example.com", "Order Confirmed", "<h1>Hi</h1>");

        assertThat(id).isNull();
    }
}
