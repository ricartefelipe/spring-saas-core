package com.union.solutions.saascore.application.user;

public class EmailAlreadyExistsException extends RuntimeException {

    private final String email;

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
        this.email = email;
    }

    public String getEmail() { return email; }
}
