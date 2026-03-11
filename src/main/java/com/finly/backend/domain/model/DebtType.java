package com.finly.backend.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DebtType {
    DEBT,
    RECEIVABLE;

    @JsonCreator
    public static DebtType from(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "DEBT", "I_OWE" -> DEBT;
            case "RECEIVABLE", "OWED_TO_ME" -> RECEIVABLE;
            default -> throw new IllegalArgumentException("Invalid debt type: " + raw);
        };
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
