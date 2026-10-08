package com.financeflow.dto;

import com.financeflow.entity.Transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull TransactionType type,
        @NotBlank @Pattern(regexp = "Food|Transportation|Education|Shopping|Entertainment|Bills|Health|Salary|Other") String category,
        @Size(max = 255) String description,
        @NotNull LocalDate date,
        @NotNull Long accountId) {
}
