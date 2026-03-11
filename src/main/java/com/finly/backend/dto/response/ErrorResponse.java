package com.finly.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private boolean success;
    private String error;
    private String message;

    public static ErrorResponse fail(String errorCode, String message) {
        return ErrorResponse.builder()
                .success(false)
                .error(errorCode)
                .message(message)
                .build();
    }
}
