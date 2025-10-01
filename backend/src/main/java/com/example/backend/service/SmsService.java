package com.example.backend.service;

import org.springframework.stereotype.Service;

@Service
public class SmsService {
    
    public void sendSms(String phoneNumber, String message) {
        // Integrate with SMS provider (Twilio, AWS SNS, etc.)
        // For now, just log the SMS
        System.out.println("SMS to " + phoneNumber + ": " + message);
        
        // Example Twilio integration:
        // Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        // Message.creator(new PhoneNumber(phoneNumber), new PhoneNumber(FROM_NUMBER), message).create();
    }
}