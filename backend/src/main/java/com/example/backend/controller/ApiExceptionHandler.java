package com.example.backend.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns service errors into proper HTTP status codes with a readable "message" for the frontend. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Map<String, Object[]> KNOWN = Map.of(
            "username-exists", new Object[] { HttpStatus.CONFLICT, "That username is already taken." },
            "email-exists", new Object[] { HttpStatus.CONFLICT, "An account with that email already exists." },
            "invalid-credentials", new Object[] { HttpStatus.UNAUTHORIZED, "Invalid username or password." },
            "invalid-or-expired-token", new Object[] { HttpStatus.BAD_REQUEST, "This reset link is invalid or has expired." },
            "user-not-found", new Object[] { HttpStatus.NOT_FOUND, "User not found." });

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
        return body(HttpStatus.FORBIDDEN, "You do not have access to this item.");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return body(HttpStatus.NOT_FOUND, "Not found.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid request.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> runtime(RuntimeException e) {
        Object[] known = KNOWN.get(e.getMessage());
        if (known != null) {
            return body((HttpStatus) known[0], (String) known[1]);
        }
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
