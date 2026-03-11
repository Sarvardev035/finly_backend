package com.finly.backend.controller;

import com.finly.backend.dto.request.ExchangeRateUpsertRequest;
import com.finly.backend.dto.response.ApiResponse;
import com.finly.backend.dto.response.ExchangeRateResponse;
import com.finly.backend.service.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.listEffectiveRates()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> create(@Valid @RequestBody ExchangeRateUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.createAdminRate(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody ExchangeRateUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.updateAdminRate(id, request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> refresh() {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.refreshFromInternet()));
    }
}
