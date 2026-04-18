package com.capics.exception;

import com.capics.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.capics.controller")
public class GlobalApiExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse> handleThrowable(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = ex.getClass().getSimpleName();
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error: " + message));
    }
}
