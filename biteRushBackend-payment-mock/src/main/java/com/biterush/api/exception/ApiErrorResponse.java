package com.biterush.api.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApiErrorResponse {
    private int code;
    private String message;
    private String error;
    private LocalDateTime timestamp;
    private String path;
    private Object details;

    public ApiErrorResponse(int code, String message, String error) {
        this.code = code;
        this.message = message;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }
}
