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
        if (phone != null && !phone.isBlank()) {
            String normalized = smsService.normalize(phone);
            String verifiedKey = (email == null ? "" : email.trim().toLowerCase()) + "|" + normalized;
            java.time.Instant until = normalized == null ? null : verifiedPhones.remove(verifiedKey);
            if (until == null || java.time.Instant.now().isAfter(until)) {
                throw new RuntimeException("phone-not-verified");
            }
        }
        
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
    private static final java.time.Duration OTP_TTL = java.time.Duration.ofMinutes(5);
    private static final java.time.Duration RESEND = java.time.Duration.ofSeconds(30);
    private static final java.time.Duration VERIFIED_TTL = java.time.Duration.ofMinutes(30);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_SENDS_PER_KEY_PER_HOUR = 5;

    @org.springframework.beans.factory.annotation.Value("${lendex.otp.max-per-hour:60}")
    private int maxOtpsPerHour = 60;

    private record OtpEntry(String hash, String phone, java.time.Instant expiresAt, int attempts) { }

    private final Map<String, OtpEntry> otps = new ConcurrentHashMap<>();
    private final Map<String, java.util.Deque<java.time.Instant>> sends = new ConcurrentHashMap<>();
    private final java.util.Deque<java.time.Instant> allSends = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private final Map<String, java.time.Instant> verifiedPhones = new ConcurrentHashMap<>();

    /**
     * Sends a 6-digit OTP to the phone by SMS if SMS is configured, otherwise to the email address.
     * Limits: one code per email or phone every 30 seconds, 5 per hour each, and a global hourly cap.
     * Returns "sms" or "email".
     */
    public synchronized String sendOtp(String email, String phone) {
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        String key = email.trim().toLowerCase();
        String normalizedPhone = phone == null || phone.isBlank() ? null : smsService.normalize(phone);
        if (phone != null && !phone.isBlank() && normalizedPhone == null) {
            throw new IllegalArgumentException("Enter a valid mobile number.");
        }
        java.time.Instant now = java.time.Instant.now();
        pruneOld(now);
        if (!allowed("email:" + key, now) || (normalizedPhone != null && !allowed("phone:" + normalizedPhone, now))
                || allSends.size() >= maxOtpsPerHour) {
            throw new RuntimeException("otp-rate-limited");
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        otps.put(key, new OtpEntry(hashOtp(key, normalizedPhone, otp), normalizedPhone, now.plus(OTP_TTL), 0));
        record(key, normalizedPhone, now);

        if (normalizedPhone != null && smsService.isEnabled()
                && smsService.sendSms(normalizedPhone, "Your Lendex verification code is " + otp + ". It expires in 5 minutes.")) {
            return "sms";
        }
        String body = "Hello,\n\nYour Lendex verification code is: " + otp + "\n\nIt expires in 5 minutes.\n\nBest regards,\nLendex Team";
        if (emailService.sendEmailFromLendex(email, "Your Lendex verification code", body)) {
            return "email";
        }
        otps.remove(key);
        throw new RuntimeException("otp-delivery-failed");
    }

    /** True once for the right code. A code allows 5 attempts and expires after 5 minutes. */
    public synchronized boolean verifyOtp(String email, String otp) {
        if (email == null || otp == null) {
            return false;
        }
        String key = email.trim().toLowerCase();
        OtpEntry entry = otps.get(key);
        java.time.Instant now = java.time.Instant.now();
        if (entry == null || now.isAfter(entry.expiresAt()) || entry.attempts() >= MAX_ATTEMPTS) {
            otps.remove(key);
            return false;
        }
        boolean ok = java.security.MessageDigest.isEqual(
                entry.hash().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                hashOtp(key, entry.phone(), otp.trim()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!ok) {
            otps.put(key, new OtpEntry(entry.hash(), entry.phone(), entry.expiresAt(), entry.attempts() + 1));
            return false;
        }
        otps.remove(key);
        if (entry.phone() != null) {
            verifiedPhones.put(key + "|" + entry.phone(), now.plus(VERIFIED_TTL));
        }
        return true;
    }

    private boolean allowed(String bucket, java.time.Instant now) {
        java.util.Deque<java.time.Instant> list = sends.get(bucket);
        if (list == null || list.isEmpty()) {
            return true;
        }
        return list.peekLast().plus(RESEND).isBefore(now) && list.size() < MAX_SENDS_PER_KEY_PER_HOUR;
    }

    private void record(String key, String phone, java.time.Instant now) {
        sends.computeIfAbsent("email:" + key, k -> new java.util.ArrayDeque<>()).addLast(now);
        if (phone != null) {
            sends.computeIfAbsent("phone:" + phone, k -> new java.util.ArrayDeque<>()).addLast(now);
        }
        allSends.addLast(now);
    }

    /** Drops expired codes, old send records and old verifications so memory does not grow. */
    private void pruneOld(java.time.Instant now) {
        java.time.Instant hourAgo = now.minus(java.time.Duration.ofHours(1));
        while (!allSends.isEmpty() && allSends.peekFirst().isBefore(hourAgo)) {
            allSends.pollFirst();
        }
        sends.values().forEach(d -> { while (!d.isEmpty() && d.peekFirst().isBefore(hourAgo)) d.pollFirst(); });
        sends.values().removeIf(java.util.Deque::isEmpty);
        otps.values().removeIf(e -> now.isAfter(e.expiresAt()));
        verifiedPhones.values().removeIf(now::isAfter);
    }

    private static String hashOtp(String email, String phone, String otp) {
        try {
            java.security.MessageDigest d = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(d.digest((email + "|" + phone + "|" + otp).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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
