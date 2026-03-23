package com.example.scoremaster.app.config;

import com.example.scoremaster.app.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage() != null ? ex.getMessage() : "Internal Server Error")
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
