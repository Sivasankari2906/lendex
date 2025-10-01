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

    public AuthService(UserRepository userRepo, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailService emailService, SmsService smsService) {
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
    
    public void sendOtp(String phone) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStorage.put(phone, otp);
        otpExpiry.put(phone, LocalDateTime.now().plusMinutes(5));
        
        String message = "Your Lendex verification code is: " + otp + ". Valid for 5 minutes.";
        smsService.sendSms(phone, message);
    }
    
    public boolean verifyOtp(String phone, String otp) {
        String storedOtp = otpStorage.get(phone);
        LocalDateTime expiry = otpExpiry.get(phone);
        
        if (storedOtp == null || expiry == null || LocalDateTime.now().isAfter(expiry)) {
            return false;
        }
        
        boolean isValid = storedOtp.equals(otp);
        if (isValid) {
            otpStorage.remove(phone);
            otpExpiry.remove(phone);
        }
        return isValid;
    }
    
    public void sendPasswordResetEmail(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("email-not-found"));
        String token = UUID.randomUUID().toString();
        resetTokens.put(token, user.getUsername());
        resetTokenExpiry.put(token, LocalDateTime.now().plusHours(1));
        
        String resetLink = "http://localhost:3000/reset-password?token=" + token;
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
