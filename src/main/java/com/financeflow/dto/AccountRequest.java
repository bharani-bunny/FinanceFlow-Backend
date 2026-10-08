package com.financeflow.dto;

import com.financeflow.entity.Account.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull AccountType type,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal balance) {
}
