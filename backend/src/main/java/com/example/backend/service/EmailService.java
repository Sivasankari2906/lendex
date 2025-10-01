package com.example.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendEmail(String toEmail, String subject, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom("lendex.team@gmail.com");
            email.setTo(toEmail);
            email.setSubject(subject);
            email.setText(message);
            mailSender.send(email);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
    
    public void sendEmailFromLendex(String toEmail, String subject, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom("lendex.team@gmail.com");
            email.setTo(toEmail);
            email.setSubject(subject);
            email.setText(message);
            mailSender.send(email);
        } catch (Exception e) {
            System.err.println("Failed to send email from Lendex: " + e.getMessage());
        }
    }
}