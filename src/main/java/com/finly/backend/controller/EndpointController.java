package com.finly.backend.controller;

import com.finly.backend.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/connect")
@Tag(name = "System", description = "System endpoint tester for Admin API")
@Slf4j
public class EndpointController {

    @PostMapping("/{endpoint}/{method}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin API Endpoint Tester")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testEndpoint(
            @PathVariable String endpoint,
            @PathVariable String method,
            @RequestBody(required = false) Map<String, Object> body) {

        log.info("Admin system test: Endpoint={}, Method={}, Body={}", endpoint, method, body);

        // Mock response for the system tester
        Map<String, Object> responseData = Map.of(
                "status", "success",
                "endpoint", endpoint,
                "method", method,
                "processedAt", java.time.LocalDateTime.now().toString(),
                "message", "Connection established and request processed.");

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }
}
