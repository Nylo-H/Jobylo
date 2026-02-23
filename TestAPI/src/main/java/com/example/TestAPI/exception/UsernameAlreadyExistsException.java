package com.example.TestAPI.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("Le nom d'utilisateur '" + username + "' est déjà utilisé");
    }
}