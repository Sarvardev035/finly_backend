package com.finly.backend.admin.config;

import com.finly.backend.domain.model.Role;
import com.finly.backend.domain.model.User;
import com.finly.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminBootstrapConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner ensureDefaultAdmin() {
        return args -> userRepository.findByEmail("admin1@finly.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Finly Admin")
                        .email("admin1@finly.com")
                        .password(passwordEncoder.encode("admin1"))
                        .role(Role.ADMIN)
                        .build()));
    }
}
