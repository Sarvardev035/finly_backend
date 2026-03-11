package com.finly.backend.dto.request;

import com.finly.backend.domain.model.AccountType;
import com.finly.backend.domain.model.CardType;
import com.finly.backend.domain.model.Currency;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotNull(message = "Account type is required")
    private AccountType type;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance cannot be negative")
    @Builder.Default
    private BigDecimal initialBalance = BigDecimal.ZERO;

    // Card-specific fields (required only if type == BANK_CARD)
    @Size(min = 16, max = 19, message = "Card number must be between 16 and 19 digits")
    private String cardNumber;

    private CardType cardType;

    @Pattern(regexp = "^(0[1-9]|1[0-2])/([0-9]{2})$", message = "Expiry date must be in MM/YY format")
    private String expiryDate;
}
