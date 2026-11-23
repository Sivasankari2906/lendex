package com.example.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;

    /** Sender address: the Gmail account in MAIL_USERNAME (Gmail sends as that account anyway). */
    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:lendex.team@gmail.com}")
    private String fromAddress;
    
    public void sendEmail(String toEmail, String subject, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromAddress);
            email.setTo(toEmail);
            email.setSubject(subject);
            email.setText(message);
            mailSender.send(email);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
    
    /** @return false if the message could not be handed to the mail server. */
    public boolean sendEmailFromLendex(String toEmail, String subject, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromAddress);
            email.setTo(toEmail);
            email.setSubject(subject);
            email.setText(message);
            mailSender.send(email);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send email from Lendex: " + e.getMessage());
            return false;
        }
    }
}