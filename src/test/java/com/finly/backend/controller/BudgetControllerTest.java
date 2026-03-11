package com.finly.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finly.backend.domain.model.BudgetType;
import com.finly.backend.dto.request.CreateBudgetRequest;
import com.finly.backend.dto.response.BudgetResponse;
import com.finly.backend.security.JwtAuthenticationEntryPoint;
import com.finly.backend.security.JwtAuthenticationFilter;
import com.finly.backend.security.UserDetailsServiceImpl;
import com.finly.backend.service.BudgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for /api/budgets endpoints.
 */
@WebMvcTest(BudgetController.class)
@DisplayName("BudgetController - /api/budgets")
class BudgetControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BudgetService budgetService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final UUID BUDGET_ID   = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private BudgetResponse sampleBudget() {
        return BudgetResponse.builder()
                .id(BUDGET_ID)
                .type(BudgetType.MONTHLY)
                .categoryId(CATEGORY_ID)
                .monthlyLimit(BigDecimal.valueOf(800))
                .spentAmount(BigDecimal.valueOf(200))
                .remainingAmount(BigDecimal.valueOf(600))
                .percentageUsed(BigDecimal.valueOf(25))
                .year(2026)
                .month(3)
                .build();
    }

    // ── POST /api/budgets ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /budgets → 201 with valid body")
    void createBudget_validRequest_returns201() throws Exception {
        when(budgetService.createOrUpdateBudget(any(), any())).thenReturn(sampleBudget());

        CreateBudgetRequest body = CreateBudgetRequest.builder()
                .type(BudgetType.MONTHLY)
                .categoryId(CATEGORY_ID)
                .monthlyLimit(BigDecimal.valueOf(800))
                .year(2026)
                .month(3)
                .build();

        mockMvc.perform(post("/api/budgets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyLimit").value(800))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(3));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /budgets → 400 when type is missing")
    void createBudget_missingType_returns400() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyLimit\":800,\"year\":2026,\"month\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /budgets → 400 when monthlyLimit is missing")
    void createBudget_missingLimit_returns400() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"MONTHLY\",\"year\":2026,\"month\":3}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/budgets?year=&month= ───────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /budgets?year=2026&month=3 → 200 returns list")
    void getBudgets_withYearMonth_returns200() throws Exception {
        when(budgetService.getBudgets(anyInt(), anyInt(), any()))
                .thenReturn(List.of(sampleBudget()));

        mockMvc.perform(get("/api/budgets").param("year", "2026").param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].year").value(2026))
                .andExpect(jsonPath("$.data[0].month").value(3));
    }

    @Test
    @DisplayName("GET /budgets → 401 when not authenticated")
    void getBudgets_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/budgets").param("year", "2026").param("month", "3"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/budgets/{id} ────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("DELETE /budgets/{id} → 200 on success")
    void deleteBudget_exists_returns200() throws Exception {
        doNothing().when(budgetService).deleteBudget(any(), any());

        mockMvc.perform(delete("/api/budgets/{id}", BUDGET_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
