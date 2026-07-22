package com.powerpulse.core.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotificationSender(
            JavaMailSender mailSender,
            @Value("${powerpulse.mail.from}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void send(NotificationLog notification) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setFrom(fromAddress);
        mailMessage.setTo(notification.getRecipientEmail());
        mailMessage.setSubject(notification.getSubject());
        mailMessage.setText(notification.getMessage());

        mailSender.send(mailMessage);
    }
}