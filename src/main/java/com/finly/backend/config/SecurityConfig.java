package com.finly.backend.config;

import com.finly.backend.security.JwtAuthenticationFilter;
import com.finly.backend.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.core.annotation.Order;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final UserDetailsServiceImpl userDetailsService;
        private final com.finly.backend.security.JwtAuthenticationEntryPoint jwtEntryPoint;

        @Bean
        @Order(1)
        public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/admin", "/admin/**")
                                .authorizeHttpRequests(req -> req
                                                .requestMatchers(
                                                                "/admin/login",
                                                                "/admin/login/**",
                                                                "/favicon.ico",
                                                                "/images/**",
                                                                "/icons/**",
                                                                "/line-awesome/**",
                                                                "/webjars/**")
                                                .permitAll()
                                                .anyRequest().hasRole("ADMIN"))
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/VAADIN/**", "/admin/login"))
                                .formLogin(form -> form
                                                .loginPage("/admin/login")
                                                .loginProcessingUrl("/admin/login")
                                                .defaultSuccessUrl("/admin/dashboard", true)
                                                .failureUrl("/admin/login?error"))
                                .logout(logout -> logout
                                                .logoutRequestMatcher(
                                                                new AntPathRequestMatcher("/admin/logout", "GET"))
                                                .logoutSuccessUrl("/admin/login?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID"))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authenticationProvider(authenticationProvider());
                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/auth/**", "/api/users/**", "/api/accounts/**",
                                                "/api/expenses/**",
                                                "/api/incomes/**", "/api/budgets/**", "/api/categories/**",
                                                "/api/debts/**",
                                                "/api/transfers/**", "/api/analytics/**", "/api/notifications/**",
                                                "/api/exchange-rates/**",
                                                "/api/connect/**", "/error",
                                                "/swagger-ui/**", "/v3/api-docs/**")
                                .authorizeHttpRequests(req -> req.requestMatchers(
                                                "/api/auth/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/v3/api-docs/**",
                                                "/v3/api-docs.yaml",
                                                "/api/exchange-rates/**",
                                                "/error").permitAll()
                                                .anyRequest().authenticated())
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/auth/**", "/api/accounts/**",
                                                                "/api/expenses/**",
                                                                "/api/incomes/**",
                                                                "/api/transfers/**", "/api/budgets/**",
                                                                "/api/categories/**",
                                                                "/api/debts/**", "/api/analytics/**",
                                                                "/api/notifications/**", "/api/users/**",
                                                                "/api/exchange-rates/**",
                                                                "/api/connect/**"))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtEntryPoint))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        @Order(3)
        public SecurityFilterChain fallbackSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(req -> req.anyRequest().permitAll())
                                .csrf(csrf -> csrf.disable());
                return http.build();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                // Support both local dev and production via env variable
                String frontendUrl = System.getenv("FRONTEND_URL");
                List<String> origins = new java.util.ArrayList<>(List.of(
                                "http://localhost:5173",
                                "http://localhost:4200"
                ));
                if (frontendUrl != null && !frontendUrl.isBlank()) {
                        origins.add(frontendUrl);
                }
                config.setAllowedOrigins(origins);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                config.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "X-XSRF-TOKEN"
                ));
                config.setExposedHeaders(List.of("Authorization"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", config);
                return source;
        }
}
