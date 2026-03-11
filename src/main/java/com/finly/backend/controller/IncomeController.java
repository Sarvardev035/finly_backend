package com.finly.backend.controller;

import com.finly.backend.dto.request.CreateIncomeRequest;
import com.finly.backend.dto.request.UpdateIncomeRequest;
import com.finly.backend.dto.response.ApiResponse;
import com.finly.backend.dto.response.IncomeResponse;
import com.finly.backend.security.UserDetailsImpl;
import com.finly.backend.service.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    @PostMapping
    public ResponseEntity<ApiResponse<IncomeResponse>> createIncome(
            @Valid @RequestBody CreateIncomeRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return new ResponseEntity<>(ApiResponse.success(incomeService.createIncome(request, principal)),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IncomeResponse>>> getIncomes(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity
                .ok(ApiResponse.success(incomeService.getIncomes(accountId, null, startDate, endDate, principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncomeResponse>> getIncomeById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(incomeService.getIncomeById(id, principal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IncomeResponse>> updateIncome(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIncomeRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(incomeService.updateIncome(id, request, principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIncome(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        incomeService.deleteIncome(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
