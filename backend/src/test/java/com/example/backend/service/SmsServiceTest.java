package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/** Checks the textbee request against a local stub server on 127.0.0.1. */
class SmsServiceTest {

    HttpServer server;
    final AtomicReference<String> path = new AtomicReference<>();
    final AtomicReference<String> apiKey = new AtomicReference<>();
    final AtomicReference<String> body = new AtomicReference<>();
    int status = 201;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            path.set(ex.getRequestURI().getPath());
            apiKey.set(ex.getRequestHeaders().getFirst("x-api-key"));
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = "{\"data\":{\"success\":true}}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, out.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(out); }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private SmsService service(String key, String device) {
        return new SmsService(key, "http://127.0.0.1:" + server.getAddress().getPort(), device, "+91");
    }

    @Test
    void sendsToTextbeeWithApiKeyRecipientAndDevice() {
        assertTrue(service("test-key", "dev-1").sendSms("98765 43210", "Your code is 123456"));
        assertEquals("/api/v1/gateway/send-sms", path.get());
        assertEquals("test-key", apiKey.get());
        assertTrue(body.get().contains("\"+919876543210\""), body.get());
        assertTrue(body.get().contains("Your code is 123456"));
        assertTrue(body.get().contains("\"deviceId\":\"dev-1\""));
    }

    @Test
    void disabledWithoutAnApiKeyAndSendsNothing() {
        SmsService sms = service("", "");
        assertFalse(sms.isEnabled());
        assertFalse(sms.sendSms("9876543210", "x"));
        assertNull(path.get());
    }

    @Test
    void gatewayErrorReturnsFalse() {
        status = 401;
        assertFalse(service("bad-key", "").sendSms("+919876543210", "x"));
    }

    @Test
    void normalizesIndianNumbersAndRejectsShortOnes() {
        SmsService sms = service("k", "");
        assertEquals("+919876543210", sms.normalize("9876543210"));
        assertEquals("+919876543210", sms.normalize("+91 98765-43210"));
        assertEquals("+919876543210", sms.normalize("919876543210"));
        assertNull(sms.normalize("12345"));
        assertNull(sms.normalize(null));
    }
}
