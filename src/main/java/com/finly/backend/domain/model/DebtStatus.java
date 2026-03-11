package com.finly.backend.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DebtStatus {
    OPEN,
    CLOSED;

    @JsonCreator
    public static DebtStatus from(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "OPEN", "ACTIVE" -> OPEN;
            case "CLOSED", "DONE", "PAID" -> CLOSED;
            default -> throw new IllegalArgumentException("Invalid debt status: " + raw);
        };
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
