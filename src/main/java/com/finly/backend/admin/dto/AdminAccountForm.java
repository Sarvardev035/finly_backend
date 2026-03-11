package com.finly.backend.admin.dto;

import com.finly.backend.domain.model.AccountType;
import com.finly.backend.domain.model.CardType;
import com.finly.backend.domain.model.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminAccountForm {

    @NotBlank
    private String name;

    @NotNull
    private AccountType type;

    @NotNull
    private Currency currency;

    private BigDecimal initialBalance = BigDecimal.ZERO;

    private String cardNumber;

    private CardType cardType;

    private String expiryDate;
}
