package com.finly.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.DebtStatus;
import com.finly.backend.domain.model.DebtType;
import com.finly.backend.dto.request.CreateDebtRequest;
import com.finly.backend.dto.request.RepaymentRequest;
import com.finly.backend.dto.response.DebtResponse;
import com.finly.backend.security.JwtAuthenticationEntryPoint;
import com.finly.backend.security.JwtAuthenticationFilter;
import com.finly.backend.security.UserDetailsServiceImpl;
import com.finly.backend.service.DebtService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for /api/debts endpoints.
 */
@WebMvcTest(DebtController.class)
@DisplayName("DebtController - /api/debts")
class DebtControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean DebtService debtService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final UUID DEBT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    private DebtResponse sampleDebt() {
        return DebtResponse.builder()
                .id(DEBT_ID)
                .personName("Ali")
                .type(DebtType.RECEIVABLE)
                .amount(BigDecimal.valueOf(500))
                .remainingAmount(BigDecimal.valueOf(500))
                .currency(Currency.UZS)
                .status(DebtStatus.OPEN)
                .dueDate(LocalDate.of(2026, 4, 1))
                .build();
    }

    // ── POST /api/debts ─────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /debts → 201 with valid body")
    void createDebt_validRequest_returns201() throws Exception {
        when(debtService.createDebt(any(), any())).thenReturn(sampleDebt());

        CreateDebtRequest body = CreateDebtRequest.builder()
                .personName("Ali")
                .type(DebtType.RECEIVABLE)
                .amount(BigDecimal.valueOf(500))
                .currency(Currency.UZS)
                .dueDate(LocalDate.of(2026, 4, 1))
                .description("Short-term loan")
                .build();

        mockMvc.perform(post("/api/debts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.personName").value("Ali"))
                .andExpect(jsonPath("$.data.type").value("RECEIVABLE"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /debts → 400 when personName is missing")
    void createDebt_missingPersonName_returns400() throws Exception {
        mockMvc.perform(post("/api/debts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RECEIVABLE\",\"amount\":500,\"dueDate\":\"2026-04-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /debts → 400 when amount is missing")
    void createDebt_missingAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/debts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personName\":\"Ali\",\"type\":\"RECEIVABLE\",\"dueDate\":\"2026-04-01\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/debts ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /debts → 200 returns list")
    void getDebts_authenticated_returns200() throws Exception {
        when(debtService.getDebts(any(), any(), any())).thenReturn(List.of(sampleDebt()));

        mockMvc.perform(get("/api/debts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].personName").value("Ali"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /debts?type=DEBT&status=OPEN → 200 with filters")
    void getDebts_withTypeAndStatusFilter_returns200() throws Exception {
        when(debtService.getDebts(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/debts")
                        .param("type", "DEBT")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /debts → 401 when not authenticated")
    void getDebts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/debts"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/debts/{id} ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /debts/{id} → 200 for existing debt")
    void getDebtById_exists_returns200() throws Exception {
        when(debtService.getDebtById(eq(DEBT_ID), any())).thenReturn(sampleDebt());

        mockMvc.perform(get("/api/debts/{id}", DEBT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(DEBT_ID.toString()));
    }

    // ── POST /api/debts/{id}/repay ──────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /debts/{id}/repay → 200 with valid repayment")
    void repayDebt_valid_returns200() throws Exception {
        DebtResponse repaidDebt = sampleDebt().toBuilder()
                .remainingAmount(BigDecimal.valueOf(400))
                .build();
        when(debtService.repayDebt(eq(DEBT_ID), any(), any())).thenReturn(repaidDebt);

        RepaymentRequest body = RepaymentRequest.builder()
                .paymentAmount(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/api/debts/{id}/repay", DEBT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.remainingAmount").value(400));
    }

    // ── DELETE /api/debts/{id} ──────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("DELETE /debts/{id} → 200 on success")
    void deleteDebt_exists_returns200() throws Exception {
        mockMvc.perform(delete("/api/debts/{id}", DEBT_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
