package com.example.TestAPI.exception;

public class UserAlreadyVerifiedException extends RuntimeException {
    public UserAlreadyVerifiedException() {
        super("Utilisateur déjà vérifié");
    }
}
