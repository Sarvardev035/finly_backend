package com.finly.backend.controller;

import com.finly.backend.dto.response.AnalyticsSummaryResponse;
import com.finly.backend.dto.response.DashboardSummaryResponse;
import com.finly.backend.security.JwtAuthenticationEntryPoint;
import com.finly.backend.security.JwtAuthenticationFilter;
import com.finly.backend.security.UserDetailsServiceImpl;
import com.finly.backend.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for /api/analytics endpoints (read-only GET endpoints).
 */
@WebMvcTest(AnalyticsController.class)
@DisplayName("AnalyticsController - /api/analytics")
class AnalyticsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AnalyticsService analyticsService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // ── GET /api/analytics/summary ──────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /analytics/summary → 200 returns summary")
    void getSummary_authenticated_returns200() throws Exception {
        AnalyticsSummaryResponse summary = AnalyticsSummaryResponse.builder()
                .totalBalance(BigDecimal.valueOf(3500))
                .totalIncome(BigDecimal.valueOf(2000))
                .totalExpense(BigDecimal.valueOf(1200))
                .savings(BigDecimal.valueOf(800))
                .build();
        when(analyticsService.getSummary(any())).thenReturn(summary);

        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBalance").value(3500))
                .andExpect(jsonPath("$.data.totalIncome").value(2000))
                .andExpect(jsonPath("$.data.totalExpense").value(1200))
                .andExpect(jsonPath("$.data.savings").value(800));
    }

    @Test
    @DisplayName("GET /analytics/summary → 401 when not authenticated")
    void getSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/analytics/expenses-by-category ─────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /analytics/expenses-by-category → 200")
    void getExpensesByCategory_authenticated_returns200() throws Exception {
        when(analyticsService.getExpensesByCategory(any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/expenses-by-category")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── GET /api/analytics/monthly-expenses ─────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /analytics/monthly-expenses → 200")
    void getMonthlyExpenses_authenticated_returns200() throws Exception {
        when(analyticsService.getMonthlyExpenses(any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/monthly-expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── GET /api/analytics/income-vs-expense ────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /analytics/income-vs-expense → 200")
    void getIncomeVsExpense_authenticated_returns200() throws Exception {
        when(analyticsService.getIncomeVsExpense(any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/income-vs-expense"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── GET /api/analytics/account-balances ─────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /analytics/account-balances → 200")
    void getAccountBalances_authenticated_returns200() throws Exception {
        when(analyticsService.getAccountBalances(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/account-balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── GET /api/analytics/dashboard ────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /analytics/dashboard → 200")
    void getDashboardSummary_authenticated_returns200() throws Exception {
        DashboardSummaryResponse dashboard = DashboardSummaryResponse.builder()
                .totalIncome(BigDecimal.valueOf(3200))
                .totalExpense(BigDecimal.valueOf(1500))
                .balance(BigDecimal.valueOf(1700))
                .savingsRate(BigDecimal.valueOf(53.1))
                .build();
        when(analyticsService.getDashboardSummary(any(), any(), any()))
                .thenReturn(dashboard);

        mockMvc.perform(get("/api/analytics/dashboard")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalIncome").value(3200));
    }

    // ── GET /api/analytics/categories ───────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /analytics/categories?type=EXPENSE → 200")
    void getCategoryStats_authenticated_returns200() throws Exception {
        when(analyticsService.getCategoryStatistics(any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/categories").param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── GET /api/analytics/timeseries ───────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /analytics/timeseries?period=DAILY → 200")
    void getTimeSeries_authenticated_returns200() throws Exception {
        when(analyticsService.getTimeSeries(any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/timeseries").param("period", "DAILY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
