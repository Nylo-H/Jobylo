package com.example.TestAPI.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("Mot de passe invalide");
    }
}