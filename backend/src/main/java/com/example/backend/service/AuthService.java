package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final SmsService smsService;
    
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpExpiry = new ConcurrentHashMap<>();
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> resetTokenExpiry = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Value("${lendex.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public AuthService(UserRepository userRepo, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailService emailService,
                       SmsService smsService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public String register(String username, String password, String fullName, String email, String phone) {
        if (userRepo.existsByUsername(username)) throw new RuntimeException("username-exists");
        if (!email.isEmpty() && userRepo.existsByEmail(email)) throw new RuntimeException("email-exists");
        
        User u = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .build();
        userRepo.save(u);
        return jwtUtil.generateToken(username);
    }
    
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private static final int OTP_RESEND_SECONDS = 30;
    private final Map<String, LocalDateTime> otpLastSent = new ConcurrentHashMap<>();

    /**
     * Sends a 6-digit OTP. If SMS is configured and a phone number is given, it goes to the phone;
     * otherwise it goes to the email address. Returns "sms" or "email".
     */
    public String sendOtp(String email, String phone) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        LocalDateTime last = otpLastSent.get(email);
        if (last != null && last.plusSeconds(OTP_RESEND_SECONDS).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Please wait " + OTP_RESEND_SECONDS + " seconds before requesting another code.");
        }
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        otpStorage.put(email, otp);
        otpExpiry.put(email, LocalDateTime.now().plusMinutes(5));
        otpLastSent.put(email, LocalDateTime.now());

        if (phone != null && !phone.isBlank() && smsService.isEnabled()
                && smsService.sendSms(phone, "Your Lendex verification code is " + otp + ". It expires in 5 minutes.")) {
            return "sms";
        }
        String subject = "Phone Verification - Lendex";
        String body = "Hello,\n\nYour phone verification OTP is: " + otp + "\n\nThis OTP will expire in 5 minutes.\n\nBest regards,\nLendex Team";
        emailService.sendEmailFromLendex(email, subject, body);
        return "email";
    }

    public boolean verifyOtp(String email, String otp) {
        String storedOtp = otpStorage.get(email);
        LocalDateTime expiry = otpExpiry.get(email);
        
        if (storedOtp == null || expiry == null || LocalDateTime.now().isAfter(expiry)) {
            return false;
        }
        
        boolean isValid = storedOtp.equals(otp);
        if (isValid) {
            otpStorage.remove(email);
            otpExpiry.remove(email);
        }
        return isValid;
    }
    
    public void sendPasswordResetEmail(String email) {
        // Do nothing for unknown emails, so the endpoint does not reveal which emails have accounts.
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        String token = UUID.randomUUID().toString();
        resetTokens.put(token, user.getUsername());
        resetTokenExpiry.put(token, LocalDateTime.now().plusHours(1));
        
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Password Reset - Lendex";
        String body = "Hello,\n\nClick the link below to reset your password:\n\n" + resetLink + "\n\nThis link will expire in 1 hour.\n\nIf you didn't request this, please ignore this email.\n\nBest regards,\nLendex Team";
        emailService.sendEmailFromLendex(email, subject, body);
    }
    
    public void resetPassword(String token, String newPassword) {
        String username = resetTokens.get(token);
        LocalDateTime expiry = resetTokenExpiry.get(token);
        
        if (username == null || expiry == null || LocalDateTime.now().isAfter(expiry)) {
            throw new RuntimeException("invalid-or-expired-token");
        }
        
        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("user-not-found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        
        resetTokens.remove(token);
        resetTokenExpiry.remove(token);
    }

    public String login(String username, String password) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("invalid-credentials"));
        if (!passwordEncoder.matches(password, u.getPasswordHash())) throw new RuntimeException("invalid-credentials");
        return jwtUtil.generateToken(username);
    }
}
