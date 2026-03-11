package com.finly.backend.controller;

import com.finly.backend.dto.request.CreateBudgetRequest;
import com.finly.backend.dto.response.ApiResponse;
import com.finly.backend.dto.response.BudgetResponse;
import com.finly.backend.security.UserDetailsImpl;
import com.finly.backend.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createOrUpdateBudget(
            @Valid @RequestBody CreateBudgetRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return new ResponseEntity<>(ApiResponse.success(budgetService.createOrUpdateBudget(request, principal)),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgets(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgets(year, month, principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        budgetService.deleteBudget(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
