package com.finly.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.finly.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Exclude static resources and Admin UI paths
        if (request.getServletPath().contains("/css") ||
                request.getServletPath().contains("/js") ||
                request.getServletPath().contains("/images") ||
                request.getServletPath().startsWith("/admin") ||
                request.getServletPath().startsWith("/VAADIN")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    Collection<? extends GrantedAuthority> authoritiesToApply = userDetails.getAuthorities();
                    String actAsUserHeader = request.getHeader("X-User-Id");
                    if (actAsUserHeader != null
                            && !actAsUserHeader.isBlank()
                            && userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                        try {
                            UUID actAsUserId = UUID.fromString(actAsUserHeader);
                            UserDetails originalAdmin = userDetails;
                            userDetails = userRepository.findById(actAsUserId)
                                    .map(user -> (UserDetails) new UserDetailsImpl(user))
                                    .orElse(userDetails);

                            // Keep admin authority while acting as another user so admin-only rules still apply.
                            LinkedHashSet<GrantedAuthority> merged = new LinkedHashSet<>();
                            merged.addAll(originalAdmin.getAuthorities());
                            merged.addAll(userDetails.getAuthorities());
                            authoritiesToApply = merged;
                        } catch (IllegalArgumentException ignored) {
                            // Ignore invalid UUID and keep authenticated admin principal
                        }
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authoritiesToApply);
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Can be caught by ExceptionHandler Filter or just ignored to return 401
        }

        filterChain.doFilter(request, response);
    }
}
