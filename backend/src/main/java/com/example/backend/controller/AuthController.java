package com.example.backend.controller;

import com.example.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String fullName = body.getOrDefault("fullName", "");
        String email = body.getOrDefault("email", "");
        String phone = body.getOrDefault("phone", "");
        String token = auth.register(username, password, fullName, email, phone);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String,String> body) {
        String phone = body.get("phone");
        auth.sendOtp(phone);
        return ResponseEntity.ok(Map.of("message", "OTP sent to your phone"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String,String> body) {
        String phone = body.get("phone");
        String otp = body.get("otp");
        boolean isValid = auth.verifyOtp(phone, otp);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String,String> body) {
        String email = body.get("email");
        auth.sendPasswordResetEmail(email);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String,String> body) {
        String token = body.get("token");
        String newPassword = body.get("password");
        auth.resetPassword(token, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String token = auth.login(username, password);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
