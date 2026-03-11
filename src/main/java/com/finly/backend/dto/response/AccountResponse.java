package com.finly.backend.dto.response;

import com.finly.backend.domain.model.AccountType;
import com.finly.backend.domain.model.CardType;
import com.finly.backend.domain.model.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {
    private UUID id;
    private String name;
    private AccountType type;
    private Currency currency;
    private BigDecimal balance;
    private String cardNumber;
    private CardType cardType;
    private String expiryDate;
    private LocalDateTime createdAt;
    private String userFullName;
}
