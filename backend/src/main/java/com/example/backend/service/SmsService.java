package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    
    @Value("${twilio.account.sid}")
    private String accountSid;
    
    @Value("${twilio.auth.token}")
    private String authToken;
    
    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;
    
    public void sendSms(String phoneNumber, String message) {
        try {
            // Initialize Twilio (would need Twilio SDK)
            // Twilio.init(accountSid, authToken);
            // Message.creator(
            //     new PhoneNumber(phoneNumber),
            //     new PhoneNumber(fromPhoneNumber),
            //     message
            // ).create();
            
            // For now, log the SMS (replace with actual Twilio call)
            System.out.println("SMS from " + fromPhoneNumber + " to " + phoneNumber + ": " + message);
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
        }
    }
}