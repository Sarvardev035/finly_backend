package com.finly.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finly.backend.dto.request.LoginRequest;
import com.finly.backend.dto.request.RegisterRequest;
import com.finly.backend.dto.request.TokenRefreshRequest;
import com.finly.backend.dto.response.AuthResponse;
import com.finly.backend.exception.InvalidCredentialsException;
import com.finly.backend.security.JwtAuthenticationEntryPoint;
import com.finly.backend.security.JwtAuthenticationFilter;
import com.finly.backend.security.UserDetailsServiceImpl;
import com.finly.backend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for POST /api/auth/register, /api/auth/login, /api/auth/token/refresh.
 * Services are mocked; Spring Security is loaded but auth endpoints are permitAll().
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController - /api/auth")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // ── POST /api/auth/register ─────────────────────────────────────────────

    @Test
    @DisplayName("POST /register → 201 with valid body")
    void register_validRequest_returns201() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        when(authService.register(any())).thenReturn(authResponse);

        RegisterRequest body = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("secret123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /register → 400 when email is missing")
    void register_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"John Doe\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register → 400 when password is blank")
    void register_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"John\",\"email\":\"john@example.com\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register → 400 when email format is invalid")
    void register_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"John\",\"email\":\"not-an-email\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/login ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login → 200 with valid credentials")
    void login_validCredentials_returns200() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        when(authService.login(any())).thenReturn(authResponse);

        LoginRequest body = LoginRequest.builder()
                .email("admin1@finly.com")
                .password("admin1")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /login → 400 when password is missing")
    void login_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin1@finly.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /login → 400 when email is missing")
    void login_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"admin1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /login → 401 when credentials are wrong")
    void login_wrongCredentials_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password."));

        LoginRequest body = LoginRequest.builder()
                .email("wrong@example.com")
                .password("wrong")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    // ── POST /api/auth/token/refresh ────────────────────────────────────────

    @Test
    @DisplayName("POST /token/refresh → 200 with valid refresh token")
    void refreshToken_validToken_returns200() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("same-refresh-token")
                .build();
        when(authService.refreshToken(any())).thenReturn(authResponse);

        TokenRefreshRequest body = TokenRefreshRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("POST /token/refresh → 400 when refresh token is blank")
    void refreshToken_blankToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
