package com.example.TestAPI.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    NOT_FOUND(404),
    BAD_REQUEST(400),
    FORBIDDEN(403),
    UNAUTHORIZED(401),
    CONFLICT(409),
    TOO_MANY_REQUESTS(429),
    INTERNAL_ERROR(500);

    private final int statusCode;

    ErrorCode(int statusCode) {
        this.statusCode = statusCode;
    }

}
