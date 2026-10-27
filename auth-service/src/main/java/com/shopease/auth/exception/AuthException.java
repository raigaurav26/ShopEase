package com.shopease.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }

    public static AuthException accountLocked() {
        return new AuthException(HttpStatus.LOCKED,
                "Account temporarily locked after too many failed sign-in attempts. Try again later.");
    }

    public static AuthException emailTaken() {
        return new AuthException(HttpStatus.CONFLICT, "Email already exists");
    }
}
