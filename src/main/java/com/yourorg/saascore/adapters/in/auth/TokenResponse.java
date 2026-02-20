package com.yourorg.saascore.adapters.in.auth;

public class TokenResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;

    public TokenResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
