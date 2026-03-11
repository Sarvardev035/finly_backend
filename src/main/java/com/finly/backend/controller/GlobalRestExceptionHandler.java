package com.finly.backend.controller;

import com.finly.backend.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("Unexpected character")) {
            message = "JSON parse error: Check for syntax errors like trailing commas or missing quotes.";
        }
        return new ResponseEntity<>(ErrorResponse.fail("VALIDATION_ERROR", message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(ErrorResponse.fail("VALIDATION_ERROR", errors.toString()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            com.finly.backend.exception.AccountNotFoundException.class,
            com.finly.backend.exception.ResourceNotFoundException.class,
            com.finly.backend.exception.CategoryNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundExceptions(Exception ex) {
        String code = "NOT_FOUND";
        if (ex instanceof com.finly.backend.exception.AccountNotFoundException)
            code = "ACCOUNT_NOT_FOUND";
        if (ex instanceof com.finly.backend.exception.CategoryNotFoundException)
            code = "CATEGORY_NOT_FOUND";

        return new ResponseEntity<>(ErrorResponse.fail(code, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            com.finly.backend.exception.EmailAlreadyExistsException.class,
            com.finly.backend.exception.DuplicateCategoryNameException.class,
            com.finly.backend.exception.CategoryInUseException.class,
            com.finly.backend.exception.AccountDeletionException.class,
            com.finly.backend.exception.AccountInUseException.class
    })
    public ResponseEntity<ErrorResponse> handleConflictExceptions(Exception ex) {
        String code = "BUSINESS_RULE_VIOLATION";
        if (ex instanceof com.finly.backend.exception.EmailAlreadyExistsException)
            code = "EMAIL_ALREADY_EXISTS";
        if (ex instanceof com.finly.backend.exception.AccountInUseException)
            code = "ACCOUNT_IN_USE";

        return new ResponseEntity<>(ErrorResponse.fail(code, ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({
            com.finly.backend.exception.InsufficientBalanceException.class,
            com.finly.backend.exception.InvalidCredentialsException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(Exception ex) {
        if (ex instanceof com.finly.backend.exception.InvalidCredentialsException) {
            return new ResponseEntity<>(ErrorResponse.fail("INVALID_CREDENTIALS", ex.getMessage()),
                    HttpStatus.UNAUTHORIZED);
        }
        if (ex instanceof com.finly.backend.exception.InsufficientBalanceException) {
            return new ResponseEntity<>(ErrorResponse.fail("INSUFFICIENT_BALANCE", ex.getMessage()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return new ResponseEntity<>(ErrorResponse.fail("BAD_REQUEST", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {
        return new ResponseEntity<>(
                ErrorResponse.fail("FORBIDDEN", "You do not have permission to access this resource"),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        ex.printStackTrace();
        return new ResponseEntity<>(ErrorResponse.fail("INTERNAL_SERVER_ERROR", ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
