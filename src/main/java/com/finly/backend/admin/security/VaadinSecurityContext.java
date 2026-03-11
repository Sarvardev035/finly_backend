package com.finly.backend.admin.security;

import com.finly.backend.security.JwtService;
import com.finly.backend.security.UserDetailsImpl;
import com.vaadin.flow.server.VaadinSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
@RequiredArgsConstructor
public class VaadinSecurityContext {

    private static final String JWT_TOKEN_KEY = "admin_jwt_token";
    private static final String ACT_AS_USER_ID_KEY = "admin_act_as_user_id";
    private final JwtService jwtService;

    public String getOrCreateToken() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }

        String cached = (String) session.getAttribute(JWT_TOKEN_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return null;
        }

        String token = jwtService.generateToken(userDetails);
        session.setAttribute(JWT_TOKEN_KEY, token);
        return token;
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    public boolean isAdminAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public void clearToken() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(JWT_TOKEN_KEY, null);
            session.setAttribute(ACT_AS_USER_ID_KEY, null);
        }
    }

    public void setActAsUserId(String userId) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(ACT_AS_USER_ID_KEY, userId);
        }
    }

    public String getActAsUserId() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(ACT_AS_USER_ID_KEY);
    }
}
