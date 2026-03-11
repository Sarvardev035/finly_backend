package com.finly.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finly.backend.domain.model.AccountType;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.dto.request.CreateAccountRequest;
import com.finly.backend.dto.response.AccountResponse;
import com.finly.backend.exception.AccountNotFoundException;
import com.finly.backend.security.JwtAuthenticationEntryPoint;
import com.finly.backend.security.JwtAuthenticationFilter;
import com.finly.backend.security.UserDetailsImpl;
import com.finly.backend.security.UserDetailsServiceImpl;
import com.finly.backend.service.AccountService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for /api/accounts CRUD endpoints.
 */
@WebMvcTest(AccountController.class)
@DisplayName("AccountController - /api/accounts")
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AccountService accountService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private AccountResponse sampleAccountResponse() {
        return AccountResponse.builder()
                .id(ACCOUNT_ID)
                .name("Main Wallet")
                .type(AccountType.CASH)
                .currency(Currency.USD)
                .balance(BigDecimal.valueOf(1200.50))
                .build();
    }

    // ── GET /api/accounts ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /accounts → 200 returns list")
    void getAllAccounts_authenticated_returns200() throws Exception {
        when(accountService.getUserAccounts(any())).thenReturn(List.of(sampleAccountResponse()));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Main Wallet"))
                .andExpect(jsonPath("$.data[0].currency").value("USD"));
    }

    @Test
    @DisplayName("GET /accounts → 401 when not authenticated")
    void getAllAccounts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/accounts/{id} ──────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /accounts/{id} → 200 for existing account")
    void getAccountById_exists_returns200() throws Exception {
        when(accountService.getAccountById(eq(ACCOUNT_ID), any()))
                .thenReturn(sampleAccountResponse());

        mockMvc.perform(get("/api/accounts/{id}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(ACCOUNT_ID.toString()));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /accounts/{id} → 404 for missing account")
    void getAccountById_notFound_returns404() throws Exception {
        when(accountService.getAccountById(eq(ACCOUNT_ID), any()))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts/{id}", ACCOUNT_ID))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/accounts ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /accounts → 201 with valid body")
    void createAccount_validRequest_returns201() throws Exception {
        when(accountService.createAccount(any(), any())).thenReturn(sampleAccountResponse());

        CreateAccountRequest body = CreateAccountRequest.builder()
                .name("Main Wallet")
                .type(AccountType.CASH)
                .currency(Currency.USD)
                .initialBalance(BigDecimal.valueOf(1200.50))
                .build();

        mockMvc.perform(post("/api/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Main Wallet"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /accounts → 400 when name is missing")
    void createAccount_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CASH\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /accounts → 400 when type is missing")
    void createAccount_missingType_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wallet\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/accounts/{id} ───────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("DELETE /accounts/{id} → 200 on success")
    void deleteAccount_exists_returns200() throws Exception {
        mockMvc.perform(delete("/api/accounts/{id}", ACCOUNT_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /accounts/{id} → 404 for missing account")
    void deleteAccount_notFound_returns404() throws Exception {
        doThrow(new AccountNotFoundException("Account not found"))
                .when(accountService).deleteAccount(eq(ACCOUNT_ID), any());

        mockMvc.perform(delete("/api/accounts/{id}", ACCOUNT_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
