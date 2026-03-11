package com.finly.backend.controller;

import com.finly.backend.domain.model.DebtStatus;
import com.finly.backend.domain.model.DebtType;
import com.finly.backend.dto.request.CreateDebtRequest;
import com.finly.backend.dto.request.RepaymentRequest;
import com.finly.backend.dto.response.ApiResponse;
import com.finly.backend.dto.response.DebtResponse;
import com.finly.backend.security.UserDetailsImpl;
import com.finly.backend.service.DebtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;

    @PostMapping
    public ResponseEntity<ApiResponse<DebtResponse>> createDebt(
            @Valid @RequestBody CreateDebtRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return new ResponseEntity<>(ApiResponse.success(debtService.createDebt(request, principal)),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DebtResponse>>> getDebts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        DebtType parsedType = (type == null || type.isBlank()) ? null : DebtType.from(type);
        DebtStatus parsedStatus = (status == null || status.isBlank()) ? null : DebtStatus.from(status);
        return ResponseEntity.ok(ApiResponse.success(debtService.getDebts(parsedType, parsedStatus, principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DebtResponse>> getDebtById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(debtService.getDebtById(id, principal)));
    }

    @PostMapping("/{id}/repay")
    public ResponseEntity<ApiResponse<DebtResponse>> repayDebt(
            @PathVariable UUID id,
            @Valid @RequestBody RepaymentRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(debtService.repayDebt(id, request, principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDebt(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        debtService.deleteDebt(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
