package com.finly.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.dto.request.CreateExpenseRequest;
import com.finly.backend.dto.response.ExpenseResponse;
import com.finly.backend.exception.ResourceNotFoundException;
import com.finly.backend.security.JwtAuthenticationEntryPoint;
import com.finly.backend.security.JwtAuthenticationFilter;
import com.finly.backend.security.UserDetailsServiceImpl;
import com.finly.backend.service.ExpenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for /api/expenses CRUD endpoints.
 */
@WebMvcTest(ExpenseController.class)
@DisplayName("ExpenseController - /api/expenses")
class ExpenseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ExpenseService expenseService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final UUID EXPENSE_ID  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACCOUNT_ID  = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private ExpenseResponse sampleExpense() {
        return ExpenseResponse.builder()
                .id(EXPENSE_ID)
                .amount(BigDecimal.valueOf(55.75))
                .currency(Currency.USD)
                .description("Lunch")
                .expenseDate(LocalDate.of(2026, 3, 5))
                .categoryId(CATEGORY_ID)
                .accountId(ACCOUNT_ID)
                .build();
    }

    // ── GET /api/expenses ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /expenses → 200 returns list")
    void getExpenses_authenticated_returns200() throws Exception {
        when(expenseService.getExpenses(any(), any(), any(), any(), any()))
                .thenReturn(List.of(sampleExpense()));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].description").value("Lunch"))
                .andExpect(jsonPath("$.data[0].amount").value(55.75));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /expenses?accountId=&startDate= → 200 with query params")
    void getExpenses_withFilters_returns200() throws Exception {
        when(expenseService.getExpenses(any(), any(), any(), any(), any()))
                .thenReturn(List.of(sampleExpense()));

        mockMvc.perform(get("/api/expenses")
                        .param("accountId", ACCOUNT_ID.toString())
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /expenses → 401 when not authenticated")
    void getExpenses_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/expenses/{id} ──────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /expenses/{id} → 200 for existing expense")
    void getExpenseById_exists_returns200() throws Exception {
        when(expenseService.getExpenseById(eq(EXPENSE_ID), any()))
                .thenReturn(sampleExpense());

        mockMvc.perform(get("/api/expenses/{id}", EXPENSE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(EXPENSE_ID.toString()));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /expenses/{id} → 404 when expense does not exist")
    void getExpenseById_notFound_returns404() throws Exception {
        when(expenseService.getExpenseById(eq(EXPENSE_ID), any()))
                .thenThrow(new ResourceNotFoundException("Expense not found"));

        mockMvc.perform(get("/api/expenses/{id}", EXPENSE_ID))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/expenses ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /expenses → 201 with valid body")
    void createExpense_validRequest_returns201() throws Exception {
        when(expenseService.createExpense(any(), any())).thenReturn(sampleExpense());

        CreateExpenseRequest body = CreateExpenseRequest.builder()
                .amount(BigDecimal.valueOf(55.75))
                .currency(Currency.USD)
                .description("Lunch")
                .expenseDate(LocalDate.of(2026, 3, 5))
                .categoryId(CATEGORY_ID)
                .accountId(ACCOUNT_ID)
                .build();

        mockMvc.perform(post("/api/expenses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Lunch"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /expenses → 400 when amount is missing")
    void createExpense_missingAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Lunch\",\"expenseDate\":\"2026-03-05\","
                                + "\"categoryId\":\"" + CATEGORY_ID + "\","
                                + "\"accountId\":\"" + ACCOUNT_ID + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /expenses → 400 when description is blank")
    void createExpense_blankDescription_returns400() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":55.75,\"description\":\"\","
                                + "\"expenseDate\":\"2026-03-05\","
                                + "\"categoryId\":\"" + CATEGORY_ID + "\","
                                + "\"accountId\":\"" + ACCOUNT_ID + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/expenses/{id} ───────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("DELETE /expenses/{id} → 200 on success")
    void deleteExpense_exists_returns200() throws Exception {
        mockMvc.perform(delete("/api/expenses/{id}", EXPENSE_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /expenses/{id} → 404 for missing expense")
    void deleteExpense_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Expense not found"))
                .when(expenseService).deleteExpense(eq(EXPENSE_ID), any());

        mockMvc.perform(delete("/api/expenses/{id}", EXPENSE_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
