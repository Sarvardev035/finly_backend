package com.finly.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashTest {
    @Test
    public void generateHash() {
        System.out.println("HASH_OUTPUT=" + new BCryptPasswordEncoder().encode("admin123"));
    }
}
