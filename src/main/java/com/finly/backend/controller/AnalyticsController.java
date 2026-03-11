package com.finly.backend.controller;

import com.finly.backend.dto.response.*;
import com.finly.backend.domain.model.CategoryType;
import com.finly.backend.service.AnalyticsService;
import com.finly.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getSummary(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getSummary(principal)));
    }

    @GetMapping("/expenses-by-category")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryAmountResponse>>> getExpensesByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getExpensesByCategory(from, to, principal)));
    }

    @GetMapping("/monthly-expenses")
    public ResponseEntity<ApiResponse<List<MonthlyAmountResponse>>> getMonthlyExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getMonthlyExpenses(from, to, principal)));
    }

    @GetMapping("/income-vs-expense")
    public ResponseEntity<ApiResponse<List<IncomeVsExpenseResponse>>> getIncomeVsExpense(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getIncomeVsExpense(from, to, principal)));
    }

    @GetMapping("/account-balances")
    public ResponseEntity<ApiResponse<List<AccountBalanceResponse>>> getAccountBalances(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAccountBalances(principal)));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : resolvedEnd.minusMonths(1);
        return ResponseEntity
                .ok(ApiResponse.success(analyticsService.getDashboardSummary(resolvedStart, resolvedEnd, principal)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryStatsResponse>>> getCategoryStatistics(
            @RequestParam(required = false, defaultValue = "EXPENSE") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : resolvedEnd.minusMonths(1);
        CategoryType parsedType = parseCategoryType(type);
        return ResponseEntity
                .ok(ApiResponse.success(
                        analyticsService.getCategoryStatistics(parsedType, resolvedStart, resolvedEnd, principal)));
    }

    @GetMapping("/timeseries")
    public ResponseEntity<ApiResponse<List<TimeSeriesResponse>>> getTimeSeries(
            @RequestParam String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : resolvedEnd.minusMonths(1);
        String parsedPeriod = parsePeriod(period);
        return ResponseEntity
                .ok(ApiResponse.success(analyticsService.getTimeSeries(parsedPeriod, resolvedStart, resolvedEnd, principal)));
    }

    private CategoryType parseCategoryType(String raw) {
        if (raw == null || raw.isBlank()) {
            return CategoryType.EXPENSE;
        }
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "EXPENSE", "EXPENSES" -> CategoryType.EXPENSE;
            case "INCOME", "INCOMES" -> CategoryType.INCOME;
            default -> throw new IllegalArgumentException("Invalid category type: " + raw);
        };
    }

    private String parsePeriod(String raw) {
        if (raw == null || raw.isBlank()) {
            return "DAILY";
        }
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "DAY", "D", "DAILY" -> "DAILY";
            case "WEEK", "W", "WEEKLY" -> "WEEKLY";
            case "MONTH", "M", "MONTHLY" -> "MONTHLY";
            case "YEAR", "Y", "YEARLY" -> "YEARLY";
            default -> throw new IllegalArgumentException("Invalid period: " + raw);
        };
    }
}
