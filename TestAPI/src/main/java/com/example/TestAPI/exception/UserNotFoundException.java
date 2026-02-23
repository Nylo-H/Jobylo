package com.example.TestAPI.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("Utilisateur non trouvé");
    }
}