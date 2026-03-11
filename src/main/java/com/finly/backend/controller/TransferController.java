package com.finly.backend.controller;

import com.finly.backend.dto.request.CreateTransferRequest;
import com.finly.backend.dto.response.ApiResponse;
import com.finly.backend.dto.response.TransferResponse;
import com.finly.backend.security.UserDetailsImpl;
import com.finly.backend.service.TransferService;
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
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
            @Valid @RequestBody CreateTransferRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return new ResponseEntity<>(ApiResponse.success(transferService.createTransfer(request, principal)),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getTransfers(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity
                .ok(ApiResponse.success(transferService.getTransfers(accountId, startDate, endDate, principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransferResponse>> getTransferById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(transferService.getTransferById(id, principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransfer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        transferService.deleteTransfer(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
