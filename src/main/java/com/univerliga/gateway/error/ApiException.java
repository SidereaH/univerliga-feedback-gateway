package com.univerliga.gateway.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final List<ApiErrorDetail> details;

    public ApiException(String code, String message, HttpStatus status, List<ApiErrorDetail> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }

    public ApiException(String code, String message, HttpStatus status) {
        this(code, message, status, List.of());
    }
}
