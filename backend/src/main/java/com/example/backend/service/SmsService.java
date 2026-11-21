package com.example.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Sends SMS through textbee (https://github.com/textbee/textbee), an open-source gateway that
 * sends texts from your own Android phone. Works with the hosted service or a self-hosted server.
 * Turned off until TEXTBEE_API_KEY is set; callers then fall back to email.
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final String apiKey;
    private final String deviceId;
    private final String defaultCountryCode;
    private final RestClient http;

    public SmsService(@Value("${lendex.sms.textbee.api-key:}") String apiKey,
                      @Value("${lendex.sms.textbee.base-url:https://api.textbee.dev}") String baseUrl,
                      @Value("${lendex.sms.textbee.device-id:}") String deviceId,
                      @Value("${lendex.sms.default-country-code:+91}") String defaultCountryCode) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.deviceId = deviceId == null ? "" : deviceId.trim();
        this.defaultCountryCode = defaultCountryCode;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        String base = baseUrl == null || baseUrl.isBlank() ? "https://api.textbee.dev" : baseUrl.trim();
        this.http = RestClient.builder().baseUrl(base).requestFactory(factory).build();
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    /** @return true if the gateway accepted the message. Never logs the message text, which may hold an OTP. */
    public boolean sendSms(String phoneNumber, String message) {
        String to = normalize(phoneNumber);
        if (to == null) {
            log.warn("SMS not sent: invalid phone number");
            return false;
        }
        if (!isEnabled()) {
            log.warn("SMS not sent to {}: set TEXTBEE_API_KEY to enable SMS", mask(to));
            return false;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("recipients", List.of(to));
        body.put("message", message);
        if (!deviceId.isEmpty()) {
            body.put("deviceId", deviceId);
        }
        try {
            http.post().uri("/api/v1/gateway/send-sms")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("SMS to {} failed: {}", mask(to), e.getMessage());
            return false;
        }
    }

    /** Accepts "+919876543210", "919876543210", "98765 43210" or "9876543210" and returns E.164, or null. */
    public String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) {
            return digits.matches("\\+[1-9][0-9]{9,14}") ? digits : null;
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1); // trunk prefix, e.g. 098765 43210
        }
        if (digits.matches("[6-9][0-9]{9}") || (digits.length() == 10 && !"+91".equals(defaultCountryCode))) {
            return defaultCountryCode + digits;
        }
        if (digits.matches("[1-9][0-9]{10,14}")) {
            return "+" + digits;
        }
        return null;
    }

    private static String mask(String phone) {
        return phone.length() <= 4 ? "****" : "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
