package com.finly.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinlyApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinlyApplication.class, args);
    }
}
