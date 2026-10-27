package com.shopease.auth.dto;

public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private String message;

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, String message) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.message = message;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    /** Lifetime of the access token in seconds. */
    public long getExpiresIn() {
        return expiresIn;
    }

    public String getMessage() {
        return message;
    }
}
